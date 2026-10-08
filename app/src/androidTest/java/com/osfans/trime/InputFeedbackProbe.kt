// SPDX-License-Identifier: GPL-3.0-or-later
package com.osfans.trime

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
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
import java.io.IOException
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
        val identity: SoundDescriptorIdentity?,
        val target: String,
        val provenNonFixture: DeniedDescriptorProof? = null,
    ) {
        init {
            check((identity == null) == (provenNonFixture != null))
        }
    }

    private data class DeniedDescriptorProof(
        val copiedSlot: Int,
        val errno: Int,
        val rawFailure: String,
        val inode: Long,
        val rawFdInfo: String,
    )

    private data class DescriptorFallbackCalibration(
        val fixtureInodes: Set<Long>,
        val available: Boolean,
    )

    private data class DescriptorAuditContext(
        val soundIdentities: Set<SoundDescriptorIdentity>,
        val fallbackCalibration: DescriptorFallbackCalibration,
        val audit: MutableList<String>,
        val result: Bundle,
    )

    private data class DescriptorSnapshot(
        val listedCount: Int,
        val handles: Map<String, DescriptorHandle>,
        val disappearedSlots: List<String>,
    ) {
        fun ownedBy(identities: Set<SoundDescriptorIdentity>): Map<String, DescriptorHandle> = handles.filterValues { it.identity?.let(identities::contains) == true }
    }

    private fun descriptorIdentity(file: File): SoundDescriptorIdentity = Os.stat(file.path).let { SoundDescriptorIdentity(it.st_dev, it.st_ino) }

    private fun copiedFdInfo(copy: ParcelFileDescriptor): String = File("/proc/self/fdinfo/${copy.fd}").bufferedReader().use { reader ->
        val raw = StringBuilder()
        val buffer = CharArray(4096)
        while (true) {
            val count = reader.read(buffer)
            if (count < 0) break
            check(raw.length + count <= 65_536) { "Owned-copy fdinfo exceeded 64 Ki characters: copied_slot=${copy.fd}" }
            raw.append(buffer, 0, count)
        }
        raw.toString()
    }

    private fun topLevelFdInfoInode(raw: String): Long? {
        // Embedded inotify/epoll ino labels describe other objects. Only the
        // exact top-level common field can identify this opened descriptor.
        val fields = raw.lineSequence().filter { Regex("^ino\\b").containsMatchIn(it) }.toList()
        if (fields.isEmpty()) return null
        check(fields.size == 1) { "Expected one top-level fdinfo ino field: $raw" }
        val value = checkNotNull(Regex("ino:[ \\t]*([0-9]+)[ \\t]*").matchEntire(fields.single())) {
            "Malformed top-level fdinfo ino field: $raw"
        }.groupValues[1]
        return checkNotNull(value.toLongOrNull()) { "Unrepresentable top-level fdinfo ino field: $raw" }
    }

    private fun calibrateDescriptorFallback(
        fixtures: Map<File, SoundDescriptorIdentity>,
        audit: MutableList<String>,
    ): DescriptorFallbackCalibration {
        var available = true
        for ((file, expected) in fixtures) {
            val pathIdentity = descriptorIdentity(file)
            check(pathIdentity == expected) { "Sound fixture path identity changed: $file" }
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { held ->
                val direct = Os.fstat(held.fileDescriptor).let { SoundDescriptorIdentity(it.st_dev, it.st_ino) }
                ParcelFileDescriptor.fromFd(held.fd).use { copy ->
                    val pinned = Os.fstat(copy.fileDescriptor).let { SoundDescriptorIdentity(it.st_dev, it.st_ino) }
                    check(pathIdentity == direct && direct == pinned) {
                        "Sound fixture identity calibration disagreed: $file path=$pathIdentity direct=$direct pinned=$pinned"
                    }
                    val raw = copiedFdInfo(copy)
                    val inode = topLevelFdInfoInode(raw)
                    if (inode == null) {
                        // Older kernels have no common ino field. Full fstat
                        // identity remains usable; EACCES fallback does not.
                        available = false
                    } else {
                        check(inode == pathIdentity.inode) { "Sound fixture fdinfo calibration disagreed: $file raw=$raw" }
                    }
                    audit += "fdinfo-calibration fixture=$file path=$pathIdentity direct=$direct pinned=$pinned " +
                        "original_slot=${held.fd} copied_slot=${copy.fd} ino=$inode raw_fdinfo=$raw"
                }
            }
        }
        return DescriptorFallbackCalibration(fixtures.values.map { it.inode }.toSet(), available)
    }

    private fun recordDescriptorFailure(context: DescriptorAuditContext, detail: String) {
        context.audit += detail
        if (!context.result.containsKey("sound_descriptor_first_failed_slot")) {
            context.result.putString("sound_descriptor_first_failed_slot", detail)
        }
    }

    private fun descriptorSnapshot(context: DescriptorAuditContext, stage: String): DescriptorSnapshot {
        val slots = checkNotNull(File("/proc/self/fd").listFiles()) {
            "Cannot list the current process descriptor slots"
        }
        val handles = linkedMapOf<String, DescriptorHandle>()
        val disappeared = mutableListOf<String>()
        for (slot in slots) {
            val originalFd = checkNotNull(slot.name.toIntOrNull()) { "Invalid descriptor slot: ${slot.name}" }
            check(originalFd >= 0)
            val copy = try {
                // An owned duplicate pins the selected object without adopting,
                // reading, seeking or otherwise mutating the original handle.
                ParcelFileDescriptor.fromFd(originalFd)
            } catch (failure: IOException) {
                val errnoFailure = failure.cause as? ErrnoException
                val detail = "$stage original_slot=${slot.name} phase=pre-pin-dup " +
                    "errno=${errnoFailure?.errno} raw_failure=$failure raw_cause=${failure.cause}"
                if (errnoFailure?.errno != OsConstants.EBADF) {
                    recordDescriptorFailure(context, detail)
                    throw failure
                }
                // Only acquisition EBADF proves a listed slot vanished before
                // pinning, including the already-closed listing directory FD.
                disappeared += detail
                context.audit += detail
                continue
            }
            var copiedSlot: Int? = null
            try {
                copy.use {
                    copiedSlot = copy.fd
                    var target: String? = null
                    var proof: DeniedDescriptorProof? = null
                    val identity = try {
                        Os.fstat(copy.fileDescriptor).let { SoundDescriptorIdentity(it.st_dev, it.st_ino) }
                    } catch (failure: ErrnoException) {
                        val detail = "$stage original_slot=${slot.name} copied_slot=${copy.fd} " +
                            "phase=pinned-fstat errno=${failure.errno} raw_failure=$failure"
                        // Save the first denied raw slot before any further
                        // metadata operation can fail and replace its exception.
                        if (!context.result.containsKey("sound_descriptor_first_fstat_denial")) {
                            context.result.putString("sound_descriptor_first_fstat_denial", detail)
                        }
                        context.audit += detail
                        if (failure.errno != OsConstants.EACCES) throw failure
                        target = Os.readlink("/proc/self/fd/${copy.fd}")
                        val raw = copiedFdInfo(copy)
                        context.audit += "$detail target=$target raw_fdinfo=$raw"
                        if (!context.result.containsKey("sound_descriptor_first_eacces_metadata")) {
                            context.result.putString("sound_descriptor_first_eacces_metadata", "$detail target=$target raw_fdinfo=$raw")
                        }
                        check(context.fallbackCalibration.available) { "EACCES fallback unavailable after all-fixture calibration: $detail" }
                        val inode = checkNotNull(topLevelFdInfoInode(raw)) { "Missing top-level fdinfo ino after EACCES: $detail raw=$raw" }
                        check(inode !in context.fallbackCalibration.fixtureInodes) {
                            "EACCES descriptor inode is ambiguous with a fixture: $detail ino=$inode raw=$raw"
                        }
                        proof = DeniedDescriptorProof(copy.fd, failure.errno, failure.toString(), inode, raw)
                        null
                    }
                    val observedTarget = target ?: Os.readlink("/proc/self/fd/${copy.fd}")
                    check(handles.put(slot.name, DescriptorHandle(identity, observedTarget, proof)) == null) {
                        "Duplicate current-process descriptor slot: ${slot.name}"
                    }
                }
            } catch (failure: Throwable) {
                recordDescriptorFailure(context, "$stage original_slot=${slot.name} copied_slot=$copiedSlot phase=pinned-audit raw_failure=$failure")
                throw failure
            }
        }
        return DescriptorSnapshot(slots.size, handles, disappeared)
    }

    private fun verifySoundDescriptorHandles(
        context: DescriptorAuditContext,
        stage: String,
        expected: Int,
        includeAllTargets: Boolean = false,
    ): DescriptorSnapshot {
        val snapshot = descriptorSnapshot(context, stage)
        val owned = snapshot.ownedBy(context.soundIdentities)
        context.audit += "$stage listed_total=${snapshot.listedCount} observed_total=${snapshot.handles.size} " +
            "fixture_slots=$owned disappeared=${snapshot.disappearedSlots}"
        if (includeAllTargets) context.audit += "$stage all_observed_slots=${snapshot.handles}"
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
                        val soundFixtures = descriptors.associateWith(::descriptorIdentity)
                        val soundIdentities = soundFixtures.values.toSet()
                        check(soundIdentities.size == 2) { "Expected two distinct sound descriptor fixture identities" }
                        var descriptorContext = DescriptorAuditContext(
                            soundIdentities,
                            calibrateDescriptorFallback(soundFixtures, descriptorAudit),
                            descriptorAudit,
                            result,
                        )
                        verifySoundDescriptorHandles(descriptorContext, "before-controls", 0)
                        // Open the SAME fixture twice. Counting only distinct inodes
                        // would incorrectly report one handle in the two-slot control.
                        descriptors.first().inputStream().use {
                            verifySoundDescriptorHandles(descriptorContext, "held-one", 1)
                            descriptors.first().inputStream().use {
                                verifySoundDescriptorHandles(descriptorContext, "held-two-same-file", 2)
                            }
                            verifySoundDescriptorHandles(descriptorContext, "held-one-after-inner-close", 1)
                        }
                        verifySoundDescriptorHandles(descriptorContext, "after-target-controls", 0)
                        val unrelated = File(folder, "unrelated-fd-control.bin")
                        unrelated.writeBytes(byteArrayOf(1))
                        val unrelatedIdentity = descriptorIdentity(unrelated)
                        check(unrelatedIdentity !in soundIdentities)
                        descriptorContext = descriptorContext.copy(
                            fallbackCalibration = calibrateDescriptorFallback(soundFixtures + (unrelated to unrelatedIdentity), descriptorAudit),
                        )
                        val unrelatedBefore = descriptorSnapshot(descriptorContext, "before-unrelated-control")
                        withOpenReaders(unrelated, 9) {
                            val held = verifySoundDescriptorHandles(descriptorContext, "held-nine-unrelated", 0)
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
                        val afterUnrelated = verifySoundDescriptorHandles(descriptorContext, "after-unrelated-close", 0)
                        check(afterUnrelated.ownedBy(setOf(unrelatedIdentity)).isEmpty())
                        check(unrelated.delete())
                        val beforeScans = verifySoundDescriptorHandles(descriptorContext, "before-scans", 0, true)
                        result.putInt("sound_descriptor_process_fds_before", beforeScans.listedCount)
                        repeat(10) { batch ->
                            repeat(20) { SoundEffectManager.getAllSoundEffects() }
                            verifySoundDescriptorHandles(descriptorContext, "after-scan-batch-${batch + 1}", 0)
                        }
                        val afterScans = verifySoundDescriptorHandles(descriptorContext, "after-200-scans", 0, true)
                        result.putInt("sound_descriptor_process_fds_after", afterScans.listedCount)
                        descriptorAudit += "scan_process_fd_delta=${afterScans.listedCount - beforeScans.listedCount} diagnostic_only=true"
                        val effects = SoundEffectManager.getAllSoundEffects().filter { it.name.startsWith(id) }.sortedBy { it.melody.size }
                        check(effects.size == 2)
                        verifySoundDescriptorHandles(descriptorContext, "after-effects-read", 0)
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
