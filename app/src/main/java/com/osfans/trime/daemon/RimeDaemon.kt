// SPDX-FileCopyrightText: 2015 - 2024 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.daemon

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.osfans.trime.R
import com.osfans.trime.TrimeApplication
import com.osfans.trime.core.Rime
import com.osfans.trime.core.RimeApi
import com.osfans.trime.core.RimeLifecycle
import com.osfans.trime.core.RimeMaintenanceMutex
import com.osfans.trime.core.RimeMessage
import com.osfans.trime.core.RimeUnavailableException
import com.osfans.trime.core.lifecycleScope
import com.osfans.trime.core.whenReady
import com.osfans.trime.ui.main.LogActivity
import com.osfans.trime.util.DeployNotification
import com.osfans.trime.util.appContext
import com.osfans.trime.util.subprocess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import splitties.systemservices.notificationManager
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Manage the singleton instance of [Rime]
 *
 * To use rime, client should call [createSession] to obtain a [RimeSession],
 * and call [destroySession] on client destroyed. Client should not leak the instance of [RimeApi],
 * and must use [RimeSession] to access rime functionalities.
 *
 * The instance of [Rime] always exists,but whether the dispatcher runs and callback works depend on clients, i.e.
 * if no clients are connected, the engine will be shut down on the lifecycle worker.
 *
 * Functions are thread-safe in this class.
 *
 * Adapted from [fcitx5-android/FcitxDaemon.kt](https://github.com/fcitx5-android/fcitx5-android/blob/364afb44dcf0d9e3db3d43a21a32601b2190cbdf/app/src/main/java/org/fcitx/fcitx5/android/daemon/FcitxDaemon.kt)
 */
object RimeDaemon {
    private val realRime by lazy { Rime() }

    private val rimeImpl by lazy { object : RimeApi by realRime {} }

    private val sessions = ConcurrentHashMap<String, Session>()

    private val lock = ReentrantLock()
    private val lifecycleChanges = Channel<Unit>(Channel.CONFLATED)
    private var restartRequested = false
    private var fullCheckRequested = false
    private val restartNotifications = mutableSetOf<Int>()

    internal val engineState: StateFlow<RimeLifecycle.State>
        get() = realRime.lifecycle.stateFlow

    private class Session(private val name: String) : RimeSession {
        private var clientScope: CoroutineScope? = null
        private fun isEstablished() = sessions[name] === this

        private inline fun <T> ensureEstablished(block: () -> T) = if (isEstablished()) {
            block()
        } else {
            throw IllegalStateException("Session $name is not established")
        }

        override fun <T> run(block: suspend RimeApi.() -> T): T = ensureEstablished {
            runBlocking { block(rimeImpl) }
        }

        override suspend fun <T> runOnReady(block: suspend RimeApi.() -> T): T = ensureEstablished {
            realRime.lifecycle.whenReady { ensureEstablished { block(rimeImpl) } }
        }

        override fun runIfReady(block: suspend RimeApi.() -> Unit) {
            ensureEstablished {
                if (realRime.isReady) {
                    lifecycleScope.launch {
                        if (isEstablished() && realRime.isReady) block(rimeImpl)
                    }
                }
            }
        }

        override val lifecycleScope: CoroutineScope
            get() = lock.withLock {
                ensureEstablished {
                    clientScope?.takeIf { it.isActive } ?: CoroutineScope(
                        realRime.lifecycleScope.coroutineContext + SupervisorJob(realRime.lifecycleScope.coroutineContext[Job]),
                    ).also { clientScope = it }
                }
            }

        fun close() {
            clientScope?.cancel()
        }
    }

    fun createSession(name: String): RimeSession = lock.withLock {
        if (sessions.containsKey(name)) {
            return@withLock sessions.getValue(name)
        }
        val session = Session(name)
        sessions[name] = session
        lifecycleChanges.trySend(Unit)
        return@withLock session
    }

    fun destroySession(name: String): Unit = lock.withLock {
        val session = sessions.remove(name) ?: return
        session.close()
        lifecycleChanges.trySend(Unit)
    }

    /**
     * Reuse a session for remote service
     */
    fun getFirstSessionOrNull(): RimeSession? = sessions.firstNotNullOfOrNull { it.value }

    private var restartId = 0

    init {
        DeployNotification.ensureChannel()
        // Only this worker changes the native lifecycle. Never hold the session
        // lock while waiting for deployment, shutdown, or restarting the engine.
        TrimeApplication.getInstance().coroutineScope.launch(Dispatchers.IO) {
            for (change in lifecycleChanges) {
                var activeNotifications = emptyList<Int>()
                try {
                    RimeMaintenanceMutex.withLock {
                        val (restart, fullCheck, notifications) = lock.withLock {
                            Triple(restartRequested, fullCheckRequested, restartNotifications.toList()).also {
                                restartRequested = false
                                fullCheckRequested = false
                                restartNotifications.clear()
                            }
                        }
                        activeNotifications = notifications
                        if (realRime.lifecycle.currentState == RimeLifecycle.State.STARTING) {
                            realRime.lifecycle.whenReady {}
                        }
                        val stopping = lock.withLock {
                            (realRime.isReady && (sessions.isEmpty() || restart)).also {
                                if (it) realRime.beginShutdown()
                            }
                        }
                        if (stopping) realRime.finishShutdown()
                        if (lock.withLock { sessions.isNotEmpty() } &&
                            realRime.lifecycle.currentState in setOf(RimeLifecycle.State.STOPPED, RimeLifecycle.State.FAILED)
                        ) {
                            realRime.startup(fullCheck)
                            realRime.lifecycle.whenReady {}
                        }
                    }
                } catch (_: RimeUnavailableException) {
                    // The engine published the cause and failure notification. Keep
                    // this worker alive for an explicit retry or a new session.
                } finally {
                    activeNotifications.forEach(notificationManager::cancel)
                }
            }
        }
        TrimeApplication.getInstance().coroutineScope.launch {
            realRime.messageFlow.collect {
                handleRimeMessage(it)
            }
        }
    }

    suspend fun retryFailedStartup() = withContext(Dispatchers.IO) {
        RimeMaintenanceMutex.withLock {
            if (realRime.lifecycle.currentState == RimeLifecycle.State.FAILED && sessions.isNotEmpty()) {
                realRime.startup()
            }
            if (realRime.lifecycle.currentState == RimeLifecycle.State.STARTING) realRime.lifecycle.whenReady {}
        }
    }

    private inline fun sendNotification(
        id: Int,
        buildAction: NotificationCompat.Builder.() -> Unit,
    ) {
        val builder =
            NotificationCompat
                .Builder(appContext, DeployNotification.CHANNEL_ID)
                .setContentTitle(appContext.getString(R.string.rime_daemon))
        builder.buildAction()
        builder.build().let { notificationManager.notify(id, it) }
    }

    /**
     * Restart Rime instance to deploy while keep the session
     */
    fun restartRime(fullCheck: Boolean = false) = lock.withLock {
        val id = restartId++
        if (!fullCheck) {
            sendNotification(id) {
                setSmallIcon(R.drawable.ic_baseline_sync_24)
                setContentTitle(appContext.getString(R.string.rime_daemon))
                setContentText(appContext.getString(R.string.restarting_rime))
                setOngoing(true)
                setProgress(100, 0, true)
                setPriority(NotificationCompat.PRIORITY_HIGH)
            }
        }
        restartRequested = true
        fullCheckRequested = fullCheckRequested || fullCheck
        restartNotifications.add(id)
        lifecycleChanges.trySend(Unit)
    }

    private suspend fun handleRimeMessage(it: RimeMessage<*>) {
        if (it is RimeMessage.DeployMessage) {
            when (it.data) {
                RimeMessage.DeployMessage.State.Start -> {
                    DeployNotification.showProgress()
                }
                RimeMessage.DeployMessage.State.Success -> {
                    DeployNotification.showSuccess()
                }
                RimeMessage.DeployMessage.State.Failure -> {
                    val log = withContext(Dispatchers.IO) {
                        try {
                            val process = subprocess("logcat", "-v", "brief", "-d", "-t", "2000", "*:W")
                            try {
                                process.inputStream.bufferedReader().use { it.readText() }
                            } finally {
                                process.errorStream.close()
                                process.outputStream.close()
                                process.destroy()
                            }
                        } catch (e: IOException) {
                            "Unable to collect deployment log: ${e.stackTraceToString()}"
                        }
                    }.takeLast(128_000)
                    val intent =
                        Intent(appContext, LogActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            putExtra(LogActivity.FROM_DEPLOY, true)
                            putExtra(LogActivity.DEPLOY_FAILURE_TRACE, log)
                        }
                    val pendingIntent =
                        PendingIntent.getActivity(
                            appContext,
                            0,
                            intent,
                            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
                        )
                    DeployNotification.showFailure(pendingIntent)
                }
            }
        }
    }
}
