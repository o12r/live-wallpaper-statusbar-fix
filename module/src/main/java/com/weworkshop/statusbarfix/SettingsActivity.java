package com.weworkshop.statusbarfix;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Lets the user customize every color this module can apply, instead of the hard-coded
 * black/white (and ZTE's 65% black) this project started with. Values are written to a plain
 * {@link SharedPreferences} file that {@link Settings} re-reads (via Xposed's shared-preferences
 * bridge) from the launcher and System UI processes, so a change here takes effect the next time
 * the wallpaper's colors are (re-)evaluated — no reboot needed.
 */
public class SettingsActivity extends Activity {
    private SharedPreferences prefs;
    private RadioGroup homeModeGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(Settings.PREFS_NAME, MODE_PRIVATE);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);
        setContentView(scroll);

        addHeader(root, getString(R.string.section_home_status));
        homeModeGroup = new RadioGroup(this);
        homeModeGroup.setOrientation(LinearLayout.VERTICAL);
        addRadio(homeModeGroup, getString(R.string.mode_auto));
        addRadio(homeModeGroup, getString(R.string.mode_dark));
        addRadio(homeModeGroup, getString(R.string.mode_light));
        root.addView(homeModeGroup);
        int savedMode = prefs.getInt("home_status_mode", Settings.HOME_STATUS_AUTO);
        ((RadioButton) homeModeGroup.getChildAt(savedMode)).setChecked(true);

        addHeader(root, getString(R.string.section_lock_status));
        Button lockStatusDark = addColorRow(root, getString(R.string.color_dark),
                prefs.getInt("lock_status_dark", Settings.DEFAULT_DARK));
        Button lockStatusLight = addColorRow(root, getString(R.string.color_light),
                prefs.getInt("lock_status_light", Settings.DEFAULT_LIGHT));

        addHeader(root, getString(R.string.section_lock_content));
        Button lockContentDark = addColorRow(root, getString(R.string.color_dark),
                prefs.getInt("lock_content_dark", Settings.DEFAULT_LOCK_CONTENT_DARK));
        Button lockContentLight = addColorRow(root, getString(R.string.color_light),
                prefs.getInt("lock_content_light", Settings.DEFAULT_LIGHT));

        addHeader(root, getString(R.string.section_label));
        Button labelDark = addColorRow(root, getString(R.string.color_dark),
                prefs.getInt("label_dark", Settings.DEFAULT_DARK));
        Button labelLight = addColorRow(root, getString(R.string.color_light),
                prefs.getInt("label_light", Settings.DEFAULT_LIGHT));

        Button save = new Button(this);
        save.setText(R.string.save);
        LinearLayout.LayoutParams saveParams =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        saveParams.topMargin = dp(28);
        save.setLayoutParams(saveParams);
        save.setOnClickListener(v -> {
            int checkedId = homeModeGroup.getCheckedRadioButtonId();
            int mode = homeModeGroup.indexOfChild(findViewById(checkedId));
            prefs.edit()
                    .putInt("home_status_mode", mode)
                    .putInt("lock_status_dark", colorOf(lockStatusDark))
                    .putInt("lock_status_light", colorOf(lockStatusLight))
                    .putInt("lock_content_dark", colorOf(lockContentDark))
                    .putInt("lock_content_light", colorOf(lockContentLight))
                    .putInt("label_dark", colorOf(labelDark))
                    .putInt("label_light", colorOf(labelLight))
                    .apply();
            Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
        });
        root.addView(save);
    }

    private void addRadio(RadioGroup group, String label) {
        RadioButton rb = new RadioButton(this);
        rb.setText(label);
        rb.setId(android.view.View.generateViewId());
        group.addView(rb);
    }

    private void addHeader(LinearLayout root, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(18);
        tv.setTypeface(Typeface.create(tv.getTypeface(), Typeface.BOLD));
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(24);
        tv.setLayoutParams(lp);
        root.addView(tv);
    }

    private Button addColorRow(LinearLayout root, String label, int initialColor) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowLp =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.topMargin = dp(10);
        row.setLayoutParams(rowLp);

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(tv);

        Button swatch = new Button(this);
        swatch.setTag(initialColor);
        swatch.setText(hex(initialColor));
        swatch.setBackground(new ColorDrawable(initialColor));
        swatch.setTextColor(contrastColor(initialColor));
        swatch.setLayoutParams(new LinearLayout.LayoutParams(dp(150), ViewGroup.LayoutParams.WRAP_CONTENT));
        swatch.setOnClickListener(v -> showColorPicker(swatch));
        row.addView(swatch);

        root.addView(row);
        return swatch;
    }

    private void showColorPicker(Button swatch) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(hex((Integer) swatch.getTag()));
        input.setHint(R.string.hex_hint);
        int p = dp(20);
        input.setPadding(p, p, p, p);
        new AlertDialog.Builder(this)
                .setTitle(R.string.color_dialog_title)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    try {
                        int color = Color.parseColor(input.getText().toString().trim());
                        swatch.setTag(color);
                        swatch.setText(hex(color));
                        swatch.setBackground(new ColorDrawable(color));
                        swatch.setTextColor(contrastColor(color));
                    } catch (IllegalArgumentException e) {
                        Toast.makeText(this, R.string.invalid_color, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static int colorOf(Button swatch) {
        return (Integer) swatch.getTag();
    }

    private static String hex(int color) {
        return String.format("#%08X", color);
    }

    /** Black or white text on the swatch button, whichever reads better against its color. */
    private static int contrastColor(int color) {
        double luminance = 0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color);
        return luminance > 140 ? Color.BLACK : Color.WHITE;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
