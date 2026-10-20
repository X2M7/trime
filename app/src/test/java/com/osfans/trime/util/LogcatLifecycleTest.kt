/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.osfans.trime.util

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class LogcatLifecycleTest :
    StringSpec({
        "snapshot retains its command arguments and releases its child" {
            runBlocking {
                val process = PipeProcess()
                var command = emptyList<String>()
                val logcat = Logcat(null) {
                    command = it
                    process
                }
                logcat.getLogAsync().await().getOrThrow() shouldBe listOf("one", "two")
                command shouldBe listOf("logcat", "", "-d")
                process.assertReleased()
            }
        }

        "a failed snapshot command returns failure instead of empty success" {
            runBlocking {
                val process = PipeProcess(java.io.ByteArrayInputStream(ByteArray(0)), exitCode = 1)
                val logcat = Logcat(null) { process }
                val result = logcat.getLogAsync().await()
                result.isFailure shouldBe true
                result.exceptionOrNull()?.message shouldBe "Unable to read logcat snapshot"
                process.assertReleased()
            }
        }

        "clear command waits for completion and closes all streams" {
            runBlocking {
                val process = PipeProcess()
                val logcat = Logcat(null) { process }
                withTimeout(5_000L) { logcat.clearLog().join() }
                process.assertReleased()
            }
        }

        "a live read failure stays primary when closing stdout also fails" {
            runBlocking {
                val readFailure = java.io.IOException("Synthetic live read failure")
                val closeFailure = java.io.IOException("Synthetic stdout close failure")
                val process = PipeProcess(object : java.io.InputStream() {
                    override fun read(): Int = throw readFailure
                    override fun close() = throw closeFailure
                })
                val logged = AtomicReference<Throwable?>()
                val tree = object : Timber.Tree() {
                    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                        if (message.startsWith("Unable to read logcat")) logged.set(t)
                    }
                }
                val logcat = Logcat(null) { process }
                Timber.plant(tree)
                try {
                    val stream = logcat.initLogFlow()
                    try {
                        withTimeout(5_000L) { stream.join() }
                        // Coroutine stack recovery can copy an exception across this boundary.
                        // Its original cause must still be the read failure, never cleanup.
                        generateSequence(checkNotNull(logged.get())) { it.cause }
                            .firstOrNull { it === readFailure || it === closeFailure } shouldBe readFailure
                        readFailure.suppressed.any { it === closeFailure } shouldBe true
                        process.assertReleased()
                    } finally {
                        withContext(NonCancellable) {
                            logcat.shutdownLogFlow()
                            stream.cancelAndJoin()
                        }
                    }
                } finally {
                    Timber.uproot(tree)
                }
            }
        }

        "stop during process creation releases that late child and allows another stream" {
            runBlocking {
                val entered = CountDownLatch(1)
                val finishCreation = CountDownLatch(1)
                val first = PipeProcess()
                val secondPipe = BlockingPipe()
                val second = PipeProcess(secondPipe, secondPipe::release, naturalExit = false)
                val starts = AtomicInteger()
                val logcat = Logcat(null) {
                    if (starts.incrementAndGet() == 1) {
                        entered.countDown()
                        check(finishCreation.await(5, TimeUnit.SECONDS))
                        first
                    } else {
                        second
                    }
                }
                val oldJob = logcat.initLogFlow()
                check(entered.await(5, TimeUnit.SECONDS))
                logcat.shutdownLogFlow()
                val newJob = logcat.initLogFlow()
                check(secondPipe.entered.await(5, TimeUnit.SECONDS))
                finishCreation.countDown()
                withTimeout(5_000L) { oldJob.join() }
                first.assertReleased()
                // The old stream's completion must not erase the newly running stream's slot.
                newJob.isActive shouldBe true
                logcat.shutdownLogFlow()
                withTimeout(5_000L) { newJob.join() }
                second.assertReleased()
                starts.get() shouldBe 2
            }
        }

        "a completed stream can start again without keeping a stale process" {
            runBlocking {
                val children = mutableListOf<PipeProcess>()
                val logcat = Logcat(null) { PipeProcess().also { children.add(it) } }
                withTimeout(5_000L) { logcat.initLogFlow().join() }
                withTimeout(5_000L) { logcat.initLogFlow().join() }
                children.size shouldBe 2
                children.forEach { it.assertReleased() }
            }
        }
    })
