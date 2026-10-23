package org.lineageos.settings.kiwi;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * The battery on the Charging optimization status card.
 *
 *  - CHARGING:    the charge is liquid with a moving wave on top, bubbles rise through it and
 *                 the bolt breathes.
 *  - BYPASS:      the charge stands still behind a pause sign (the battery is held) while a
 *                 glowing comet of current circles AROUND the battery: the charger powers
 *                 the phone directly.
 *  - FULL:        a check, and a short burst of sparks the moment the battery becomes full.
 *  - DISCHARGING: still picture, no animation (no wasted frames on battery).
 *
 * Level changes glide instead of jumping. Animations stop when the view is off screen
 * and follow the system "remove animations" setting.
 */
public class BatteryAnimationView extends View {

    public enum State { DISCHARGING, CHARGING, BYPASS, FULL }

    private static final int BUBBLES = 6;
    private static final int COMET_STEPS = 14;
    private static final float COMET_LENGTH = 0.32f;   // part of the ring the comet covers
    private static final int SPARKS = 8;
    private static final long SPARK_MS = 900;

    private State mState = State.DISCHARGING;
    private float mLevel;          // 0..1, what is drawn
    private float mTargetLevel;    // 0..1, where it is going
    private float mPhase;          // 0..1, loop position of the wave / bubbles / comet
    private float mSpark = 1f;     // 0..1 burst progress, 1 = no burst

    private int mTrackColor, mFillColor, mWaveColor, mGlyphColor, mFlowColor;

    private final Paint mTrackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mWavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mGlyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mFlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF mBody = new RectF();
    private final RectF mCap = new RectF();
    private final RectF mRing = new RectF();
    private final RectF mBar = new RectF();
    private final Path mBodyPath = new Path();
    private final Path mWavePath = new Path();
    private final Path mRingPath = new Path();
    private final Path mSegment = new Path();
    private final Path mBolt = new Path();
    private final Path mCheck = new Path();
    private final PathMeasure mRingMeasure = new PathMeasure();
    private final float[] mPos = new float[2];
    private float mRingLength;

    private ValueAnimator mLoop;
    private ValueAnimator mLevelAnim;
    private ValueAnimator mSparkAnim;
    private boolean mVisible;

    public BatteryAnimationView(Context context) {
        this(context, null);
    }

    public BatteryAnimationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mFillPaint.setStyle(Paint.Style.FILL);
        mWavePaint.setStyle(Paint.Style.FILL);
        mTrackPaint.setStyle(Paint.Style.FILL);
        mGlyphPaint.setStyle(Paint.Style.FILL);
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setStrokeCap(Paint.Cap.ROUND);
        mStrokePaint.setStrokeJoin(Paint.Join.ROUND);
        mFlowPaint.setStyle(Paint.Style.STROKE);
        mFlowPaint.setStrokeCap(Paint.Cap.ROUND);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    /** Colours come from the card so the battery always matches it. */
    public void setColors(int track, int fill, int wave, int glyph, int flow) {
        mTrackColor = track;
        mFillColor = fill;
        mWaveColor = wave;
        mGlyphColor = glyph;
        mFlowColor = flow;
        invalidate();
    }

    /** @param percent 0..100, or negative when unknown (drawn empty). */
    public void setBattery(State state, int percent) {
        final float target = Math.max(0, Math.min(100, percent)) / 100f;
        final boolean stateChanged = state != mState;
        final boolean becameFull = stateChanged && state == State.FULL;
        mState = state;

        if (!isLaidOut() || !KiwiMotion.enabled()) {
            if (mLevelAnim != null) mLevelAnim.cancel();
            mLevel = mTargetLevel = target;
        } else if (target != mTargetLevel) {
            mTargetLevel = target;
            if (mLevelAnim != null) mLevelAnim.cancel();
            mLevelAnim = ValueAnimator.ofFloat(mLevel, target);
            mLevelAnim.setDuration(700);
            mLevelAnim.setInterpolator(KiwiMotion.EMPHASIZED_DECELERATE);
            mLevelAnim.addUpdateListener(a -> {
                mLevel = (float) a.getAnimatedValue();
                invalidate();
            });
            mLevelAnim.start();
        }
        if (becameFull && mVisible && isLaidOut()) playSparks();
        if (stateChanged) updateLoop();
        invalidate();
    }

    // ---- animation life cycle -------------------------------------------------------

    @Override
    public void onVisibilityAggregated(boolean isVisible) {
        super.onVisibilityAggregated(isVisible);
        mVisible = isVisible;
        updateLoop();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mVisible = false;
        updateLoop();
        if (mLevelAnim != null) mLevelAnim.cancel();
        if (mSparkAnim != null) mSparkAnim.cancel();
        mSpark = 1f;
    }

