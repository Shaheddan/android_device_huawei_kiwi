package org.lineageos.settings.kiwi;

import android.app.AlertDialog;
import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.DateFormatSymbols;
import java.util.Arrays;
import java.util.Calendar;

/**
 * Asks the two things the phone cannot know about its battery: the month it was new, and
 * how often it was charged before the phone started counting cycles itself. Built in code
 * so it takes the system dialog theme (Material You) without extra resources.
 */
final class HealthHistoryDialog {

    /** kiwi (Honor 5X) launched in late 2015, so its battery cannot be older. */
    private static final int FIRST_YEAR = 2015;
    private static final int DEFAULT_YEAR = 2015;
    private static final int DEFAULT_MONTH = Calendar.NOVEMBER;

    private HealthHistoryDialog() {}

    static void show(Context c, Runnable onSaved) {
        final BatteryHealth.Estimate saved = BatteryHealth.estimate(c, BatteryHealth.stickyBattery(c));
        final Calendar now = Calendar.getInstance();
        final Calendar initial = Calendar.getInstance();
        if (saved.newSince > 0) {
            initial.setTimeInMillis(saved.newSince);
        } else {
            initial.set(DEFAULT_YEAR, DEFAULT_MONTH, 1);
        }

        final int pad = Math.round(KiwiStyle.dp(c, 24));
        final LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(pad, Math.round(KiwiStyle.dp(c, 8)), pad, 0);

        box.addView(text(c, R.string.history_when, 16, KiwiStyle.onSurface(c)));
        box.addView(text(c, R.string.history_when_hint, 14, KiwiStyle.onSurfaceVariant(c)));

        final LinearLayout pickers = new LinearLayout(c);
        pickers.setOrientation(LinearLayout.HORIZONTAL);
        pickers.setGravity(Gravity.CENTER);
        final NumberPicker month = new NumberPicker(c);
        month.setMinValue(0);
        month.setMaxValue(11);
        // getMonths() can carry a 13th (empty) month for lunar calendars; keep twelve.
        month.setDisplayedValues(Arrays.copyOf(DateFormatSymbols.getInstance().getMonths(), 12));
        month.setValue(initial.get(Calendar.MONTH));
        month.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        final NumberPicker year = new NumberPicker(c);
        year.setMinValue(FIRST_YEAR);
        year.setMaxValue(now.get(Calendar.YEAR));
        year.setValue(Math.max(FIRST_YEAR, Math.min(now.get(Calendar.YEAR),
                initial.get(Calendar.YEAR))));
        year.setWrapSelectorWheel(false);
        year.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        final LinearLayout.LayoutParams pickerLp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        pickers.addView(month, pickerLp);
        pickers.addView(year, new LinearLayout.LayoutParams(pickerLp));
        box.addView(pickers);

        final TextView usageTitle = text(c, R.string.history_usage, 16, KiwiStyle.onSurface(c));
        usageTitle.setPadding(0, Math.round(KiwiStyle.dp(c, 16)), 0, 0);
        box.addView(usageTitle);
        box.addView(text(c, R.string.history_usage_hint, 14, KiwiStyle.onSurfaceVariant(c)));

        final RadioGroup usage = new RadioGroup(c);
        usage.setOrientation(RadioGroup.VERTICAL);
        final int[] labels = { R.string.usage_daily, R.string.usage_often, R.string.usage_rarely };
        final int[] values = { BatteryHealth.USAGE_DAILY, BatteryHealth.USAGE_OFTEN,
                BatteryHealth.USAGE_RARELY };
        for (int i = 0; i < labels.length; i++) {
            final RadioButton b = new RadioButton(c);
            b.setId(View.generateViewId());
            b.setTag(values[i]);
            b.setText(labels[i]);
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            b.setMinHeight(Math.round(KiwiStyle.dp(c, 48)));
            usage.addView(b);
            if (saved.usage == values[i]) usage.check(b.getId());
        }
        box.addView(usage);

        final ScrollView scroll = new ScrollView(c);
        scroll.addView(box);

        final AlertDialog dialog = new AlertDialog.Builder(c)
                .setTitle(R.string.history_title)
                .setView(scroll)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.history_save, (d, which) -> {
                    final View checked = usage.findViewById(usage.getCheckedRadioButtonId());
                    if (checked == null) return;
                    final Calendar picked = Calendar.getInstance();
                    picked.clear();
                    picked.set(year.getValue(), month.getValue(), 1);
                    // A month in the future means "this month".
                    long when = picked.getTimeInMillis();
                    if (when > now.getTimeInMillis()) {
                        picked.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), 1);
                        when = picked.getTimeInMillis();
                    }
                    BatteryHealth.saveHistory(c, when, (Integer) checked.getTag());
                    if (onSaved != null) onSaved.run();
                })
                .create();
        dialog.show();

        // Save needs an answer to the usage question; nothing is picked for the user.
        final Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        save.setEnabled(usage.getCheckedRadioButtonId() != -1);
        usage.setOnCheckedChangeListener((g, id) -> save.setEnabled(id != -1));
    }

    private static TextView text(Context c, int res, int sp, int color) {
        final TextView t = new TextView(c);
        t.setText(res);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setPadding(0, Math.round(KiwiStyle.dp(c, 4)), 0, Math.round(KiwiStyle.dp(c, 4)));
        return t;
    }
}
