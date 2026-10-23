package org.lineageos.settings.kiwi;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.OvershootInterpolator;

/**
 * The radio button of a Charging optimization mode row, drawn so its motion matches the
 * rest of the page: when selected, the ring takes the primary colour and the centre dot
 * grows with a slight overshoot; when deselected, the dot shrinks away.
 */
public class RadioDotView extends View {

    private final Paint mRing = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mDot = new Paint(Paint.ANTI_ALIAS_FLAG);

    private boolean mChecked;
    private float mProgress;   // 0 = empty ring, 1 = selected
    private int mOnColor;
    private int mOffColor;
    private ValueAnimator mAnim;

    public RadioDotView(Context context) {
        this(context, null);
    }

    public RadioDotView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mRing.setStyle(Paint.Style.STROKE);
        mRing.setStrokeWidth(KiwiStyle.dp(context, 2));
        mDot.setStyle(Paint.Style.FILL);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    public void setColors(int on, int off) {
        mOnColor = on;
        mOffColor = off;
        invalidate();
    }

    public void setChecked(boolean checked, boolean animate) {
        if (checked == mChecked && (mAnim == null || !mAnim.isRunning())) {
            mProgress = checked ? 1f : 0f;
            invalidate();
            return;
        }
        mChecked = checked;
        if (mAnim != null) mAnim.cancel();
        final float target = checked ? 1f : 0f;
        if (!animate || !KiwiMotion.enabled() || !isAttachedToWindow()) {
            mProgress = target;
            invalidate();
            return;
        }
        mAnim = ValueAnimator.ofFloat(mProgress, target);
        mAnim.setDuration(checked ? KiwiMotion.MEDIUM : KiwiMotion.SHORT);
        mAnim.setInterpolator(checked ? new OvershootInterpolator(2f)
                : KiwiMotion.EMPHASIZED_ACCELERATE);
        mAnim.addUpdateListener(a -> {
            mProgress = (float) a.getAnimatedValue();
            invalidate();
        });
        mAnim.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAnim != null) mAnim.end();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final float cx = getWidth() / 2f;
        final float cy = getHeight() / 2f;
        final float outer = Math.min(cx, cy) - mRing.getStrokeWidth();
        final float p = Math.max(0f, mProgress);
        mRing.setColor(KiwiMotion.blend(mOffColor, mOnColor, Math.min(1f, p)));
        canvas.drawCircle(cx, cy, outer, mRing);
        if (p > 0f) {
            mDot.setColor(mOnColor);
            canvas.drawCircle(cx, cy, outer * 0.5f * p, mDot);
        }
    }
}