    private void updateLoop() {
        final boolean animated = mState == State.CHARGING || mState == State.BYPASS;
        final boolean run = animated && mVisible && isAttachedToWindow() && KiwiMotion.enabled();
        if (!run) {
            if (mLoop != null) {
                mLoop.cancel();
                mLoop = null;
            }
            return;
        }
        final long duration = mState == State.BYPASS ? 2200 : 2600;
        if (mLoop != null && mLoop.getDuration() == duration) return;
        if (mLoop != null) mLoop.cancel();
        mLoop = ValueAnimator.ofFloat(0f, 1f);
        mLoop.setDuration(duration);
        mLoop.setRepeatCount(ValueAnimator.INFINITE);
        mLoop.setInterpolator(new LinearInterpolator());
        mLoop.addUpdateListener(a -> {
            mPhase = (float) a.getAnimatedValue();
            invalidate();
        });
        mLoop.start();
    }

    private void playSparks() {
        if (!KiwiMotion.enabled()) return;
        if (mSparkAnim != null) mSparkAnim.cancel();
        mSparkAnim = ValueAnimator.ofFloat(0f, 1f);
        mSparkAnim.setDuration(SPARK_MS);
        mSparkAnim.setInterpolator(KiwiMotion.EMPHASIZED_DECELERATE);
        mSparkAnim.addUpdateListener(a -> {
            mSpark = (float) a.getAnimatedValue();
            invalidate();
        });
        mSparkAnim.start();
    }

    // ---- geometry -------------------------------------------------------------------

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        final Context c = getContext();
        final float pad = KiwiStyle.dp(c, 10);    // room for the current ring and sparks
        final float availW = w - getPaddingLeft() - getPaddingRight() - 2 * pad;
        final float availH = h - getPaddingTop() - getPaddingBottom() - 2 * pad;

        // Battery proportions: body 1 : 1.75, cap on top.
        final float bodyW = Math.min(availW, availH / 1.9f);
        final float bodyH = bodyW * 1.75f;
        final float capH = bodyW * 0.1f;
        final float cx = getPaddingLeft() + pad + availW / 2f;
        final float top = getPaddingTop() + pad + (availH - bodyH - capH) / 2f + capH;

        mBody.set(cx - bodyW / 2f, top, cx + bodyW / 2f, top + bodyH);
        final float capW = bodyW * 0.38f;
        mCap.set(cx - capW / 2f, top - capH, cx + capW / 2f, top + capH * 0.6f);

        final float radius = bodyW * 0.24f;
        mBodyPath.reset();
        mBodyPath.addRoundRect(mBody, radius, radius, Path.Direction.CW);

        // The current ring follows the body at a distance, cap included.
        final float gap = KiwiStyle.dp(c, 6);
        mRing.set(mBody.left - gap, mCap.top - gap, mBody.right + gap, mBody.bottom + gap);
        mRingPath.reset();
        mRingPath.addRoundRect(mRing, radius + gap, radius + gap, Path.Direction.CW);
        mRingMeasure.setPath(mRingPath, true);
        mRingLength = mRingMeasure.getLength();
        mFlowPaint.setStrokeWidth(KiwiStyle.dp(c, 3.5f));

