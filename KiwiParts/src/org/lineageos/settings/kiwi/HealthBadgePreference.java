package org.lineageos.settings.kiwi;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

/**
 * Pixel's battery health status at the top of the page: a large circle and "Normal" (at or
 * above 80 % of the design capacity) or "Reduced" (below), with a line of advice when
 * Reduced. Before the battery history is set up it shows "Not set up".
 */
public class HealthBadgePreference extends Preference {

    private int mStatus = -1;
    private boolean mIntroDone;
    private View mBoundView;

    public HealthBadgePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.health_badge);
        setSelectable(false);
        setPersistent(false);
    }

    /** No opening animation (the page was recreated, e.g. on rotation, not opened). */
    public void skipIntro() {
        mIntroDone = true;
    }

    public void setStatus(int status) {
        if (status != mStatus) {
            mStatus = status;
            notifyChanged();
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        if (mStatus < 0) return;
        final Context c = getContext();
        final boolean sameView = holder.itemView == mBoundView;
        mBoundView = holder.itemView;

        final int circle;
        final int glyph;
        final int detail;
        switch (mStatus) {
            case BatteryHealth.STATUS_NORMAL:
                circle = KiwiStyle.healthGood(c);
                glyph = KiwiStyle.onHealthGood(c);
                detail = 0;
                break;
            case BatteryHealth.STATUS_REDUCED:
                circle = KiwiStyle.healthReduced(c);
                glyph = KiwiStyle.onHealthReduced(c);
                detail = R.string.health_status_reduced_detail;
                break;
            default:
                circle = KiwiStyle.secondaryContainer(c);
                glyph = KiwiStyle.onSecondaryContainer(c);
                detail = R.string.health_status_unknown_detail;
                break;
        }

        // The opening animation plays once, the first time the page shows a status.
        final HealthBadgeView badge = (HealthBadgeView) holder.findViewById(R.id.health_badge_view);
        badge.setStatus(mStatus, circle, glyph, !mIntroDone);
        mIntroDone = true;

        final String label = c.getString(BatteryHealth.statusLabel(mStatus));
        final TextView labelView = (TextView) holder.findViewById(R.id.health_badge_label);
        labelView.setTextColor(KiwiStyle.onSurface(c));
        KiwiMotion.crossfadeText(labelView, label, sameView);

        final TextView detailView = (TextView) holder.findViewById(R.id.health_badge_detail);
        detailView.setTextColor(KiwiStyle.onSurfaceVariant(c));
        final CharSequence detailText = detail == 0 ? "" : c.getString(detail);
        detailView.setText(detailText);
        detailView.setVisibility(TextUtils.isEmpty(detailText) ? View.GONE : View.VISIBLE);

        holder.itemView.setContentDescription(c.getString(R.string.health_badge_description,
                label) + (TextUtils.isEmpty(detailText) ? "" : ". " + detailText));
    }
}
