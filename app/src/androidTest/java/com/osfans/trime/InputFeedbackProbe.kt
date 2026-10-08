// SPDX-License-Identifier: GPL-3.0-or-later
package com.osfans.trime

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.view.KeyEvent
import com.osfans.trime.daemon.RimeDaemon
import com.osfans.trime.data.base.DataManager
import com.osfans.trime.data.prefs.AppPrefs
import com.osfans.trime.data.soundeffect.SoundEffectManager
import com.osfans.trime.ime.keyboard.InputFeedbackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlin.math.sin

internal object InputFeedbackProbe {
    private fun field(name: String): Any? = InputFeedbackManager::class.java.getDeclaredField(name).let {
        it.isAccessible = true
        it.get(InputFeedbackManager)
    }

    private data class SoundDescriptorIdentity(val device: Long, val inode: Long)

    private data class DescriptorHandle(
        val identity: SoundDescriptorIdentity,
        val target: String,
    )

    private data class DescriptorSnapshot(
        val listedCount: Int,
        val handles: Map<String, DescriptorHandle>,
        val disappearedSlots: List<String>,
    ) {
        fun ownedBy(identities: Set<SoundDescriptorIdentity>): Map<String, DescriptorHandle> = handles.filterValues { it.identity in identities }
    }

    private fun descriptorIdentity(file: File): SoundDescriptorIdentity = Os.stat(file.path).let { SoundDescriptorIdentity(it.st_dev, it.st_ino) }

    private fun descriptorSnapshot(): DescriptorSnapshot {
        val slots = checkNotNull(File("/proc/self/fd").listFiles()) {
            "Cannot list the current process descriptor slots"
        }
        val handles = linkedMapOf<String, DescriptorHandle>()
        val disappeared = mutableListOf<String>()
        for (slot in slots) {
            val stat = try {
                // stat follows the procfs magic link to the actual open file,
                // including aliased paths and files unlinked after opening.
                Os.stat(slot.path)
            } catch (failure: ErrnoException) {
                // An unrelated slot may close between directory listing and stat.
                // Other errors (including EBADF for this pathname call) must fail.
                if (failure.errno != OsConstants.ENOENT) throw failure
                disappeared += "${slot.name}:stat:ENOENT"
                continue
            }
            val target = try {
                Os.readlink(slot.path)
            } catch (failure: ErrnoException) {
                if (failure.errno != OsConstants.ENOENT) throw failure
                disappeared += "${slot.name}:readlink:ENOENT"
                // Do not discard a stat-confirmed fixture handle just because
                // its diagnostic pathname disappears after the ownership check.
                "<readlink disappeared after stat>"
            }
            check(
                handles.put(
                    slot.name,
                    DescriptorHandle(
                        SoundDescriptorIdentity(stat.st_dev, stat.st_ino),
                        target,
                    ),
                ) == null,
            ) { "Duplicate current-process descriptor slot: ${slot.name}" }
        }
        return DescriptorSnapshot(slots.size, handles, disappeared)
    }

    private fun verifySoundDescriptorHandles(
        identities: Set<SoundDescriptorIdentity>,
        stage: String,
        expected: Int,
        audit: MutableList<String>,
        includeAllTargets: Boolean = false,
    ): DescriptorSnapshot {
        val snapshot = descriptorSnapshot()
        val owned = snapshot.ownedBy(identities)
        audit += "$stage listed_total=${snapshot.listedCount} observed_total=${snapshot.handles.size} " +
            "fixture_slots=$owned disappeared=${snapshot.disappearedSlots}"
        if (includeAllTargets) audit += "$stage all_observed_slots=${snapshot.handles}"
        check(owned.size == expected) {
            "Sound fixture descriptor slots at $stage: expected=$expected actual=${owned.size} " +
                "owned=$owned all_observed_slots=${snapshot.handles} disappeared=${snapshot.disappearedSlots}"
        }
        return snapshot
    }

    private fun withOpenReaders(file: File, count: Int, block: () -> Unit) {
        if (count == 0) {
            block()
        } else {
            file.inputStream().use { withOpenReaders(file, count - 1, block) }
        }
    }

