package org.lineageos.settings.kiwi;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

/**
 * The status circle at the top of Battery health: green with a check (Normal), amber with
 * "!" (Reduced) or neutral with "?" (not set up yet).
 *
 * When the page opens the circle pops in with a slight overshoot, the check draws itself
 * stroke by stroke and a soft ring spreads out once. A later status change blends the circle
 * colour and redraws the glyph.
 */
public class HealthBadgeView extends View {

    private static final long INTRO_MS = 950;
    private static final OvershootInterpolator POP = new OvershootInterpolator(1.6f);

    private final Paint mCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mGlyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path mCheck = new Path();
    private final Path mCheckPart = new Path();
    private final PathMeasure mMeasure = new PathMeasure();

    private int mStatus = -1;
    private int mCircleColor;
    private int mGlyphColor;

    // Animation state: 1 = finished.
    private float mPop = 1f;         // circle scale progress
    private float mFade = 1f;        // circle opacity
    private float mGlyph = 1f;       // how much of the glyph is drawn
    private float mRing = 1f;        // spreading ring (1 = gone)

    private boolean mIntroPending;
    private ValueAnimator mAnim;

    public HealthBadgeView(Context context) {
        this(context, null);
    }

    public HealthBadgeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mCirclePaint.setStyle(Paint.Style.FILL);
        mRingPaint.setStyle(Paint.Style.STROKE);
        mGlyphPaint.setStyle(Paint.Style.STROKE);
        mGlyphPaint.setStrokeCap(Paint.Cap.ROUND);
        mGlyphPaint.setStrokeJoin(Paint.Join.ROUND);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    /**
     * @param intro play the opening animation (once per page) instead of a status change
     */
    public void setStatus(int status, int circle, int glyph, boolean intro) {
        final boolean changed = mStatus != -1 && status != mStatus;
        final int fromCircle = mCircleColor;
        mStatus = status;
        mCircleColor = circle;
        mGlyphColor = glyph;

        if (!KiwiMotion.enabled()) {
            finish();
            return;
        }
        if (intro) {
            mIntroPending = true;
            mPop = 0f;
            mFade = 0f;
            mGlyph = 0f;
            mRing = 0f;
            if (isLaidOut() && isAttachedToWindow()) startIntro();
            invalidate();
        } else if (changed && isAttachedToWindow()) {
            startChange(fromCircle);
        } else if (mAnim == null || !mAnim.isRunning()) {
            invalidate();
        }
    }

    private void finish() {
        if (mAnim != null) mAnim.cancel();
        mIntroPending = false;
        mPop = mFade = mGlyph = mRing = 1f;
        invalidate();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (mIntroPending && isAttachedToWindow()) post(this::startIntro);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // Off screen there is nothing to animate: jump to the end state.
        if (mAnim != null) mAnim.cancel();
        mIntroPending = false;
        mPop = mFade = mGlyph = mRing = 1f;
    }

    private void startIntro() {
        if (!mIntroPending) return;
        mIntroPending = false;
        if (mAnim != null) mAnim.cancel();
        mAnim = ValueAnimator.ofFloat(0f, 1f);
        mAnim.setDuration(INTRO_MS);
        mAnim.setInterpolator(new LinearInterpolator());
        mAnim.addUpdateListener(a -> {
            final float t = a.getAnimatedFraction() * INTRO_MS;
            mPop = POP.getInterpolation(clamp(t / 450f));
            mFade = clamp(t / 200f);
            mGlyph = KiwiMotion.EMPHASIZED_DECELERATE.getInterpolation(clamp((t - 300f) / 400f));
            mRing = KiwiMotion.EMPHASIZED_DECELERATE.getInterpolation(clamp((t - 250f) / 700f));
            invalidate();
        });
        mAnim.start();
    }

