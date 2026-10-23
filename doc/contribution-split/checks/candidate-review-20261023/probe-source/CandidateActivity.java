/* SPDX-License-Identifier: GPL-3.0-or-later */
package org.x2m7.trime.readiness.candidate;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Loads the installed, unmodified production view; does not start Trime or Rime. */
public final class CandidateActivity extends Activity {
    TextView actual;
    TextView reference;
    TextView title;
    String sourceApk;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            Context production = createPackageContext("com.osfans.trime.debug",
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
            sourceApk = production.getApplicationInfo().sourceDir;
            Class<?> type = production.getClassLoader().loadClass("com.osfans.trime.ime.core.AutoScaleTextView");
            actual = (TextView) type.getConstructor(Context.class).newInstance(this);
            Class<?> mode = production.getClassLoader().loadClass(type.getName() + "$Mode");
            Object proportional = null;
            for (Object value : mode.getEnumConstants()) if (value.toString().equals("Proportional")) proportional = value;
            if (proportional == null) throw new IllegalStateException("Production scale mode absent");
            type.getMethod("setScaleMode", mode).invoke(actual, proportional);
            reference = new TextView(this);
            title = new TextView(this);
            title.setText("Actual production AutoScale (left), framework TextView (right)");
            LinearLayout root = new LinearLayout(this);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setBackgroundColor(Color.WHITE);
            root.setPadding(20, 20, 20, 20);
            root.addView(title);
            LinearLayout row = new LinearLayout(this);
            for (TextView label : new TextView[] {actual, reference}) {
                label.setTextSize(22);
                label.setTextColor(Color.BLACK);
                label.setSingleLine(true);
                label.setGravity(Gravity.CENTER);
                row.addView(label, new LinearLayout.LayoutParams(120, 80));
            }
            root.addView(row);
            setContentView(root);
        } catch (Exception error) {
            throw new IllegalStateException("Could not construct installed production view", error);
        }
    }

    void bind(String text, String stage) {
        title.setText(stage + ": production left / framework right");
        actual.setText(text);
        reference.setText(text);
    }
}