    fun run(instrumentation: Instrumentation) {
        var success = false
        val result = Bundle()
        val descriptorAudit = mutableListOf<String>()
        try {
            check(Build.HARDWARE in setOf("ranchu", "goldfish"))
            check(RimeDaemon.getFirstSessionOrNull() == null) { "Feedback probe requires an idle installation" }
            val prefs = AppPrefs.defaultInstance().keyboard
            val booleans = listOf(prefs.soundOnKeyPress, prefs.useCustomSoundEffect, prefs.speakOnKeyPress, prefs.speakOnCommit)
            val previous = booleans.map { it.getValue() }
            val previousName = prefs.customSoundEffect.getValue()
            val previousVolume = prefs.soundVolume.getValue()
            val id = "runtime-feedback-${UUID.randomUUID()}"
            val root = File(DataManager.userDataDir, "soundeffect")
            val folder = File(root, id)
            check(folder.mkdirs())
            val descriptors = listOf(File(root, "$id-long.sound.yaml"), File(root, "$id-short.sound.yaml"))
            try {
                val audio = ByteBuffer.allocate(44 + 1600).order(ByteOrder.LITTLE_ENDIAN)
                audio.put("RIFF".toByteArray()).putInt(1636).put("WAVEfmt ".toByteArray())
                    .putInt(16).putShort(1).putShort(1).putInt(8000).putInt(16000)
                    .putShort(2).putShort(16).put("data".toByteArray()).putInt(1600)
                repeat(800) { audio.putShort((sin(it * 2.0 * Math.PI * 240 / 8000) * 3000).toInt().toShort()) }
                File(folder, "key.wav").writeBytes(audio.array())
                descriptors.forEachIndexed { index, file ->
                    check(!file.exists())
                    val name = "$id-${if (index == 0) "long" else "short"}"
                    val melody = List(if (index == 0) 5 else 1) { "key.wav" }.joinToString(", ")
                    file.writeText("name: $name\nfolder: $id\nsound: [key.wav]\nmelody: [$melody]\nkeyset: []\n")
                }
                runBlocking {
                    withTimeout(30_000) {
                        withContext(Dispatchers.Main) {
                            booleans.forEach { it.setValue(false) }
                            InputFeedbackManager.init()
                            repeat(20) { InputFeedbackManager.startInput() }
                            check(field("tts") == null && field("soundPool") == null)
                            prefs.speakOnKeyPress.setValue(true)
                            prefs.speakOnCommit.setValue(true)
                            InputFeedbackManager.startInput(sensitive = true)
                            InputFeedbackManager.keyPressSpeak(KeyEvent.KEYCODE_A)
                            InputFeedbackManager.textCommitSpeak("private-test-only")
                            check(field("tts") == null && field("pendingSpeech") == null)
                            prefs.speakOnKeyPress.setValue(false)
                            prefs.speakOnCommit.setValue(false)
                            @Suppress("DEPRECATION")
                            val hasSpeechEngine =
                                instrumentation.targetContext.packageManager
                                    .queryIntentServices(
                                        Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE),
                                        PackageManager.MATCH_DEFAULT_ONLY,
                                    )
                                    .orEmpty()
                                    .isNotEmpty()
                            if (!hasSpeechEngine) {
                                prefs.speakOnCommit.setValue(true)
                                InputFeedbackManager.startInput(sensitive = false)
                                InputFeedbackManager.textCommitSpeak("unavailable-engine-control")
                                check(field("tts") == null && field("pendingSpeech") == null)
                                val retryAfter = field("speechRetryAfter") as Long
                                check(retryAfter > SystemClock.uptimeMillis())
                                InputFeedbackManager.textCommitSpeak("unavailable-engine-cooldown")
                                check(field("tts") == null && field("pendingSpeech") == null)
                                check(field("speechRetryAfter") == retryAfter)
                                prefs.speakOnCommit.setValue(false)
                            }
                        }
                        val soundIdentities = descriptors.map(::descriptorIdentity).toSet()
                        check(soundIdentities.size == 2) { "Expected two distinct sound descriptor fixture identities" }
                        verifySoundDescriptorHandles(soundIdentities, "before-controls", 0, descriptorAudit)
                        // Open the SAME fixture twice. Counting only distinct inodes
                        // would incorrectly report one handle in the two-slot control.
                        descriptors.first().inputStream().use {
                            verifySoundDescriptorHandles(soundIdentities, "held-one", 1, descriptorAudit)
                            descriptors.first().inputStream().use {
                                verifySoundDescriptorHandles(soundIdentities, "held-two-same-file", 2, descriptorAudit)
                            }
                            verifySoundDescriptorHandles(soundIdentities, "held-one-after-inner-close", 1, descriptorAudit)
                        }
                        verifySoundDescriptorHandles(soundIdentities, "after-target-controls", 0, descriptorAudit)
                        val unrelated = File(folder, "unrelated-fd-control.bin")
                        unrelated.writeBytes(byteArrayOf(1))
                        val unrelatedIdentity = descriptorIdentity(unrelated)
                        check(unrelatedIdentity !in soundIdentities)
                        val unrelatedBefore = descriptorSnapshot()
                        withOpenReaders(unrelated, 9) {
                            val held = verifySoundDescriptorHandles(soundIdentities, "held-nine-unrelated", 0, descriptorAudit)
                            val unrelatedSlots = held.ownedBy(setOf(unrelatedIdentity))
                            check(unrelatedSlots.size == 9) { "Unrelated open-slot control missing handles: $unrelatedSlots" }
                            // Remove only the verified nine-slot cohort from THIS
                            // snapshot. Do not assume other process FDs stayed fixed.
                            val withoutUnrelated = held.listedCount - unrelatedSlots.size
                            check(held.listedCount > withoutUnrelated + 8)
                            descriptorAudit += "unrelated-control actual_before=${unrelatedBefore.listedCount} " +
                                "actual_held=${held.listedCount} unrelated_slots=$unrelatedSlots " +
                                "same_snapshot_old_global_before=$withoutUnrelated " +
                                "same_snapshot_old_global_after=${held.listedCount} old_plus8_rejected=true"
                        }
                        val afterUnrelated = verifySoundDescriptorHandles(soundIdentities, "after-unrelated-close", 0, descriptorAudit)
                        check(afterUnrelated.ownedBy(setOf(unrelatedIdentity)).isEmpty())
                        check(unrelated.delete())
                        val beforeScans = verifySoundDescriptorHandles(soundIdentities, "before-scans", 0, descriptorAudit, true)
                        result.putInt("sound_descriptor_process_fds_before", beforeScans.listedCount)
                        repeat(10) { batch ->
                            repeat(20) { SoundEffectManager.getAllSoundEffects() }
                            verifySoundDescriptorHandles(soundIdentities, "after-scan-batch-${batch + 1}", 0, descriptorAudit)
                        }
                        val afterScans = verifySoundDescriptorHandles(soundIdentities, "after-200-scans", 0, descriptorAudit, true)
                        result.putInt("sound_descriptor_process_fds_after", afterScans.listedCount)
                        descriptorAudit += "scan_process_fd_delta=${afterScans.listedCount - beforeScans.listedCount} diagnostic_only=true"
                        val effects = SoundEffectManager.getAllSoundEffects().filter { it.name.startsWith(id) }.sortedBy { it.melody.size }
                        check(effects.size == 2)
                        verifySoundDescriptorHandles(soundIdentities, "after-effects-read", 0, descriptorAudit)
                        withContext(Dispatchers.Main) {
                            prefs.soundOnKeyPress.setValue(true)
                            prefs.useCustomSoundEffect.setValue(true)
                            prefs.soundVolume.setValue(10)
                            SoundEffectManager.switchEffect(effects.last())
                        }
                        while (!withContext(Dispatchers.Main) { (field("readySounds") as Set<*>).isNotEmpty() }) delay(50)
                        withContext(Dispatchers.Main) {
                            repeat(4) { InputFeedbackManager.keyPressSound(KeyEvent.KEYCODE_A) }
                            SoundEffectManager.switchEffect(effects.first())
                            // Press before the asynchronous reload can reset the old melody index.
                            InputFeedbackManager.keyPressSound(KeyEvent.KEYCODE_A)
                        }
                        delay(200)
                        repeat(100) {
                            withContext(Dispatchers.Main) { InputFeedbackManager.keyPressSound(KeyEvent.KEYCODE_A) }
                            delay(30)
                        }
                        withContext(Dispatchers.Main) {
                            check(SoundEffectManager.activeSoundEffect?.name == effects.first().name)
                            check(field("tts") == null)
                            InputFeedbackManager.destroy()
                            check(field("tts") == null && field("soundPool") == null)
                            check((field("loadedSounds") as Map<*, *>).isEmpty())
                            check((field("readySounds") as Set<*>).isEmpty())
                            InputFeedbackManager.reloadSoundEffects()
                            InputFeedbackManager.startInput()
                            check(field("soundLoading") == null && field("soundPool") == null)
                        }
                    }
                }
            } finally {
                instrumentation.runOnMainSync {
                    InputFeedbackManager.destroy()
                    booleans.zip(previous).forEach { (pref, value) -> pref.setValue(value) }
                    prefs.customSoundEffect.setValue(previousName)
                    prefs.soundVolume.setValue(previousVolume)
                }
                descriptors.forEach { if (it.exists()) check(it.delete()) }
                check(folder.deleteRecursively())
                SoundEffectManager.init()
                check(prefs.customSoundEffect.sharedPreferences.edit().commit())
            }
            success = true
            result.putString("stream", "PASS: lazy feedback, 200 descriptor scans, actual SoundPool loading, shorter melody switch, playback and resource release\n")
        } catch (e: Throwable) {
            result.putString("stream", "FAIL: ${e.stackTraceToString()}\n")
        }
        result.putString("sound_descriptor_fd_audit", descriptorAudit.joinToString("\n"))
        result.putBoolean("passed", success)
        instrumentation.finish(if (success) Activity.RESULT_OK else Activity.RESULT_CANCELED, result)
    }
}
