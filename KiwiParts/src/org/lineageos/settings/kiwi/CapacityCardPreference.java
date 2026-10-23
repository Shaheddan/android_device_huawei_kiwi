package org.lineageos.settings.kiwi;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.format.DateFormat;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.text.NumberFormat;
import java.util.Date;

/**
 * "Battery capacity": the estimated share of its design capacity the battery still holds.
 *
 * Closed, it shows the percentage under the title like Pixel. Open, it shows the number
 * large, Pixel's explanation, a bar against the 80 % line, and what the estimate is built on
 * (charge cycles, battery age, mAh), with a button to set up or edit the battery history.
 * Before the history is set up it opens by itself and asks for it.
 */
public class CapacityCardPreference extends HealthCardPreference {

    private BatteryHealth.Estimate mEstimate;
    private Runnable mOnEditHistory;
    private boolean mAutoOpened;
    private boolean mReveal;
    private ValueAnimator mCount;

    public CapacityCardPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setIcon(R.drawable.ic_health_capacity);
        setTitle(R.string.capacity_title);
    }

    public void setOnEditHistory(Runnable onEditHistory) {
        mOnEditHistory = onEditHistory;
    }

    public void setEstimate(BatteryHealth.Estimate e) {
        // Just set up with the card open: play the reveal (count-up, bar) on the next bind.
        if (mEstimate != null && !mEstimate.setUp && e.setUp && isExpanded()) mReveal = true;
        mEstimate = e;
        setSummary(e.setUp ? getContext().getString(R.string.capacity_value, e.capacityPct)
                : getContext().getString(R.string.capacity_not_set));
        if (!e.setUp && !mAutoOpened) {
            // Nothing to show until the history is known: open on the set-up text once.
            mAutoOpened = true;
            setExpanded(true);
        }
        notifyChanged();
    }

    @Override
    protected boolean hideHeaderDetailsWhenExpanded() {
        return true;
    }

    @Override
    protected int getDetailsLayout() {
        return R.layout.health_capacity_details;
    }

    @Override
    protected void onBindDetails(ViewGroup d) {
        final Context c = getContext();
        final BatteryHealth.Estimate e = mEstimate;
        if (e == null) return;
        final int onSurface = KiwiStyle.onSurface(c);
        final int variant = KiwiStyle.onSurfaceVariant(c);

        final View setUp = d.findViewById(R.id.capacity_setup_group);
        final View result = d.findViewById(R.id.capacity_result_group);
        setUp.setVisibility(e.setUp ? View.GONE : View.VISIBLE);
        result.setVisibility(e.setUp ? View.VISIBLE : View.GONE);
        ((TextView) d.findViewById(R.id.capacity_setup_body)).setTextColor(variant);

        final Button button = (Button) d.findViewById(R.id.capacity_history_button);
        button.setText(e.setUp ? R.string.capacity_edit : R.string.capacity_setup);
        button.setTextColor(KiwiStyle.onSecondaryContainer(c));
        button.setBackground(KiwiStyle.ripple(c,
                KiwiStyle.rounded(c, 20, KiwiStyle.secondaryContainer(c)),
                KiwiStyle.radii(KiwiStyle.dp(c, 20), KiwiStyle.dp(c, 20))));
        button.setCompoundDrawableTintList(
                ColorStateList.valueOf(KiwiStyle.onSecondaryContainer(c)));
        button.setOnClickListener(v -> {
            if (mOnEditHistory != null) mOnEditHistory.run();
        });
        if (!e.setUp) return;

        final TextView label = (TextView) d.findViewById(R.id.capacity_label);
        label.setTextColor(variant);
        final TextView big = (TextView) d.findViewById(R.id.capacity_big);
        big.setTextColor(onSurface);
        if (mCount == null || !mCount.isRunning()) big.setText(bigPercent(e.capacityPct));

        final boolean reduced = e.status == BatteryHealth.STATUS_REDUCED;
        final CapacityBarView bar = (CapacityBarView) d.findViewById(R.id.capacity_bar);
        bar.setColors(KiwiStyle.surfaceContainerHighest(c),
                reduced ? KiwiStyle.healthReduced(c) : KiwiStyle.primary(c),
                KiwiStyle.surfaceContainer(c));
        bar.setValue(e.capacityPct / 100f);
        final TextView threshold = (TextView) d.findViewById(R.id.capacity_threshold);
        threshold.setText(c.getString(R.string.capacity_threshold, BatteryHealth.NORMAL_MIN_PCT));
        threshold.setTextColor(variant);

        ((TextView) d.findViewById(R.id.capacity_body)).setTextColor(onSurface);
        ((TextView) d.findViewById(R.id.capacity_decrease)).setTextColor(onSurface);

        // What the estimate is built on.
        final NumberFormat nf = NumberFormat.getIntegerInstance();
        final String cycles = e.earlierCycles > 0 && e.countedCycles < e.totalCycles
                ? c.getString(R.string.capacity_cycles_about, nf.format(e.totalCycles))
                : nf.format(e.totalCycles);
        final String cyclesDetail;
        if (e.countStart > 0 && e.countedCycles >= 0) {
            cyclesDetail = c.getString(R.string.capacity_cycles_counted,
                    nf.format(e.countedCycles),
                    DateFormat.getMediumDateFormat(c).format(new Date(e.countStart)));
        } else {
            cyclesDetail = c.getString(R.string.capacity_cycles_not_counted);
        }
        bindRow(d, R.id.capacity_row_cycles, R.string.capacity_cycles_label, cycles, cyclesDetail);
        bindRow(d, R.id.capacity_row_age, R.string.capacity_age_label, formatAge(c, e.ageYears),
                null);
        bindRow(d, R.id.capacity_row_mah, R.string.capacity_now_label,
                c.getString(R.string.capacity_now_value, nf.format(e.capacityMah),
                        nf.format(BatteryHealth.DESIGN_MAH)), null);

        final TextView how = (TextView) d.findViewById(R.id.capacity_how);
        how.setTextColor(variant);

        if (mReveal) {
            mReveal = false;
            d.post(() -> onDetailsShown(d));
        }
    }

    private void bindRow(View d, int rowId, int label, CharSequence value, CharSequence detail) {
        final Context c = getContext();
        final View row = d.findViewById(rowId);
        final TextView l = (TextView) row.findViewById(R.id.health_row_label);
        final TextView v = (TextView) row.findViewById(R.id.health_row_value);
        final TextView s = (TextView) row.findViewById(R.id.health_row_detail);
        l.setText(label);
        l.setTextColor(KiwiStyle.onSurfaceVariant(c));
        v.setText(value);
        v.setTextColor(KiwiStyle.onSurface(c));
        s.setText(detail);
        s.setTextColor(KiwiStyle.onSurfaceVariant(c));
        s.setVisibility(detail == null ? View.GONE : View.VISIBLE);
        row.setContentDescription(c.getString(label) + ": " + value
                + (detail == null ? "" : ". " + detail));
    }

    /** "87%" with a smaller percent sign, like Pixel. */
    private CharSequence bigPercent(int pct) {
        final String text = getContext().getString(R.string.capacity_value, pct);
        final SpannableString s = new SpannableString(text);
        final int sign = text.indexOf('%');
        if (sign >= 0) {
            s.setSpan(new RelativeSizeSpan(0.5f), sign, sign + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return s;
    }

    static String formatAge(Context c, double years) {
        final int months = (int) Math.floor(years * 12 + 1e-6);
        if (months < 1) return c.getString(R.string.age_less_than_month);
        final int y = months / 12;
        final int m = months % 12;
        final String ys = c.getResources().getQuantityString(R.plurals.age_years, y, y);
        final String ms = c.getResources().getQuantityString(R.plurals.age_months, m, m);
        if (y == 0) return ms;
        if (m == 0) return ys;
        return c.getString(R.string.age_years_months, ys, ms);
    }

    @Override
    protected void onDetailsShown(ViewGroup d) {
        final BatteryHealth.Estimate e = mEstimate;
        if (e == null || !e.setUp) return;
        // The bar grows in and the number counts up to the estimate as the card opens.
        ((CapacityBarView) d.findViewById(R.id.capacity_bar)).playFill(150);
        final TextView big = (TextView) d.findViewById(R.id.capacity_big);
        if (mCount != null) mCount.cancel();
        mCount = ValueAnimator.ofInt(Math.max(0, e.capacityPct - 25), e.capacityPct);
        mCount.setStartDelay(150);
        mCount.setDuration(900);
        mCount.setInterpolator(KiwiMotion.EMPHASIZED_DECELERATE);
        mCount.addUpdateListener(a -> big.setText(bigPercent((Integer) a.getAnimatedValue())));
        mCount.start();
    }
}