        buildBolt(cx, mBody.centerY(), bodyW * 0.42f);
        buildCheck(cx, mBody.centerY(), bodyW * 0.42f);
        mStrokePaint.setStrokeWidth(bodyW * 0.075f);
    }

    /** Material "bolt" glyph, centred at (cx, cy), height h. */
    private void buildBolt(float cx, float cy, float h) {
        final float s = h / 18f;   // glyph drawn on an 11 x 18 grid
        final float x0 = cx - 5.5f * s;
        final float y0 = cy - 9f * s;
        mBolt.reset();
        mBolt.moveTo(x0 + 7f * s, y0);
        mBolt.lineTo(x0, y0 + 10.5f * s);
        mBolt.lineTo(x0 + 5f * s, y0 + 10.5f * s);
        mBolt.lineTo(x0 + 4f * s, y0 + 18f * s);
        mBolt.lineTo(x0 + 11f * s, y0 + 7.5f * s);
        mBolt.lineTo(x0 + 6f * s, y0 + 7.5f * s);
        mBolt.close();
    }

    private void buildCheck(float cx, float cy, float size) {
        final float s = size / 2f;
        mCheck.reset();
        mCheck.moveTo(cx - 0.62f * s, cy + 0.02f * s);
        mCheck.lineTo(cx - 0.18f * s, cy + 0.46f * s);
        mCheck.lineTo(cx + 0.66f * s, cy - 0.40f * s);
    }

    // ---- drawing --------------------------------------------------------------------

    @Override
    protected void onDraw(Canvas canvas) {
        if (mBody.isEmpty()) return;

        // Empty battery and its cap.
        mTrackPaint.setColor(mTrackColor);
        canvas.drawRoundRect(mCap, mCap.height() / 2f, mCap.height() / 2f, mTrackPaint);
        canvas.drawPath(mBodyPath, mTrackPaint);

        // Charge: clipped to the body so the wave, bubbles and corners stay inside it.
        canvas.save();
        canvas.clipPath(mBodyPath);
        final float fillTop = mBody.bottom - mBody.height() * mLevel;
        mFillPaint.setColor(mFillColor);
        if (mState == State.CHARGING && mLevel < 0.995f) {
            final float amp = KiwiStyle.dp(getContext(), 3.5f);
            // A softer second wave behind, half a cycle apart, gives depth.
            mWavePaint.setColor(mWaveColor);
            buildWave(fillTop, amp, mPhase + 0.5f);
            canvas.drawPath(mWavePath, mWavePaint);
            buildWave(fillTop, amp, mPhase);
            canvas.drawPath(mWavePath, mFillPaint);
            drawBubbles(canvas, fillTop);
        } else {
            canvas.drawRect(mBody.left, fillTop, mBody.right, mBody.bottom, mFillPaint);
        }
        canvas.restore();

        if (mState == State.DISCHARGING) return;
        // The glyph sits in the middle of the battery, partly over the charge and partly
        // over the empty part, so it is drawn twice: in the "on fill" colour where it covers
        // the charge and in the fill colour where it covers the empty part.
        drawGlyph(canvas, fillTop, true);
        drawGlyph(canvas, fillTop, false);
        if (mState == State.BYPASS) drawComet(canvas);
        if (mState == State.FULL && mSpark < 1f) drawSparks(canvas);
    }

    private void drawGlyph(Canvas canvas, float fillTop, boolean overCharge) {
        final int color = overCharge ? mGlyphColor : mFillColor;
        canvas.save();
        if (overCharge) {
            canvas.clipRect(mBody.left, fillTop, mBody.right, mBody.bottom);
        } else {
            canvas.clipRect(mBody.left, mBody.top, mBody.right, fillTop);
        }
        switch (mState) {
            case CHARGING: {
                // Bolt breathes gently with the wave.
                final float pulse = 0.9f + 0.1f * (float) Math.sin(mPhase * 2 * Math.PI);
                canvas.scale(pulse, pulse, mBody.centerX(), mBody.centerY());
                mGlyphPaint.setColor(color);
                canvas.drawPath(mBolt, mGlyphPaint);
                break;
            }
            case BYPASS:
                drawPause(canvas, color);
                break;
            case FULL:
                mStrokePaint.setColor(color);
                canvas.drawPath(mCheck, mStrokePaint);
                break;
            default:
                break;
        }
        canvas.restore();
    }

    /** Liquid surface across the body at height top, one wavelength per body width. */
    private void buildWave(float top, float amp, float phase) {
        final float left = mBody.left;
        final float width = mBody.width();
        final int steps = 24;
        mWavePath.reset();
        mWavePath.moveTo(left, mBody.bottom);
        for (int i = 0; i <= steps; i++) {
            final float x = left + width * i / steps;
            final double angle = 2 * Math.PI * ((double) i / steps + phase);
            mWavePath.lineTo(x, top + amp * (float) Math.sin(angle));
        }
        mWavePath.lineTo(mBody.right, mBody.bottom);
        mWavePath.close();
    }

    /**
     * Bubbles rising through the charge, each on its own lane, size and timing, fading out
     * as they reach the surface.
     */
    private void drawBubbles(Canvas canvas, float fillTop) {
        final float depth = mBody.bottom - fillTop;
        if (depth < KiwiStyle.dp(getContext(), 12)) return;
        final float base = mBody.width() * 0.05f;
        mGlyphPaint.setColor(mGlyphColor);
        for (int i = 0; i < BUBBLES; i++) {
            // Fixed pseudo-random lane, size and offset per bubble.
            final float lane = 0.18f + 0.64f * ((i * 0.618f) % 1f);
            final float size = base * (0.6f + 0.5f * ((i * 0.37f) % 1f));
            final float t = (mPhase * (1f + 0.25f * (i % 3)) + i / (float) BUBBLES) % 1f;
            final float y = mBody.bottom - size - t * (depth - size);
            final float wobble = (float) Math.sin((t * 3 + i) * Math.PI) * base * 0.6f;
            final float x = mBody.left + mBody.width() * lane + wobble;
            final float fade = t < 0.15f ? t / 0.15f : t > 0.8f ? (1f - t) / 0.2f : 1f;
            mGlyphPaint.setAlpha(Math.round(90 * Math.max(0f, fade)));
            canvas.drawCircle(x, y, size, mGlyphPaint);
        }
        mGlyphPaint.setAlpha(255);
    }

    /** Two rounded bars in the middle of the battery: charging is paused. */
    private void drawPause(Canvas canvas, int color) {
        final float w = mBody.width();
        final float barW = w * 0.13f;
        final float barH = w * 0.40f;
        final float gap = w * 0.11f;
        final float cx = mBody.centerX();
        final float cy = mBody.centerY();
        mGlyphPaint.setColor(color);
        mBar.set(cx - gap / 2f - barW, cy - barH / 2f, cx - gap / 2f, cy + barH / 2f);
        canvas.drawRoundRect(mBar, barW / 2f, barW / 2f, mGlyphPaint);
        mBar.set(cx + gap / 2f, cy - barH / 2f, cx + gap / 2f + barW, cy + barH / 2f);
        canvas.drawRoundRect(mBar, barW / 2f, barW / 2f, mGlyphPaint);
    }

    /**
     * Current flowing around the battery: a bright round head with a tail that fades behind
     * it, drawn as short ring segments with falling opacity. The segments meet end to end
     * with flat ends, so no part is drawn twice (overlaps would show as beads). A faint full
     * ring underneath keeps the path visible.
     */
    private void drawComet(Canvas canvas) {
        if (mRingLength <= 0f) return;
        mFlowPaint.setColor(mFlowColor);
        mFlowPaint.setAlpha(40);
        mFlowPaint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawPath(mRingPath, mFlowPaint);

        final float tail = mRingLength * COMET_LENGTH;
        final float head = mPhase * mRingLength;
        final float step = tail / COMET_STEPS;
        mFlowPaint.setStrokeCap(Paint.Cap.BUTT);
        for (int i = 0; i < COMET_STEPS; i++) {
            final float end = head - i * step;
            mFlowPaint.setAlpha(Math.round(255 * (1f - i / (float) COMET_STEPS)));
            drawRingSegment(canvas, end - step, end);
        }
        mFlowPaint.setStrokeCap(Paint.Cap.ROUND);

        // The head: a dot as wide as the stroke.
        mRingMeasure.getPosTan(head % mRingLength, mPos, null);
        mGlyphPaint.setColor(mFlowColor);
        canvas.drawCircle(mPos[0], mPos[1], mFlowPaint.getStrokeWidth() * 0.6f, mGlyphPaint);
    }

    /** Draws [start, end] along the closed ring, wrapping past its start point. */
    private void drawRingSegment(Canvas canvas, float start, float end) {
        float s = start % mRingLength;
        float e = end % mRingLength;
        if (s < 0) s += mRingLength;
        if (e < 0) e += mRingLength;
        mSegment.reset();
        if (s <= e) {
            mRingMeasure.getSegment(s, e, mSegment, true);
        } else {
            mRingMeasure.getSegment(s, mRingLength, mSegment, true);
            mRingMeasure.getSegment(0, e, mSegment, true);
        }
        canvas.drawPath(mSegment, mFlowPaint);
    }

    /** Short sparks flying out from the battery once, when it becomes full. */
    private void drawSparks(Canvas canvas) {
        final float cx = mBody.centerX();
        final float cy = mBody.centerY();
        final float inner = mBody.height() * 0.55f;
        final float reach = KiwiStyle.dp(getContext(), 10);
        mFlowPaint.setColor(mFlowColor);
        mFlowPaint.setStrokeCap(Paint.Cap.ROUND);
        mFlowPaint.setAlpha(Math.round(255 * (1f - mSpark)));
        for (int i = 0; i < SPARKS; i++) {
            final double a = 2 * Math.PI * i / SPARKS + Math.PI / SPARKS;
            final float r0 = inner + reach * mSpark;
            final float r1 = r0 + reach * 0.6f * (1f - mSpark);
            final float dx = (float) Math.cos(a);
            final float dy = (float) Math.sin(a);
            // Squash vertically so the sparks follow the battery's tall shape.
            canvas.drawLine(cx + dx * r0 * 0.62f, cy + dy * r0, cx + dx * r1 * 0.62f,
                    cy + dy * r1, mFlowPaint);
        }
    }
}
