package org.lineageos.settings.kiwi;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

/**
 * Hero card at the top of Charging optimization: an animated battery, a state chip, the
 * level and what the charger and battery are doing right now. Display only.
 *
 * Android 15 motion: when the state changes the card colour blends into the new one, the
 * level counts to the new value and the texts cross-fade instead of jumping.
 */
public class BypassStatusPreference extends Preference {

    private BatteryAnimationView.State mState = BatteryAnimationView.State.DISCHARGING;
    private int mPercent = -1;
    private boolean mLow;
    private CharSequence mChip = "";
    private CharSequence mStatusTitle = "";
    private CharSequence mStatusSummary = "";

    // What the bound view shows now, to animate from on the next bind.
    private View mBoundView;
    private int mShownCard;
    private int mShownOnCard;
    private int mShownPercent = -1;
    private ValueAnimator mColorAnim;
    private ValueAnimator mCountAnim;

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
        if (state == mState && percent == mPercent && low == mLow
                && chip.toString().equals(mChip.toString())
                && title.toString().equals(mStatusTitle.toString())
                && summary.toString().equals(mStatusSummary.toString())) {
            return;   // battery broadcasts repeat often; only rebind on a real change
        }
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
        final View root = holder.itemView;
        // Animate only when this same view is updated in place; a fresh or recycled view
        // starts from the final state.
        final boolean animate = root == mBoundView && KiwiMotion.enabled();
        mBoundView = root;
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

        final View cardView = holder.findViewById(R.id.status_card);
        final TextView chip = (TextView) holder.findViewById(R.id.status_chip);
        final TextView level = (TextView) holder.findViewById(R.id.status_level);
        final TextView title = (TextView) holder.findViewById(R.id.status_title);
        final TextView summary = (TextView) holder.findViewById(R.id.status_summary);

        GradientDrawable card = cardView.getBackground() instanceof GradientDrawable
                ? (GradientDrawable) cardView.getBackground() : null;
        if (card == null) {
            card = KiwiStyle.rounded(c, KiwiStyle.CARD_RADIUS_DP + 4, cardColor);
            cardView.setBackground(card);
        }
        if (mColorAnim != null) mColorAnim.cancel();
        if (animate && cardColor != mShownCard) {
            // The card and its text colours blend into the new state together.
            final GradientDrawable target = card;
            final int fromCard = mShownCard;
            final int fromText = mShownOnCard;
            mColorAnim = ValueAnimator.ofFloat(0f, 1f);
            mColorAnim.setDuration(KiwiMotion.LONG);
            mColorAnim.setInterpolator(KiwiMotion.EMPHASIZED);
            mColorAnim.addUpdateListener(a -> {
                final float f = (float) a.getAnimatedValue();
                target.setColor(KiwiMotion.blend(fromCard, cardColor, f));
                final int text = KiwiMotion.blend(fromText, onCard, f);
                level.setTextColor(text);
                title.setTextColor(text);
                summary.setTextColor(soft(text));
            });
            mColorAnim.start();
        } else {
            card.setColor(cardColor);
            level.setTextColor(onCard);
            title.setTextColor(onCard);
            summary.setTextColor(soft(onCard));
        }
        mShownCard = cardColor;
        mShownOnCard = onCard;

        final BatteryAnimationView battery =
                (BatteryAnimationView) holder.findViewById(R.id.battery_view);
        battery.setColors(track, fill, wave, glyph, KiwiStyle.primary(c));
        battery.setBattery(mState, mPercent);

        final GradientDrawable chipBg = new GradientDrawable();
        chipBg.setCornerRadius(KiwiStyle.dp(c, 100));
        chipBg.setColor(fill);
        chip.setBackground(chipBg);
        chip.setTextColor(glyph);
        KiwiMotion.crossfadeText(chip, mChip, animate);
        chip.setVisibility(mChip.length() > 0 ? View.VISIBLE : View.GONE);

        bindLevel(level, animate);
        KiwiMotion.crossfadeText(title, mStatusTitle, animate);
        KiwiMotion.crossfadeText(summary, mStatusSummary, animate);

        // One spoken summary for the whole card.
        holder.itemView.setContentDescription(mChip + ". "
                + (mPercent >= 0 ? c.getString(R.string.bypass_level, mPercent) : "") + ". "
                + mStatusTitle + ". " + mStatusSummary);
    }

    /** Secondary text on the card: the card's text colour at 80 %. */
    private static int soft(int color) {
        return (color & 0x00FFFFFF) | 0xCC000000;
    }

    /** The level counts to its new value, like a meter, rather than jumping. */
    private void bindLevel(TextView level, boolean animate) {
        final Context c = getContext();
        if (mCountAnim != null) mCountAnim.cancel();
        if (mPercent < 0) {
            level.setText("");
        } else if (animate && mShownPercent >= 0 && mShownPercent != mPercent) {
            mCountAnim = ValueAnimator.ofInt(mShownPercent, mPercent);
            mCountAnim.setDuration(Math.min(900, 120 + 40L * Math.abs(mPercent - mShownPercent)));
            mCountAnim.setInterpolator(KiwiMotion.EMPHASIZED_DECELERATE);
            mCountAnim.addUpdateListener(a -> level.setText(
                    c.getString(R.string.bypass_level, (Integer) a.getAnimatedValue())));
            mCountAnim.start();
        } else {
            level.setText(c.getString(R.string.bypass_level, mPercent));
        }
        mShownPercent = mPercent;
    }
}
