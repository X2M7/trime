/* SPDX-License-Identifier: GPL-3.0-or-later */
package org.x2m7.trime.readiness.candidate;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.json.JSONArray;
import org.json.JSONObject;

/** Small Android rendering regression, separate from native-candidate acceptance. */
public final class CandidateProbe extends Instrumentation {
    private final JSONObject report = new JSONObject();
    private final JSONArray stages = new JSONArray();
    private Throwable mainError;

    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }

    private void main(Runnable work) throws Exception {
        mainError = null;
        runOnMainSync(() -> { try { work.run(); } catch (Throwable error) { mainError = error; } });
        if (mainError != null) throw new Exception("Main-thread production-view operation failed", mainError);
    }

    private static String sha(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream stream = new FileInputStream(file)) {
            byte[] buffer = new byte[65536]; int count;
            while ((count = stream.read(buffer)) != -1) digest.update(buffer, 0, count);
        }
        StringBuilder result = new StringBuilder();
        for (byte value : digest.digest()) result.append(String.format(java.util.Locale.ROOT, "%02x", value & 255));
        return result.toString();
    }

    private static void png(Bitmap bitmap, File path) throws Exception {
        try (FileOutputStream output = new FileOutputStream(path)) {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) throw new IllegalStateException("PNG failed");
        }
    }

    private static int ink(Bitmap bitmap, Rect bounds) {
        int count = 0;
        for (int y = bounds.top; y < bounds.bottom; y++) for (int x = bounds.left; x < bounds.right; x++) {
            int pixel = bitmap.getPixel(x, y);
            if (Color.alpha(pixel) >= 128 && Color.red(pixel) + Color.green(pixel) + Color.blue(pixel) <= 162) count++;
        }
        return count;
    }

    private JSONObject capture(TextView view, Bitmap screen, File directory, String name, String expected) throws Exception {
        int[] point = new int[2]; view.getLocationOnScreen(point);
        Rect bounds = new Rect(point[0], point[1], point[0] + view.getWidth(), point[1] + view.getHeight());
        if (bounds.isEmpty() || bounds.left < 0 || bounds.top < 0 || bounds.right > screen.getWidth() || bounds.bottom > screen.getHeight()) throw new IllegalStateException("Label not fully on screen");
        int screenInk = ink(screen, bounds);
        String textBeforeNode = view.getText().toString();
        String layoutBeforeNode = view.getLayout() == null ? null : view.getLayout().getText().toString();
        AccessibilityNodeInfo node = view.createAccessibilityNodeInfo();
        String accessible;
        try { accessible = node.getText() == null ? "" : node.getText().toString(); }
        finally { recycleNode(node); }
        String textAfterNode = view.getText().toString();
        String layout = view.getLayout() == null ? null : view.getLayout().getText().toString();
        Bitmap software = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(software);
        canvas.translate(-view.getScrollX(), -view.getScrollY());
        view.draw(canvas);
        int softwareInk = ink(software, new Rect(0, 0, software.getWidth(), software.getHeight()));
        File softwareFile = new File(directory, name + ".png");
        png(software, softwareFile); software.recycle();
        boolean glyphs = expected.isEmpty() ? screenInk == 0 && softwareInk == 0 : screenInk > 0 && softwareInk > 0 && screenInk >= Math.max(1, softwareInk / 4);
        return new JSONObject().put("text", view.getText().toString()).put("frameworkLayoutText", layout == null ? JSONObject.NULL : layout)
                .put("textBeforeAccessibilityNode", textBeforeNode).put("frameworkLayoutTextBeforeAccessibilityNode", layoutBeforeNode == null ? JSONObject.NULL : layoutBeforeNode)
                .put("textAfterAccessibilityNode", textAfterNode).put("frameworkLayoutTextAfterAccessibilityNode", layout == null ? JSONObject.NULL : layout)
                .put("accessibilityText", accessible).put("bounds", new JSONArray(new int[] {bounds.left, bounds.top, bounds.right, bounds.bottom}))
                .put("textColorArgb", view.getCurrentTextColor() & 0xffffffffL).put("textSizePx", view.getTextSize())
                .put("paintColorArgb", view.getPaint().getColor() & 0xffffffffL).put("paintTextScaleX", view.getPaint().getTextScaleX())
                .put("scrollX", view.getScrollX()).put("scrollY", view.getScrollY()).put("textScaleX", view.getTextScaleX())
                .put("screenInk", screenInk).put("softwareInk", softwareInk).put("softwareSha256", sha(softwareFile))
                .put("textAndLayoutPassed", view.getText().toString().equals(expected) && expected.equals(layout))
                .put("accessibilityPassed", accessible.equals(expected)).put("glyphPassed", glyphs);
    }

    // API 21 requires recycling; the method is deprecated on newer platforms.
    @SuppressWarnings("deprecation")
    private static void recycleNode(AccessibilityNodeInfo node) { node.recycle(); }

    @Override public void onStart() {
        File directory = null; CandidateActivity activity = null; boolean passed = false;
        Bundle result = new Bundle();
        try {
            waitForIdleSync();
            String id = UUID.randomUUID().toString();
            directory = new File(getTargetContext().getCacheDir(), "candidate-layout-" + id);
            if (!directory.mkdir()) throw new IllegalStateException("Fresh output directory required");
            activity = (CandidateActivity) startActivitySync(new Intent(getTargetContext(), CandidateActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            final CandidateActivity owner = activity;
            final boolean[] ready = {false};
            long focusDeadline = SystemClock.uptimeMillis() + 8000;
            do {
                main(() -> ready[0] = owner.hasWindowFocus() && owner.actual.isShown() && owner.actual.getWidth() > 0);
                if (ready[0]) break;
                SystemClock.sleep(50);
            } while (SystemClock.uptimeMillis() < focusDeadline);
            if (!ready[0]) throw new IllegalStateException("Own Activity did not receive focus within 8 seconds");
            report.put("runId", id).put("sourceApk", activity.sourceApk).put("sourceApkSha256", sha(new File(activity.sourceApk)))
                    .put("pid", android.os.Process.myPid()).put("sdk", android.os.Build.VERSION.SDK_INT)
                    .put("nativeOrTrimeApplicationStartedByProbe", false).put("hardwareCapturedBeforeSoftwareDraw", true);
            String[] names = {"constructor", "hanzi", "latin", "null", "rebind"};
            String[] values = {"", "你", "ni", null, "你"};
            passed = true;
            for (int i = 0; i < names.length; i++) {
                final String text = values[i]; final String name = names[i];
                final boolean bind = i != 0;
                CountDownLatch drawn = new CountDownLatch(1);
                AtomicLong observedDraw = new AtomicLong();
                ViewTreeObserver.OnDrawListener listener = () -> {
                    observedDraw.compareAndSet(0, SystemClock.uptimeMillis()); drawn.countDown();
                };
                long bindUptime = SystemClock.uptimeMillis();
                main(() -> {
                    owner.getWindow().getDecorView().getViewTreeObserver().addOnDrawListener(listener);
                    if (bind) owner.bind(text, name);
                    owner.actual.invalidate(); owner.reference.invalidate(); owner.getWindow().getDecorView().invalidate();
                });
                boolean sawDraw;
                try { sawDraw = drawn.await(3, TimeUnit.SECONDS); }
                finally { main(() -> owner.getWindow().getDecorView().getViewTreeObserver().removeOnDrawListener(listener)); }
                if (!sawDraw) throw new IllegalStateException("Actual OnDraw did not arrive within 3 seconds");
                // OnDraw and animation callbacks precede SurfaceFlinger presentation.
                // Keep the first hardware capture separate from later software View.draw().
                SystemClock.sleep(1000);
                waitForIdleSync();
                long screenshotUptime = SystemClock.uptimeMillis();
                Bitmap screen = getUiAutomation().takeScreenshot();
                if (screen == null) throw new IllegalStateException("Hardware screenshot unavailable");
                File hardware = new File(directory, name + "-screen.png"); png(screen, hardware);
                final File output = directory; final JSONObject stage = new JSONObject().put("name", name).put("screenSha256", sha(hardware))
                        .put("bindUptimeMillis", bindUptime).put("observedOnDrawUptimeMillis", observedDraw.get())
                        .put("hardwareCaptureUptimeMillis", screenshotUptime).put("minimumCompositorSettleMillis", 1000);
                final String expected = text == null ? "" : text;
                try { main(() -> {
                    try { stage.put("production", capture(owner.actual, screen, output, name + "-production", expected));
                        stage.put("reference", capture(owner.reference, screen, output, name + "-reference", expected)); }
                    catch (Exception error) { throw new IllegalStateException(error); }
                }); } finally { screen.recycle(); }
                boolean stagePassed = true;
                for (String kind : new String[] {"production", "reference"}) {
                    JSONObject label = stage.getJSONObject(kind);
                    stagePassed &= label.getBoolean("textAndLayoutPassed") && label.getBoolean("accessibilityPassed") && label.getBoolean("glyphPassed");
                }
                stage.put("passed", stagePassed); stages.put(stage); passed &= stagePassed;
            }
        } catch (Throwable error) {
            passed = false;
            try { report.put("failure", android.util.Log.getStackTraceString(error)); } catch (Exception ignored) { }
        } finally {
            if (activity != null) {
                final CandidateActivity owner = activity;
                try { main(owner::finish); } catch (Exception error) { passed = false; }
            }
            try {
                report.put("passed", passed).put("stages", stages).put("scope", "Installed production AutoScale view in a separate Activity. Does not prove native candidate integration, physical audio, or whole-app acceptance.");
                if (directory != null) try (FileOutputStream out = new FileOutputStream(new File(directory, "result.json"))) { out.write((report.toString(2) + "\n").getBytes(StandardCharsets.UTF_8)); }
                result.putString("report", report.toString()); result.putString("directory", directory == null ? "" : directory.toString());
            } catch (Exception error) { passed = false; result.putString("reportFailure", android.util.Log.getStackTraceString(error)); }
            finish(passed ? Activity.RESULT_OK : Activity.RESULT_CANCELED, result);
        }
    }
}
