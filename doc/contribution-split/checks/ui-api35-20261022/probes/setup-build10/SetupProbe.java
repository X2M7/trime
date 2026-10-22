/* SPDX-License-Identifier: GPL-3.0-or-later */
package org.x2m7.trime.readiness.ui;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.ScrollView;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import org.json.JSONArray;
import org.json.JSONObject;

/** Actual SetupActivity geometry and injected touch; does not call performClick. */
public final class SetupProbe extends Instrumentation {
    private final JSONObject report = new JSONObject();
    private final JSONArray frames = new JSONArray();
    private final JSONArray touches = new JSONArray();
    private final JSONArray scrollGestures = new JSONArray();
    private final JSONArray reachable = new JSONArray();
    private final JSONArray reachabilityObservations = new JSONArray();
    private Activity activity;
    private File output;
    private SharedPreferences prefs;
    private final Map<String, Object> saved = new HashMap<>();
    private Throwable mainError;
    private float expectedFontScale = 1.0f;

    @Override public void onCreate(Bundle args) {
        super.onCreate(args);
        if (args != null) expectedFontScale = Float.parseFloat(args.getString("fontScale", "1.0"));
        start();
    }

    private void main(final Runnable work) throws Exception {
        mainError = null;
        runOnMainSync(() -> {
            try { work.run(); } catch (Throwable error) { mainError = error; }
        });
        if (mainError != null) throw new Exception("Main-thread probe failed", mainError);
    }

    private View view(String name) {
        int id = getTargetContext().getResources().getIdentifier(name, "id", getTargetContext().getPackageName());
        View scope = activity.getWindow().getDecorView();
        if (!name.equals("viewpager") && !name.equals("next_button") && !name.equals("prev_button") && !name.equals("skip_button")) {
            try {
                Object fragments = activity.getClass().getMethod("getSupportFragmentManager").invoke(activity);
                Object fragment = fragments.getClass().getMethod("findFragmentByTag", String.class).invoke(fragments, "f" + page());
                if (fragment == null) throw new IllegalStateException("No current setup fragment");
                scope = (View) fragment.getClass().getMethod("getView").invoke(fragment);
            } catch (Exception error) { throw new IllegalStateException(error); }
        }
        View result = scope.findViewById(id);
        if (result == null) throw new IllegalStateException("Missing view: " + name);
        return result;
    }

    private int page() {
        try { return (Integer) view("viewpager").getClass().getMethod("getCurrentItem").invoke(view("viewpager")); }
        catch (Exception error) { throw new IllegalStateException(error); }
    }

    private void waitForPage(int expected) throws Exception {
        long end = SystemClock.uptimeMillis() + 8000;
        boolean[] found = new boolean[1];
        while (SystemClock.uptimeMillis() < end) {
            waitForIdleSync();
            main(() -> {
                try {
                    int scroll = (Integer) view("viewpager").getClass().getMethod("getScrollState").invoke(view("viewpager"));
                    int actualPage = page();
                    JSONObject observation = new JSONObject();
                    observation.put("requestedPage", expected);
                    observation.put("actualPage", actualPage);
                    observation.put("scrollState", scroll);
                    observation.put("activityHasWindowFocus", activity.hasWindowFocus());
                    observation.put("decorHasWindowFocus", activity.getWindow().getDecorView().hasWindowFocus());
                    observation.put("activityFinishing", activity.isFinishing());
                    observation.put("decorShown", activity.getWindow().getDecorView().isShown());
                    observation.put("uptimeMillis", SystemClock.uptimeMillis());
                    report.put("lastPageObservation", observation);
                    found[0] = activity.hasWindowFocus() && actualPage == expected && scroll == 0;
                } catch (Exception error) { throw new IllegalStateException(error); }
            });
            if (found[0]) { SystemClock.sleep(300); return; }
            SystemClock.sleep(50);
        }
        try { capture("failure-wait-page-" + expected); }
        catch (Throwable error) { report.put("failureCaptureError", android.util.Log.getStackTraceString(error)); }
        throw new IllegalStateException("Page/window not ready: " + expected);
    }

