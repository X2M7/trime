/* SPDX-License-Identifier: GPL-3.0-or-later */
package org.x2m7.trime.readiness.feedback;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONArray;
import org.json.JSONObject;

/** Tests the real APK's manager on Android's main Looper and real audio APIs. */
public final class FeedbackProbe extends Instrumentation {
    private final JSONObject report = new JSONObject();
    private final JSONArray checks = new JSONArray();
    private final Map<String, Object> saved = new HashMap<>();
    private SharedPreferences prefs;
    private Class<?> managerClass;
    private Object manager;
    private File output;
    private File soundDescriptor;
    private File soundFile;
    private File soundDirectory;
    private Field activeSoundEffect;
    private Object savedSoundEffect;
    private boolean savedSoundEffectCaptured;
    private Throwable mainError;

    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }

    private void main(final Runnable work) throws Exception {
        mainError = null;
        runOnMainSync(() -> { try { work.run(); } catch (Throwable error) { mainError = error; } });
        if (mainError != null) throw new Exception("Main-thread probe failed", mainError);
    }

    private Object field(String name) {
        try { Field item = managerClass.getDeclaredField(name); item.setAccessible(true); return item.get(null); }
        catch (Exception error) { throw new IllegalStateException(error); }
    }

    private Object property(Object owner, String method) {
        try { Method item = owner.getClass().getDeclaredMethod(method); item.setAccessible(true); return item.invoke(owner); }
        catch (Exception error) { throw new IllegalStateException(error); }
    }

    private Object call(String name, Class<?>[] types, Object... args) {
        try { return managerClass.getDeclaredMethod(name, types).invoke(manager, args); }
        catch (Exception error) { throw new IllegalStateException(error); }
    }

    private void call(String name) { call(name, new Class<?>[0]); }
    private void init() { call("init", new Class<?>[] { Context.class }, getTargetContext()); }
    private Object current(String resource) { return property(field(resource), "getCurrent"); }
    private Object ready(String resource) { return property(field(resource), "getReady"); }
    private boolean canAcquire(String resource) { return (Boolean) property(field(resource), "getCanAcquire"); }
    private void assertTrue(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    private void checked(String label) { checks.put(label); }

    private void preference(String key, Object value) {
        SharedPreferences.Editor edit = prefs.edit();
        if (value instanceof Boolean) edit.putBoolean(key, (Boolean) value);
        else if (value instanceof Integer) edit.putInt(key, (Integer) value);
        else edit.putString(key, (String) value);
        assertTrue(edit.commit(), "Preference commit failed: " + key);
    }

    private void resetDisabled() {
        preference("speak_on_keypress", false);
        preference("speak_on_commit", false);
        preference("sound_on_keypress", false);
        preference("custom_sound_effect_enabled", false);
        preference("sound_volume", 20);
        call("destroy");
        init();
    }

    private void queuedRequest(boolean destroy) throws Exception {
        main(() -> {
            resetDisabled();
            preference("speak_on_commit", true);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread worker = new Thread(() -> {
                try { call("textCommitSpeak", new Class<?>[] { String.class }, "fictional queued 731"); }
                catch (Throwable error) { failure.set(error); }
            }, "ReadinessSpeechWorker");
            worker.start();
            try { worker.join(3000); } catch (InterruptedException error) { throw new IllegalStateException(error); }
            assertTrue(!worker.isAlive() && failure.get() == null, "Worker did not enqueue cleanly");
            assertTrue(current("speech") == null, "Worker touched owner resource before Looper delivery");
            if (destroy) { call("destroy"); init(); } else call("finishInput");
        });
        waitForIdleSync();
        main(() -> {
            assertTrue(current("speech") == null && field("pendingSpeech") == null, "Old queued request survived lifecycle change");
        });
        checked(destroy ? "real worker queued speech rejected after destroy/init" : "real worker queued speech rejected after finishInput");
    }

    private void makeSound() throws Exception {
        File soundRoot = new File(getTargetContext().getExternalFilesDir(null), "rime/soundeffect");
        assertTrue(soundRoot.isDirectory() || soundRoot.mkdirs(), "No soundeffect directory");
        File descriptor = new File(soundRoot, "readiness_probe.sound.yaml");
        File directory = new File(soundRoot, "readiness_probe");
        assertTrue(!descriptor.exists() && !directory.exists(), "Fresh sound fixture required");
        assertTrue(directory.mkdir(), "Cannot create sound fixture directory");
        soundDescriptor = descriptor;
        soundDirectory = directory;
        soundFile = new File(soundDirectory, "tone.wav");
        int count = 1600;
        byte[] bytes = new byte[44 + count * 2];
        putAscii(bytes, 0, "RIFF"); putInt(bytes, 4, bytes.length - 8); putAscii(bytes, 8, "WAVEfmt ");
        putInt(bytes, 16, 16); putShort(bytes, 20, 1); putShort(bytes, 22, 1);
        putInt(bytes, 24, 8000); putInt(bytes, 28, 16000); putShort(bytes, 32, 2); putShort(bytes, 34, 16);
        putAscii(bytes, 36, "data"); putInt(bytes, 40, count * 2);
        for (int i = 0; i < count; i++) putShort(bytes, 44 + i * 2, (int) (Math.sin(i * 2 * Math.PI * 440 / 8000) * 4000));
        try (FileOutputStream stream = new FileOutputStream(soundFile)) { stream.write(bytes); }
        try (FileOutputStream stream = new FileOutputStream(soundDescriptor)) {
            stream.write("name: readiness_probe\nfolder: readiness_probe\nsound: [tone.wav]\nkeyset: []\n".getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void putAscii(byte[] bytes, int offset, String value) {
        for (int i = 0; i < value.length(); i++) bytes[offset + i] = (byte) value.charAt(i);
    }
    private static void putShort(byte[] bytes, int offset, int value) { bytes[offset] = (byte) value; bytes[offset + 1] = (byte) (value >>> 8); }
    private static void putInt(byte[] bytes, int offset, int value) { putShort(bytes, offset, value); putShort(bytes, offset + 2, value >>> 16); }

    private void testSounds() throws Exception {
        makeSound();
        main(() -> {
            resetDisabled();
            preference("sound_on_keypress", true);
            call("keyPressSound", new Class<?>[] { int.class }, KeyEvent.KEYCODE_A);
            assertTrue(current("sounds") == null, "System sound unexpectedly allocated a custom SoundPool");
            preference("custom_sound_effect_enabled", true);
            preference("sound_volume", 0);
            call("startInput");
            call("keyPressSound", new Class<?>[] { int.class }, KeyEvent.KEYCODE_A);
            assertTrue(current("sounds") == null, "Muted custom sound allocated a SoundPool");
            preference("sound_volume", 20);
            try {
                Class<?> effects = getTargetContext().getClassLoader().loadClass("com.osfans.trime.data.soundeffect.SoundEffectManager");
                activeSoundEffect = effects.getDeclaredField("activeSoundEffect");
                activeSoundEffect.setAccessible(true);
                savedSoundEffect = activeSoundEffect.get(null);
                savedSoundEffectCaptured = true;
                effects.getMethod("switchEffect", String.class).invoke(effects.getField("INSTANCE").get(null), "readiness_probe");
            } catch (Exception error) { throw new IllegalStateException(error); }
            assertTrue(current("sounds") instanceof android.media.SoundPool && ready("sounds") == current("sounds"), "Real SoundPool not allocated");
        });
        long end = SystemClock.uptimeMillis() + 15000;
        boolean[] loaded = new boolean[1];
        while (SystemClock.uptimeMillis() < end) {
            main(() -> loaded[0] = ((Set<?>) field("readySounds")).size() == 1 && ((SparseIntArray) field("cachedSoundIds")).size() == 1);
            if (loaded[0]) break;
            SystemClock.sleep(50);
        }
        assertTrue(loaded[0], "WAV did not load through real SoundPool callback");
        main(() -> {
            call("keyPressSound", new Class<?>[] { int.class }, KeyEvent.KEYCODE_A);
            call("finishInput");
            assertTrue(current("sounds") != null, "finishInput unexpectedly discarded reusable sound resource");
            call("destroy");
            assertTrue(current("sounds") == null && ((Set<?>) field("readySounds")).isEmpty() && ((SparseIntArray) field("cachedSoundIds")).size() == 0 && ((Map<?, ?>) field("loadedSounds")).isEmpty(), "Sound destroy retained resource/cache");
        });
        checked("system sound does not allocate custom pool; muted custom feedback allocates none");
        checked("real SoundPool loads WAV and manager receives load callback, plays, retains across finish and clears on destroy");
    }

    private void testSpeech() throws Exception {
        final Object[] acquired = new Object[1];
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<String> speechFailure = new AtomicReference<>();
        main(() -> {
            resetDisabled();
            preference("speak_on_commit", true);
            call("textCommitSpeak", new Class<?>[] { String.class }, "fictional initial 731");
            acquired[0] = current("speech");
            if (acquired[0] instanceof TextToSpeech) {
                // Still in the same main-loop task: the manager's posted initialization
                // callback cannot consume the initial pending utterance before this listener.
                ((TextToSpeech) acquired[0]).setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String id) { started.countDown(); }
                    @Override public void onDone(String id) { done.countDown(); }
                    // Android still declares this legacy callback abstract; implement it for API 21.
                    @SuppressWarnings("deprecation")
                    @Override public void onError(String id) { speechFailure.set(id); done.countDown(); }
                    @Override public void onError(String id, int code) { speechFailure.set(id + ":" + code); done.countDown(); }
                });
            }
        });
        long end = SystemClock.uptimeMillis() + 20000;
        boolean[] initialized = new boolean[1];
        boolean[] failed = new boolean[1];
        while (SystemClock.uptimeMillis() < end) {
            main(() -> { initialized[0] = ready("speech") != null; failed[0] = !canAcquire("speech"); });
            if (initialized[0] || failed[0]) break;
            SystemClock.sleep(50);
        }
        JSONObject tts = new JSONObject();
        TextToSpeech engine = (TextToSpeech) acquired[0];
        JSONArray engines = new JSONArray();
        if (engine != null) for (TextToSpeech.EngineInfo info : engine.getEngines()) engines.put(info.name);
        tts.put("engines", engines);
        tts.put("initialized", initialized[0]);
        tts.put("failedInitialization", failed[0]);
        assertTrue(initialized[0] || failed[0], "TTS initialization did not finish within 20 seconds");
        if (initialized[0]) {
            boolean start = started.await(15, TimeUnit.SECONDS);
            boolean finish = done.await(15, TimeUnit.SECONDS);
            tts.put("utteranceStart", start); tts.put("utteranceDone", finish); tts.put("utteranceError", speechFailure.get());
            assertTrue(start && finish && speechFailure.get() == null, "Real TTS utterance failed");
            main(() -> {
                call("finishInput"); assertTrue(field("pendingSpeech") == null && ready("speech") != null, "Speech finish lifecycle mismatch");
                call("destroy"); assertTrue(current("speech") == null && field("applicationContext") == null, "Speech destroy retained resources");
            });
            checked("real TTS initializes, manager utterance starts/completes, finish clears pending and destroy clears resource");
        } else {
            main(() -> {
                assertTrue(current("speech") == null && field("pendingSpeech") == null, "Failed TTS retained resource/pending");
                call("textCommitSpeak", new Class<?>[] { String.class }, "fictional retry 732");
                assertTrue(current("speech") == null, "Failed TTS retried on every request");
                call("destroy");
            });
            checked("real TTS initialization failure releases resource, clears pending and avoids per-key retry");
        }
        report.put("tts", tts);
    }

    private void cleanup() {
        if (manager != null) call("destroy");
        if (savedSoundEffectCaptured) {
            try { activeSoundEffect.set(null, savedSoundEffect); }
            catch (Exception error) { throw new IllegalStateException(error); }
        }
        if (prefs != null) {
            SharedPreferences.Editor edit = prefs.edit();
            for (Map.Entry<String, Object> entry : saved.entrySet()) {
                Object value = entry.getValue();
                if (value == null) edit.remove(entry.getKey());
                else if (value instanceof Boolean) edit.putBoolean(entry.getKey(), (Boolean) value);
                else if (value instanceof Integer) edit.putInt(entry.getKey(), (Integer) value);
                else edit.putString(entry.getKey(), (String) value);
            }
            assertTrue(edit.commit(), "Feedback preference restore failed");
        }
        for (File file : new File[] { soundFile, soundDescriptor, soundDirectory }) {
            if (file != null && file.exists()) assertTrue(file.delete(), "Sound fixture cleanup failed: " + file);
        }
    }

    @Override public void onStart() {
        Bundle result = new Bundle();
        boolean passed = false;
        try {
            // Instrumentation.start() may race the target Application.onCreate().
            // Reach its first idle before reading production preference-dependent singletons.
            waitForIdleSync();
            Context target = getTargetContext();
            File candidate = new File(target.getCacheDir(), "readiness-feedback");
            assertTrue(candidate.mkdir(), "Fresh feedback output required");
            output = candidate;
            prefs = target.getSharedPreferences(target.getPackageName() + "_preferences", Context.MODE_PRIVATE);
            for (String key : new String[] { "speak_on_keypress", "speak_on_commit", "sound_on_keypress", "custom_sound_effect_enabled", "sound_volume", "custom_sound_effect_name" }) saved.put(key, prefs.getAll().get(key));
            main(() -> {
                try {
                    managerClass = target.getClassLoader().loadClass("com.osfans.trime.ime.keyboard.InputFeedbackManager");
                    manager = managerClass.getField("INSTANCE").get(null);
                } catch (Exception error) { throw new IllegalStateException(error); }
            });
            main(() -> {
                resetDisabled(); call("startInput");
                call("keyPressSpeak", new Class<?>[] { int.class }, KeyEvent.KEYCODE_A);
                call("textCommitSpeak", new Class<?>[] { String.class }, "fictional disabled 730");
                call("keyPressSound", new Class<?>[] { int.class }, KeyEvent.KEYCODE_A);
                assertTrue(current("speech") == null && current("sounds") == null && field("pendingSpeech") == null, "Disabled/init feedback allocated resources");
            });
            checked("disabled initialization and requests allocate no TTS or SoundPool");
            queuedRequest(false); queuedRequest(true);
            testSounds(); testSpeech();
            passed = true;
        } catch (Throwable error) {
            try { report.put("failure", android.util.Log.getStackTraceString(error)); } catch (Exception ignored) { }
        } finally {
            try { main(this::cleanup); } catch (Throwable error) {
                passed = false;
                try { report.put("cleanupFailure", android.util.Log.getStackTraceString(error)); } catch (Exception ignored) { }
            }
            try {
                report.put("passed", passed); report.put("checks", checks);
                report.put("pid", android.os.Process.myPid()); report.put("sdk", android.os.Build.VERSION.SDK_INT);
                report.put("targetApk", getTargetContext().getApplicationInfo().sourceDir);
                report.put("scope", "Real target manager/Looper, real SoundPool WAV load callback and TTS initialization; TTS speech only if ready engine reports start/done. No physical audibility, actual keyboard gestures or observed stale native callback claim");
                if (output != null) try (FileOutputStream stream = new FileOutputStream(new File(output, "result.json"))) { stream.write(report.toString(2).getBytes(StandardCharsets.UTF_8)); }
                result.putString("report", report.toString());
            } catch (Exception error) { result.putString("reportFailure", android.util.Log.getStackTraceString(error)); passed = false; }
            finish(passed ? Activity.RESULT_OK : Activity.RESULT_CANCELED, result);
        }
    }
}
