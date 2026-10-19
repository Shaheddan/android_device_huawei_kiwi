package org.lineageos.settings.kiwi;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

/**
 * Android 14 Material You hero card at the top of the bypass charging page: an animated
 * battery, a state chip, the level and what the charger and battery are doing right now.
 * Display only; it changes no setting.
 */
public class BypassStatusPreference extends Preference {

    private BatteryAnimationView.State mState = BatteryAnimationView.State.DISCHARGING;
    private int mPercent = -1;
    private boolean mLow;
    private CharSequence mChip = "";
    private CharSequence mStatusTitle = "";
    private CharSequence mStatusSummary = "";

    public BypassStatusPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.bypass_status_card);
        setSelectable(false);
        setPersistent(false);
    }

    /**
     * @param state   drives the battery animation and the card colour
     * @param percent battery level 0..100, or -1 when not known yet
     * @param low     draw the charge in the error colour (low battery, on battery)
     */
    public void setStatus(BatteryAnimationView.State state, int percent, boolean low,
            CharSequence chip, CharSequence title, CharSequence summary) {
        mState = state;
        mPercent = percent;
        mLow = low;
        mChip = chip;
        mStatusTitle = title;
        mStatusSummary = summary;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        final Context c = getContext();
        final boolean active = mState == BatteryAnimationView.State.BYPASS;

        // Primary container while bypass holds the battery, secondary container otherwise.
        final int cardColor = active ? KiwiStyle.primaryContainer(c)
                : KiwiStyle.secondaryContainer(c);
        final int onCard = active ? KiwiStyle.onPrimaryContainer(c)
                : KiwiStyle.onSecondaryContainer(c);
        final int track = active ? KiwiStyle.primaryContainerHigh(c)
                : KiwiStyle.secondaryContainerHigh(c);
        final int fill = mLow ? KiwiStyle.error(c) : KiwiStyle.primary(c);
        final int glyph = mLow ? KiwiStyle.onError(c) : KiwiStyle.onPrimary(c);
        // The back wave is the fill colour at half strength over the empty track.
        final int wave = (fill & 0x00FFFFFF) | 0x80000000;

        final GradientDrawable card = new GradientDrawable();
        card.setCornerRadius(KiwiStyle.dp(c, 32));
        card.setColor(cardColor);
        holder.findViewById(R.id.status_card).setBackground(card);

        final BatteryAnimationView battery =
                (BatteryAnimationView) holder.findViewById(R.id.battery_view);
        battery.setColors(track, fill, wave, glyph, KiwiStyle.primary(c));
        battery.setBattery(mState, mPercent);

        final TextView chip = (TextView) holder.findViewById(R.id.status_chip);
        final GradientDrawable chipBg = new GradientDrawable();
        chipBg.setCornerRadius(KiwiStyle.dp(c, 100));
        chipBg.setColor(fill);
        chip.setBackground(chipBg);
        chip.setTextColor(glyph);
        chip.setText(mChip);
        chip.setVisibility(mChip.length() > 0 ? View.VISIBLE : View.GONE);

        final TextView level = (TextView) holder.findViewById(R.id.status_level);
        level.setText(mPercent >= 0 ? c.getString(R.string.bypass_level, mPercent) : "");
        level.setTextColor(onCard);

        final TextView title = (TextView) holder.findViewById(R.id.status_title);
        title.setText(mStatusTitle);
        title.setTextColor(onCard);

        final TextView summary = (TextView) holder.findViewById(R.id.status_summary);
        summary.setText(mStatusSummary);
        summary.setTextColor(onCard);
        summary.setAlpha(0.8f);

        // One spoken summary for the whole card.
        holder.itemView.setContentDescription(mChip + ". " + level.getText() + ". "
                + mStatusTitle + ". " + mStatusSummary);
    }
}
