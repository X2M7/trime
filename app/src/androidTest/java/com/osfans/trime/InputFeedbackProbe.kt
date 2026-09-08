// SPDX-License-Identifier: GPL-3.0-or-later
package com.osfans.trime

import android.app.Activity
import android.app.Instrumentation
import android.os.Build
import android.os.Bundle
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

    fun run(instrumentation: Instrumentation) {
        var success = false
        val result = Bundle()
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
                        }
                        val fdsBefore = File("/proc/self/fd").list()!!.size
                        repeat(200) { SoundEffectManager.getAllSoundEffects() }
                        check(File("/proc/self/fd").list()!!.size <= fdsBefore + 8) { "Sound descriptor handles accumulated" }
                        val effects = SoundEffectManager.getAllSoundEffects().filter { it.name.startsWith(id) }.sortedBy { it.melody.size }
                        check(effects.size == 2)
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
            }
            success = true
            result.putString("stream", "PASS: lazy feedback, 200 descriptor scans, actual SoundPool loading, shorter melody switch, playback and resource release\n")
        } catch (e: Throwable) {
            result.putString("stream", "FAIL: ${e.stackTraceToString()}\n")
        }
        result.putBoolean("passed", success)
        instrumentation.finish(if (success) Activity.RESULT_OK else Activity.RESULT_CANCELED, result)
    }
}
