package org.lineageos.settings.kiwi;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Bar under the capacity number: the full track is the battery when new, the filled part is
 * what it holds now, and a notch marks the 80 % line where the status turns to Reduced.
 * The fill grows in when the card opens.
 */
public class CapacityBarView extends View {

    private final Paint mTrack = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mNotch = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mRect = new RectF();

    private float mValue;      // 0..1 drawn
    private float mTarget;     // 0..1 wanted
    private final float mThreshold = BatteryHealth.NORMAL_MIN_PCT / 100f;
    private ValueAnimator mAnim;

    public CapacityBarView(Context context) {
        this(context, null);
    }

    public CapacityBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mNotch.setStyle(Paint.Style.STROKE);
        mNotch.setStrokeWidth(KiwiStyle.dp(context, 3));
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    /** @param notch the card colour, so the notch reads as a cut in the bar */
    public void setColors(int track, int fill, int notch) {
        mTrack.setColor(track);
        mFill.setColor(fill);
        mNotch.setColor(notch);
        invalidate();
    }

    public void setValue(float value) {
        mTarget = Math.max(0f, Math.min(1f, value));
        if (mAnim == null || !mAnim.isRunning()) mValue = mTarget;
        invalidate();
    }

    /** Grows the fill from empty to the value (when the capacity card opens). */
    public void playFill(long delay) {
        if (!KiwiMotion.enabled()) {
            mValue = mTarget;
            invalidate();
            return;
        }
        if (mAnim != null) mAnim.cancel();
        mAnim = ValueAnimator.ofFloat(0f, mTarget);
        mAnim.setStartDelay(delay);
        mAnim.setDuration(900);
        mAnim.setInterpolator(KiwiMotion.EMPHASIZED_DECELERATE);
        mAnim.addUpdateListener(a -> {
            mValue = (float) a.getAnimatedValue();
            invalidate();
        });
        mValue = 0f;
        mAnim.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAnim != null) mAnim.cancel();
        mValue = mTarget;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final float w = getWidth() - getPaddingLeft() - getPaddingRight();
        final float h = getHeight() - getPaddingTop() - getPaddingBottom();
        final float left = getPaddingLeft();
        final float top = getPaddingTop();
        final float radius = h / 2f;

        mRect.set(left, top, left + w, top + h);
        canvas.drawRoundRect(mRect, radius, radius, mTrack);
        // Right-to-left languages fill from the right.
        final boolean rtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
        if (mValue > 0f) {
            // Never narrower than its own rounded ends.
            final float fill = Math.max(h, w * mValue);
            if (rtl) {
                mRect.set(left + w - fill, top, left + w, top + h);
            } else {
                mRect.set(left, top, left + fill, top + h);
            }
            canvas.drawRoundRect(mRect, radius, radius, mFill);
        }
        final float x = left + w * (rtl ? 1f - mThreshold : mThreshold);
        canvas.drawLine(x, top - 1, x, top + h + 1, mNotch);
    }
}
