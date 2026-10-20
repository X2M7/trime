package com.osfans.trime.clipheightprobe;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

public final class ProbeActivity extends Activity {
    private static final String TAG = "ClipHeightProbe";
    private TextView status;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        status = new TextView(this);
        status.setText("Measuring the two unchanged production layouts...");
        setContentView(status);
        status.post(new Runnable() { @Override public void run() { measureAll(); }});
    }

    private Context themed(float fontScale) {
        Configuration config = new Configuration(getResources().getConfiguration());
        config.fontScale = fontScale;
        return new ContextThemeWrapper(createConfigurationContext(config),
                android.R.style.Theme_DeviceDefault_Light_Dialog);
    }

    private JSONObject rect(View view) throws Exception {
        return new JSONObject().put("left", view.getLeft()).put("top", view.getTop())
                .put("right", view.getRight()).put("bottom", view.getBottom())
                .put("measuredWidth", view.getMeasuredWidth())
                .put("measuredHeight", view.getMeasuredHeight());
    }

    private JSONObject one(Context context, int layout, String text, int heightDp)
            throws Exception {
        View root = LayoutInflater.from(context).inflate(layout, null, false);
        EditText edit = (EditText) root.findViewById(R.id.clip_edit_text);
        View cancel = root.findViewById(R.id.clip_edit_cancel);
        View ok = root.findViewById(R.id.clip_edit_ok);
        edit.setText(text);
        edit.setSelection(0);
        int widthPx = Math.round(360 * context.getResources().getDisplayMetrics().density);
        int heightPx = Math.round(heightDp * context.getResources().getDisplayMetrics().density);
        for (int pass = 0; pass < 2; pass++) {
            root.measure(View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.AT_MOST));
            root.layout(0, 0, root.getMeasuredWidth(), root.getMeasuredHeight());
        }
        boolean editorAboveButtons = edit.getTop() >= 0 && edit.getBottom() <= cancel.getTop()
                && edit.getBottom() <= ok.getTop() && edit.getHeight() > 0;
        boolean buttonsInside = cancel.getTop() >= 0 && ok.getTop() >= 0
                && cancel.getBottom() <= root.getHeight() && ok.getBottom() <= root.getHeight()
                && cancel.getLeft() >= 0 && ok.getLeft() >= 0
                && cancel.getRight() <= root.getWidth() && ok.getRight() <= root.getWidth();
        return new JSONObject().put("root", rect(root)).put("editor", rect(edit))
                .put("cancel", rect(cancel)).put("ok", rect(ok))
                .put("textLength", edit.length()).put("lineCount", edit.getLineCount())
                .put("textSizePx", edit.getTextSize()).put("scrollY", edit.getScrollY())
                .put("parentHeightLimitPx", heightPx)
                .put("editorAboveButtons", editorAboveButtons)
                .put("buttonsInsideParent", buttonsInside)
                .put("textUnchanged", text.equals(edit.getText().toString()))
                .put("geometryInvariant", editorAboveButtons && buttonsInside);
    }

    private void measureAll() {
        JSONObject result = new JSONObject();
        try {
            StringBuilder longText = new StringBuilder();
            for (int i = 1; i <= 40; i++) longText.append("Line ").append(i)
                    .append(": clipboard height regression text\n");
            JSONArray pairs = new JSONArray();
            boolean contrast = false;
            boolean allFixedPass = true;
            for (float scale : new float[] {1.0f, 2.0f}) {
                Context context = themed(scale);
                for (int height : new int[] {160, 240, 600}) {
                    for (boolean longContent : new boolean[] {false, true}) {
                        String text = longContent ? longText.toString() : "Short clipboard text";
                        JSONObject baseline = one(context, R.layout.baseline_clip_edit, text, height);
                        JSONObject fixed = one(context, R.layout.fixed_clip_edit, text, height);
                        boolean before = baseline.getBoolean("geometryInvariant");
                        boolean after = fixed.getBoolean("geometryInvariant");
                        contrast |= !before && after;
                        allFixedPass &= after;
                        pairs.put(new JSONObject().put("fontScale", scale).put("widthDp", 360)
                                .put("heightLimitDp", height).put("longContent", longContent)
                                .put("baseline", baseline).put("fixed", fixed));
                    }
                }
            }
            result.put("schema", "clipboard-height-android-measure/v1")
                    .put("sdkInt", android.os.Build.VERSION.SDK_INT)
                    .put("model", android.os.Build.MODEL).put("pairs", pairs)
                    .put("actualBaselineFailureFixedPassObserved", contrast)
                    .put("allFixedGeometryCasesPass", allFixedPass)
                    .put("comparisonPass", contrast && allFixedPass)
                    .put("scope", "Unattached production XML inflated and measured on Android main thread. "
                            + "No Activity-window/IME/insets, actual scrolling, or touch acceptance.");
        } catch (Throwable error) {
            try { result.put("fatal", Log.getStackTraceString(error)); }
            catch (Exception ignored) { Log.e(TAG, "Could not encode failure", error); }
        }
        try {
            File file = new File(getFilesDir(), "result.json");
            try (FileOutputStream output = new FileOutputStream(file)) {
                output.write(result.toString(2).getBytes(StandardCharsets.UTF_8));
            }
            status.setText(result.toString(2));
            Log.i(TAG, "Result saved: " + file + "; comparisonPass="
                    + result.optBoolean("comparisonPass", false));
        } catch (Exception error) {
            status.setText(Log.getStackTraceString(error));
            Log.e(TAG, "Could not save result", error);
        }
    }
}