    private void startChange(int fromCircle) {
        if (mAnim != null) mAnim.cancel();
        final int toCircle = mCircleColor;
        mPop = mFade = 1f;
        mRing = 1f;
        mAnim = ValueAnimator.ofFloat(0f, 1f);
        mAnim.setDuration(KiwiMotion.LONG + 200);
        mAnim.setInterpolator(new LinearInterpolator());
        mAnim.addUpdateListener(a -> {
            final float f = a.getAnimatedFraction();
            mCircleColor = KiwiMotion.blend(fromCircle, toCircle,
                    KiwiMotion.EMPHASIZED.getInterpolation(clamp(f / 0.6f)));
            mGlyph = KiwiMotion.EMPHASIZED_DECELERATE.getInterpolation(clamp((f - 0.3f) / 0.7f));
            invalidate();
        });
        mAnim.start();
    }

    private static float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        final float r = radius();
        final float cx = w / 2f;
        final float cy = h / 2f;
        // A check drawn as one stroke: short arm down, long arm up.
        mCheck.reset();
        mCheck.moveTo(cx - 0.36f * r, cy + 0.02f * r);
        mCheck.lineTo(cx - 0.10f * r, cy + 0.28f * r);
        mCheck.lineTo(cx + 0.38f * r, cy - 0.22f * r);
        mGlyphPaint.setStrokeWidth(0.14f * r);
        mRingPaint.setStrokeWidth(KiwiStyle.dp(getContext(), 3));
        mTextPaint.setTextSize(1.05f * r);
    }

    /** The circle leaves room around it for the spreading ring. */
    private float radius() {
        return Math.min(getWidth(), getHeight()) / 2f / 1.5f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mStatus < 0) return;
        final float cx = getWidth() / 2f;
        final float cy = getHeight() / 2f;
        final float r = radius();

        // One soft ring spreading out from the circle (opening animation only).
        if (mRing < 1f) {
            mRingPaint.setColor(mCircleColor);
            mRingPaint.setAlpha(Math.round(110 * (1f - mRing)));
            canvas.drawCircle(cx, cy, r * (1f + 0.5f * mRing), mRingPaint);
        }

        final float scale = 0.4f + 0.6f * mPop;
        mCirclePaint.setColor(mCircleColor);
        mCirclePaint.setAlpha(Math.round(255 * mFade));
        canvas.drawCircle(cx, cy, r * scale, mCirclePaint);
        if (mGlyph <= 0f) return;

        canvas.save();
        canvas.scale(scale, scale, cx, cy);
        switch (mStatus) {
            case BatteryHealth.STATUS_NORMAL:
                mGlyphPaint.setColor(mGlyphColor);
                mMeasure.setPath(mCheck, false);
                mCheckPart.reset();
                mMeasure.getSegment(0, mMeasure.getLength() * mGlyph, mCheckPart, true);
                canvas.drawPath(mCheckPart, mGlyphPaint);
                break;
            case BatteryHealth.STATUS_REDUCED: {
                // "!": the bar grows down, then the dot appears.
                mGlyphPaint.setColor(mGlyphColor);
                final float top = cy - 0.42f * r;
                final float bottom = cy + 0.10f * r;
                canvas.drawLine(cx, top, cx, top + (bottom - top) * Math.min(1f, mGlyph / 0.7f),
                        mGlyphPaint);
                if (mGlyph > 0.7f) {
                    mCirclePaint.setColor(mGlyphColor);
                    mCirclePaint.setAlpha(Math.round(255 * (mGlyph - 0.7f) / 0.3f));
                    canvas.drawCircle(cx, cy + 0.36f * r, 0.085f * r, mCirclePaint);
                }
                break;
            }
            default: {
                mTextPaint.setColor(mGlyphColor);
                mTextPaint.setAlpha(Math.round(255 * mGlyph));
                final Paint.FontMetrics fm = mTextPaint.getFontMetrics();
                canvas.drawText("?", cx, cy - (fm.ascent + fm.descent) / 2f, mTextPaint);
                break;
            }
        }
        canvas.restore();
    }
}
