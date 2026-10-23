package org.lineageos.settings.kiwi;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.TextView;

/**
 * Material 3 motion for the KiwiParts pages: the M3 easing curves and durations, plus small
 * helpers. Everything checks {@link #enabled()} so the system "Remove animations" setting
 * turns every animation into an instant change.
 */
final class KiwiMotion {

    /** M3 emphasized: most transitions (expand, collapse, state changes). */
    static final Interpolator EMPHASIZED = new PathInterpolator(0.2f, 0f, 0f, 1f);
    /** M3 emphasized decelerate: things entering the screen. */
    static final Interpolator EMPHASIZED_DECELERATE =
            new PathInterpolator(0.05f, 0.7f, 0.1f, 1f);
    /** M3 emphasized accelerate: things leaving the screen. */
    static final Interpolator EMPHASIZED_ACCELERATE =
            new PathInterpolator(0.3f, 0f, 0.8f, 0.15f);

    static final long SHORT = 150;
    static final long MEDIUM = 350;
    static final long LONG = 500;

    private static final ArgbEvaluator ARGB = new ArgbEvaluator();

    private KiwiMotion() {}

    /** False when the user turned animations off (animator duration scale 0). */
    static boolean enabled() {
        return ValueAnimator.areAnimatorsEnabled();
    }

    static int blend(int from, int to, float fraction) {
        return (Integer) ARGB.evaluate(fraction, from, to);
    }

    /**
     * Swaps a text with a short fade out and in, so a new status reads as a change rather
     * than a jump. Instant when the text is the same or animations are off.
     */
    static void crossfadeText(TextView view, CharSequence text, boolean animate) {
        final CharSequence shown = view.getText();
        final boolean same = TextUtils.equals(shown == null ? null : shown.toString(),
                text == null ? null : text.toString());
        // Cancelling also drops the end action of a fade still in flight, so a text that
        // was requested and then taken back never lands late.
        view.animate().cancel();
        if (same || !animate || !enabled() || TextUtils.isEmpty(shown)) {
            view.setText(text);
            view.setAlpha(1f);
            return;
        }
        view.animate().alpha(0f).setStartDelay(0).setDuration(SHORT / 2 + 30)
                .setInterpolator(EMPHASIZED_ACCELERATE)
                .withEndAction(() -> {
                    view.setText(text);
                    view.animate().alpha(1f).setStartDelay(0).setDuration(SHORT + 50)
                            .setInterpolator(EMPHASIZED_DECELERATE).start();
                }).start();
    }

    /** Fades a view in from slightly below: used for content that appears inside a card. */
    static void rise(View view, long delay, float distancePx) {
        view.setAlpha(0f);
        view.setTranslationY(distancePx);
        view.animate().alpha(1f).translationY(0f).setStartDelay(delay)
                .setDuration(LONG).setInterpolator(EMPHASIZED_DECELERATE).start();
    }
}
