package org.lineageos.settings.kiwi;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreference;

/**
 * "Use bypass charging": the Android 15 main switch bar, a large pill that turns from a
 * neutral surface to the primary container when switched on, blending between the two.
 *
 * Still the stock SettingsLib switch row underneath (same switch widget as the rest of
 * Settings); only the row's shape, colours and title size change. Not persistent: the page
 * maps it onto the bypass_now / bypass_auto keys that BypassService reads.
 */
public class BypassMainSwitchPreference extends SwitchPreference {

    private View mBoundView;
    private GradientDrawable mShape;
    private int mShownColor;
    private ValueAnimator mAnim;

    public BypassMainSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPersistent(false);
        setIconSpaceReserved(false);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        final Context c = getContext();
        final View row = holder.itemView;
        final boolean animate = row == mBoundView && KiwiMotion.enabled();
        mBoundView = row;

        final boolean on = isChecked();
        final int color = on ? KiwiStyle.primaryContainer(c) : KiwiStyle.switchBarOff(c);
        final int text = on ? KiwiStyle.onPrimaryContainer(c) : KiwiStyle.onSurface(c);

        if (!animate || mShape == null) {
            if (mAnim != null) mAnim.cancel();
            final float r = KiwiStyle.dp(c, KiwiStyle.CARD_RADIUS_DP);
            mShape = new GradientDrawable();
            mShape.setCornerRadius(r);
            mShape.setColor(color);
            final int side = Math.round(KiwiStyle.dp(c, 16));
            final int vertical = Math.round(KiwiStyle.dp(c, 8));
            row.setBackground(new InsetDrawable(
                    KiwiStyle.ripple(c, mShape, KiwiStyle.radii(r, r)),
                    side, vertical, side, vertical));
            // 16dp outside the pill + 24dp inside it; taller than a normal row.
            row.setPaddingRelative(Math.round(KiwiStyle.dp(c, 40)), Math.round(KiwiStyle.dp(c, 20)),
                    Math.round(KiwiStyle.dp(c, 36)), Math.round(KiwiStyle.dp(c, 20)));
        } else if (color != mShownColor) {
            if (mAnim != null) mAnim.cancel();
            final GradientDrawable shape = mShape;
            final int from = mShownColor;
            mAnim = ValueAnimator.ofFloat(0f, 1f);
            mAnim.setDuration(KiwiMotion.MEDIUM);
            mAnim.setInterpolator(KiwiMotion.EMPHASIZED);
            mAnim.addUpdateListener(a ->
                    shape.setColor(KiwiMotion.blend(from, color, (float) a.getAnimatedValue())));
            mAnim.start();
        }
        mShownColor = color;

        final View title = holder.findViewById(android.R.id.title);
        if (title instanceof TextView) {
            final TextView t = (TextView) title;
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            t.setTextColor(text);
            t.setMaxLines(2);
        }
    }
}