    private void touch(String name) throws Exception {
        Rect[] bounds = new Rect[1];
        main(() -> {
            View item = view(name);
            Rect clipped = new Rect();
            if (!item.isShown() || !item.isEnabled() || !item.getLocalVisibleRect(clipped) || clipped.isEmpty()) {
                throw new IllegalStateException("Not touchable: " + name);
            }
            int[] screen = new int[2];
            item.getLocationOnScreen(screen);
            clipped.offset(screen[0] - item.getScrollX(), screen[1] - item.getScrollY());
            bounds[0] = clipped;
        });
        long now = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, bounds[0].centerX(), bounds[0].centerY(), 0);
        MotionEvent up = MotionEvent.obtain(now, now + 60, MotionEvent.ACTION_UP, bounds[0].centerX(), bounds[0].centerY(), 0);
        down.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        up.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        try { sendPointerSync(down); SystemClock.sleep(60); sendPointerSync(up); }
        finally { down.recycle(); up.recycle(); }
        JSONObject touch = new JSONObject();
        touch.put("view", name);
        touch.put("screenVisibleRect", new JSONArray(new int[] { bounds[0].left, bounds[0].top, bounds[0].right, bounds[0].bottom }));
        touch.put("screenX", bounds[0].centerX()); touch.put("screenY", bounds[0].centerY());
        touch.put("downUptimeMillis", now); touch.put("upEventUptimeMillis", now + 60);
        touches.put(touch);
        waitForIdleSync();
    }

    private JSONObject geometry(String name) throws Exception {
        View item = view(name);
        int[] xy = new int[2];
        item.getLocationOnScreen(xy);
        Rect clipped = new Rect();
        boolean visible = item.getLocalVisibleRect(clipped);
        JSONArray localVisibleRect = new JSONArray(new int[] { clipped.left, clipped.top, clipped.right, clipped.bottom });
        clipped.offset(xy[0] - item.getScrollX(), xy[1] - item.getScrollY());
        Rect rootVisible = new Rect();
        item.getGlobalVisibleRect(rootVisible);
        JSONObject result = new JSONObject();
        result.put("shown", item.isShown());
        result.put("enabled", item.isEnabled());
        result.put("visibility", item.getVisibility());
        result.put("rect", new JSONArray(new int[] { xy[0], xy[1], xy[0] + item.getWidth(), xy[1] + item.getHeight() }));
        result.put("rectCoordinates", "screen full view bounds");
        result.put("localVisibleRect", localVisibleRect);
        result.put("rootVisibleRect", new JSONArray(new int[] { rootVisible.left, rootVisible.top, rootVisible.right, rootVisible.bottom }));
        result.put("visibleRect", new JSONArray(new int[] { clipped.left, clipped.top, clipped.right, clipped.bottom }));
        result.put("visibleRectCoordinates", "screen; localVisibleRect minus own scrollX/Y then translated by getLocationOnScreen");
        result.put("hasVisibleRect", visible);
        if (item instanceof TextView) result.put("text", ((TextView) item).getText().toString());
        return result;
    }

    private int[] screenBounds(View item) {
        int[] point = new int[2]; item.getLocationOnScreen(point);
        return new int[] { point[0], point[1], point[0] + item.getWidth(), point[1] + item.getHeight() };
    }

    private ScrollView scroller() {
        View item = view("setup_scroll");
        if (!(item instanceof ScrollView)) throw new IllegalStateException("Expected new setup_scroll root");
        return (ScrollView) item;
    }

    private void swipe(boolean towardBottom) throws Exception { swipe(towardBottom, Float.NaN); }

    private void swipe(boolean towardBottom, float requestedDistance) throws Exception {
        final Rect[] bounds = new Rect[1];
        final int[] before = new int[1];
        main(() -> {
            ScrollView scroll = scroller(); Rect local = new Rect();
            if (!scroll.getLocalVisibleRect(local)) throw new IllegalStateException("Scroll root not visible");
            int[] point = new int[2]; scroll.getLocationOnScreen(point); local.offset(point[0] - scroll.getScrollX(), point[1] - scroll.getScrollY());
            bounds[0] = local; before[0] = scroll.getScrollY();
        });
        Rect area = bounds[0];
        float x = area.centerX();
        float start = area.top + area.height() * (towardBottom ? 0.8f : 0.2f);
        float distance = Float.isNaN(requestedDistance) ? area.height() * 0.6f : Math.min(area.height() * 0.6f, Math.max(24f, requestedDistance + 16f));
        float finish = start + (towardBottom ? -distance : distance);
        long downTime = SystemClock.uptimeMillis();
        for (int i = 0; i <= 9; i++) {
            int action = i == 0 ? MotionEvent.ACTION_DOWN : i == 9 ? MotionEvent.ACTION_UP : MotionEvent.ACTION_MOVE;
            float fraction = Math.min(i, 8) / 8f;
            MotionEvent event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, start + (finish - start) * fraction, 0);
            event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
            try { sendPointerSync(event); } finally { event.recycle(); }
            if (i < 9) SystemClock.sleep(70);
        }
        int previous = Integer.MIN_VALUE; int stable = 0; int[] after = new int[1];
        for (int i = 0; i < 20; i++) {
            waitForIdleSync(); main(() -> after[0] = scroller().getScrollY());
            stable = after[0] == previous ? stable + 1 : 0; previous = after[0];
            if (stable == 2) break;
            SystemClock.sleep(50);
        }
        if (stable != 2) throw new IllegalStateException("Scroll did not settle in the bounded observation");
        JSONObject item = new JSONObject();
        item.put("towardBottom", towardBottom); item.put("beforeScrollY", before[0]); item.put("afterScrollY", after[0]);
        item.put("screenRect", new JSONArray(new int[] { area.left, area.top, area.right, area.bottom }));
        item.put("screenX", x); item.put("screenStartY", start); item.put("screenEndY", finish);
        scrollGestures.put(item);
    }

    private void ensureFullyVisible(String name) throws Exception {
        boolean[] full = new boolean[1]; boolean[] towardBottom = new boolean[1]; float[] missingDistance = new float[1];
        for (int attempt = 0; attempt <= 8; attempt++) {
            main(() -> {
                View item = view(name); Rect local = new Rect();
                full[0] = item.isShown() && item.getWidth() > 0 && item.getHeight() > 0 && item.getLocalVisibleRect(local) &&
                    local.left == 0 && local.top == 0 && local.right == item.getWidth() && local.bottom == item.getHeight();
                int[] child = screenBounds(item); int[] parent = screenBounds(scroller());
                towardBottom[0] = child[3] > parent[3] - scroller().getPaddingBottom();
                missingDistance[0] = towardBottom[0] ? child[3] - (parent[3] - scroller().getPaddingBottom()) : parent[1] + scroller().getPaddingTop() - child[1];
                try { JSONObject observation = new JSONObject(); observation.put("view", name); observation.put("geometry", geometry(name)); observation.put("scrollY", scroller().getScrollY()); observation.put("missingDistance", missingDistance[0]); observation.put("full", full[0]); reachabilityObservations.put(observation); } catch (Exception error) { throw new IllegalStateException(error); }
            });
            if (full[0]) {
                main(() -> {
                    try {
                        JSONObject item = new JSONObject(); item.put("view", name); item.put("geometry", geometry(name));
                        item.put("scrollY", scroller().getScrollY()); reachable.put(item);
                    } catch (Exception error) { throw new IllegalStateException(error); }
                });
                return;
            }
            if (attempt < 8) swipe(towardBottom[0], missingDistance[0]);
        }
        throw new IllegalStateException("View cannot become fully visible by bounded real swipes: " + name);
    }

    private void checkScrollReachability() throws Exception {
        final boolean[] direction = new boolean[1]; final int[] initialY = new int[1];
        final Map<String, int[]> navigation = new HashMap<>();
        main(() -> {
            ScrollView scroll = scroller(); initialY[0] = scroll.getScrollY();
            if (!scroll.canScrollVertically(1) && !scroll.canScrollVertically(-1)) throw new IllegalStateException("Short-screen page did not become scrollable");
            direction[0] = scroll.canScrollVertically(1);
            for (String id : new String[] { "viewpager", "next_button", "skip_button" }) navigation.put(id, screenBounds(view(id)));
        });
        swipe(direction[0]);
        main(() -> { if (scroller().getScrollY() == initialY[0]) throw new IllegalStateException("Real swipe did not change scrollY"); });
        for (String id : new String[] { "app_specific_storage_desc", "sync_from_external_desc", "app_specific_storage_option", "sync_from_external_option", "hint_text", "step_text", "app_icon" }) ensureFullyVisible(id);
        capture("mode-app-selected-scroll-top");
        ensureFullyVisible("done_icon"); ensureFullyVisible("done_text");
        main(() -> {
            for (Map.Entry<String, int[]> item : navigation.entrySet()) {
                if (!Arrays.equals(item.getValue(), screenBounds(view(item.getKey())))) throw new IllegalStateException("Inner scrolling moved navigation: " + item.getKey());
            }
            if (!"APP_STORAGE".equals(prefs.getString("profile_data_storage_mode", ""))) throw new IllegalStateException("Swiping changed selected storage mode");
        });
    }

    private void capture(String name) throws Exception {
        waitForIdleSync();
        JSONObject frame = new JSONObject();
        main(() -> {
            try {
                frame.put("name", name);
                frame.put("page", page());
                frame.put("fontScale", activity.getResources().getConfiguration().fontScale);
                if (Math.abs(activity.getResources().getConfiguration().fontScale - expectedFontScale) > 0.01f) throw new IllegalStateException("Actual fontScale differs from requested probe configuration");
                frame.put("stepTextSizePx", ((TextView) view("step_text")).getTextSize());
                frame.put("densityDpi", activity.getResources().getDisplayMetrics().densityDpi);
                for (String id : new String[] { "viewpager", "app_icon", "step_text", "hint_text", "storage_mode_options", "action_button", "done_text", "done_icon", "next_button", "prev_button", "skip_button" }) {
                    frame.put(id, geometry(id));
                }
                ScrollView scroll = scroller();
                frame.put("setup_scroll", geometry("setup_scroll")); frame.put("scrollY", scroll.getScrollY());
                frame.put("scrollChildHeight", scroll.getChildAt(0).getHeight()); frame.put("scrollPaddingTop", scroll.getPaddingTop()); frame.put("scrollPaddingBottom", scroll.getPaddingBottom());
                int footerTop = Integer.MAX_VALUE;
                for (String id : new String[] { "next_button", "prev_button", "skip_button" }) if (view(id).isShown()) footerTop = Math.min(footerTop, screenBounds(view(id))[1]);
                if (screenBounds(view("viewpager"))[3] != footerTop) throw new IllegalStateException("Pager is not bounded above visible navigation");
            } catch (Exception error) { throw new IllegalStateException(error); }
        });
        Bitmap bitmap = getUiAutomation().takeScreenshot();
        if (bitmap == null) throw new IllegalStateException("No screenshot");
        File screenshot = new File(output, name + ".png");
        try (FileOutputStream stream = new FileOutputStream(screenshot)) {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) throw new IllegalStateException("PNG encode failed");
        } finally { bitmap.recycle(); }
        frame.put("png", screenshot.getName());
        frames.put(frame);
    }

    private void save(String key) { saved.put(key, prefs.getAll().get(key)); }

    private void restore() {
        if (prefs == null) return;
        SharedPreferences.Editor edit = prefs.edit();
        for (Map.Entry<String, Object> entry : saved.entrySet()) {
            if (entry.getValue() == null) edit.remove(entry.getKey());
            else edit.putString(entry.getKey(), (String) entry.getValue());
        }
        if (!edit.commit()) throw new IllegalStateException("Preference restore failed");
    }

    @Override public void onStart() {
        Bundle result = new Bundle();
        boolean passed = false;
        try {
            Context target = getTargetContext();
            File candidate = new File(target.getCacheDir(), "readiness-setup");
            if (!candidate.mkdir()) throw new IllegalStateException("Fresh output required: " + candidate);
            output = candidate;
            prefs = target.getSharedPreferences(target.getPackageName() + "_preferences", Context.MODE_PRIVATE);
            save("profile_data_storage_mode");
            save("profile_external_rime_tree_uri");
            main(() -> {
                if (!prefs.edit().putString("profile_data_storage_mode", "external_sync").putString("profile_external_rime_tree_uri", "").commit()) {
                    throw new IllegalStateException("Setup precondition commit failed");
                }
            });
            Intent launch = new Intent().setClassName(target.getPackageName(), "com.osfans.trime.ui.setup.SetupActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity = startActivitySync(launch);
            waitForPage(0);
            capture("mode-external");
            ensureFullyVisible("app_specific_storage_option");
            touch("app_specific_storage_option");
            SystemClock.sleep(300);
            capture("mode-app-selected-before-scroll");
            main(() -> {
                if (!"APP_STORAGE".equals(prefs.getString("profile_data_storage_mode", ""))) throw new IllegalStateException("Radio touch did not change storage mode");
            });
            checkScrollReachability(); capture("mode-app-selected");
            touch("next_button"); waitForPage(1); capture("enable-after-next");
            touch("prev_button"); waitForPage(0); ensureFullyVisible("done_icon"); ensureFullyVisible("done_text"); capture("mode-after-prev");
            touch("next_button"); waitForPage(1);
            touch("next_button"); waitForPage(2); capture("select-next-gone");
            main(() -> {
                if (view("next_button").getVisibility() != View.GONE) throw new IllegalStateException("Expected hidden Next on incomplete Select page");
            });
            passed = true;
        } catch (Throwable error) {
            try { report.put("failure", android.util.Log.getStackTraceString(error)); } catch (Exception ignored) { }
        } finally {
            try {
                main(() -> { if (activity != null) activity.finish(); restore(); });
            } catch (Throwable error) {
                passed = false;
                try { report.put("cleanupFailure", android.util.Log.getStackTraceString(error)); } catch (Exception ignored) { }
            }
            try {
                report.put("passed", passed);
                report.put("frames", frames);
                report.put("touches", touches);
                report.put("scrollGestures", scrollGestures); report.put("fullyReachableViews", reachable); report.put("reachabilityObservations", reachabilityObservations); report.put("expectedFontScale", expectedFontScale);
                report.put("pid", android.os.Process.myPid());
                report.put("sdk", android.os.Build.VERSION.SDK_INT);
                report.put("targetApk", getTargetContext().getApplicationInfo().sourceDir);
                report.put("scope", "Real target SetupActivity, five navigation steps plus bounded real vertical swipes and complete visible bounds for Done/icon/text. Actual fontScale is checked; no complete onboarding or upgrade acceptance");
                if (output != null) try (FileOutputStream stream = new FileOutputStream(new File(output, "result.json"))) {
                    stream.write(report.toString(2).getBytes(StandardCharsets.UTF_8));
                }
                result.putString("report", report.toString());
            } catch (Exception error) { result.putString("reportFailure", android.util.Log.getStackTraceString(error)); passed = false; }
            finish(passed ? Activity.RESULT_OK : Activity.RESULT_CANCELED, result);
        }
    }
}
