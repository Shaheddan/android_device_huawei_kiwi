package org.lineageos.settings.kiwi;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.TypedValue;

/**
 * Material 3 colour roles and shapes for the KiwiParts pages, taken from the device's
 * Material You (Monet) palette so the pages follow the wallpaper colours and dark theme
 * like the rest of Android 15 Settings.
 *
 * The system palette maps _0/_10/_50/_100.._900/_1000 to M3 tones
 * 100/99/95/90..10/0, so e.g. accent1_600 is primary (tone 40) in light theme.
 */
final class KiwiStyle {

    /** Where a row sits in a group of rows that share one rounded card. */
    static final int GROUP_SINGLE = 0;
    static final int GROUP_TOP = 1;
    static final int GROUP_MIDDLE = 2;
    static final int GROUP_BOTTOM = 3;

    /** Large rounded cards (battery health cards, status card, main switch). */
    static final float CARD_RADIUS_DP = 28;

    private KiwiStyle() {}

    static boolean isNight(Context c) {
        return (c.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    private static int pick(Context c, int lightRes, int darkRes) {
        return c.getColor(isNight(c) ? darkRes : lightRes);
    }

    // ---- M3 roles -------------------------------------------------------------------

    static int primary(Context c) {
        return pick(c, android.R.color.system_accent1_600, android.R.color.system_accent1_200);
    }

    static int onPrimary(Context c) {
        return pick(c, android.R.color.system_accent1_0, android.R.color.system_accent1_800);
    }

    static int primaryContainer(Context c) {
        return pick(c, android.R.color.system_accent1_100, android.R.color.system_accent1_700);
    }

    static int onPrimaryContainer(Context c) {
        return pick(c, android.R.color.system_accent1_900, android.R.color.system_accent1_100);
    }

    /** One tone deeper than the primary container: the empty part of the battery. */
    static int primaryContainerHigh(Context c) {
        return pick(c, android.R.color.system_accent1_200, android.R.color.system_accent1_800);
    }

    static int secondaryContainer(Context c) {
        return pick(c, android.R.color.system_accent2_100, android.R.color.system_accent2_700);
    }

    static int onSecondaryContainer(Context c) {
        return pick(c, android.R.color.system_accent2_900, android.R.color.system_accent2_100);
    }

    static int secondaryContainerHigh(Context c) {
        return pick(c, android.R.color.system_accent2_200, android.R.color.system_accent2_800);
    }

    /** Cards and grouped rows on the page (M3 surface container high). */
    static int surfaceContainer(Context c) {
        return pick(c, android.R.color.system_neutral2_100, android.R.color.system_neutral2_800);
    }

    /** One step above the cards: chevron circles, empty bar tracks, the off main switch. */
    static int surfaceContainerHighest(Context c) {
        return pick(c, android.R.color.system_neutral2_200, android.R.color.system_neutral2_700);
    }

    /**
     * The main switch bar while off: in light theme the card tone (a tone darker looks
     * heavy next to the white page), in dark theme one step above the cards.
     */
    static int switchBarOff(Context c) {
        return pick(c, android.R.color.system_neutral2_100, android.R.color.system_neutral2_700);
    }

    static int onSurface(Context c) {
        return pick(c, android.R.color.system_neutral1_900, android.R.color.system_neutral1_100);
    }

    static int onSurfaceVariant(Context c) {
        return pick(c, android.R.color.system_neutral2_700, android.R.color.system_neutral2_200);
    }

    static int outline(Context c) {
        return pick(c, android.R.color.system_neutral2_500, android.R.color.system_neutral2_400);
    }

    /** M3 baseline error; the Monet palette has no error ramp. */
    static int error(Context c) {
        return isNight(c) ? 0xFFF2B8B5 : 0xFFB3261E;
    }

    static int onError(Context c) {
        return isNight(c) ? 0xFF601410 : 0xFFFFFFFF;
    }

    // ---- battery health status colours ----------------------------------------------
    // Fixed colours like Pixel's badge: health must read as "good" or "worn" whatever the
    // wallpaper palette is, so these do not follow Monet. Same tones in light and dark.

    /** Normal: the green of Pixel's badge, with a dark check on it. */
    static int healthGood(Context c) {
        return 0xFF81C995;
    }

    static int onHealthGood(Context c) {
        return 0xFF0B2E16;
    }

    /** Reduced: amber, a warning rather than an error. */
    static int healthReduced(Context c) {
        return 0xFFFFB95C;
    }

    static int onHealthReduced(Context c) {
        return 0xFF432B00;
    }

    // ---- shapes ---------------------------------------------------------------------

    static float dp(Context c, float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                c.getResources().getDisplayMetrics());
    }

    static GradientDrawable rounded(Context c, float radiusDp, int color) {
        final GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(c, radiusDp));
        shape.setColor(color);
        return shape;
    }

    /** A ripple in the M3 state-layer strength, clipped to the given shape. */
    static Drawable ripple(Context c, Drawable content, float[] radii) {
        final GradientDrawable mask = new GradientDrawable();
        mask.setCornerRadii(radii);
        mask.setColor(0xFFFFFFFF);
        final int ripple = (onSurface(c) & 0x00FFFFFF) | 0x1F000000;
        return new RippleDrawable(ColorStateList.valueOf(ripple), content, mask);
    }

    static float[] radii(float top, float bottom) {
        return new float[] { top, top, top, top, bottom, bottom, bottom, bottom };
    }

    /**
     * Grouped row: a rounded card with large outer corners, small inner corners where rows
     * meet, a 2dp gap between rows and a ripple clipped to the card.
     */
    static Drawable groupedRowBackground(Context c, int position, boolean clickable) {
        final float big = dp(c, 24);
        final float small = dp(c, 6);
        final float top = position == GROUP_SINGLE || position == GROUP_TOP ? big : small;
        final float bottom = position == GROUP_SINGLE || position == GROUP_BOTTOM ? big : small;
        final float[] radii = radii(top, bottom);

        final GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadii(radii);
        shape.setColor(surfaceContainer(c));

        final Drawable content = clickable ? ripple(c, shape, radii) : shape;
        final int side = Math.round(dp(c, 16));
        final int gap = Math.round(dp(c, 1));
        return new InsetDrawable(content, side, gap, side, gap);
    }

    /** Tonal circle behind a row icon (primary container, so it stands out from the row). */
    static Drawable iconCircle(Context c) {
        final GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(primaryContainer(c));
        return circle;
    }
}
