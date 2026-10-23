package org.lineageos.settings.kiwi;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ColorDrawable;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

/**
 * Pixel's expandable Battery health card: a rounded card with an icon, a title, an optional
 * summary and a chevron in a small circle. Tapping the header opens the card: the chevron
 * turns over, the details grow open while the cards below slide down, and the details fade
 * in. Subclasses supply the details and can add their own entrance animation.
 */
public abstract class HealthCardPreference extends Preference {

    private boolean mExpanded;
    private View mBoundView;
    private ValueAnimator mAnim;

    public HealthCardPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.health_card);
        setSelectable(false);
        setPersistent(false);
    }

    /** Layout inflated once into the card body. */
    protected abstract int getDetailsLayout();

    /** Fill the card body (called on every bind). */
    protected abstract void onBindDetails(ViewGroup details);

    /** Extra animation for the details as the card opens (count-ups, staggered rows). */
    protected void onDetailsShown(ViewGroup details) {}

    /** The header without its summary and icon reads better while the card is open. */
    protected boolean hideHeaderDetailsWhenExpanded() {
        return false;
    }

    public boolean isExpanded() {
        return mExpanded;
    }

    /** Opens or closes the card without animation (from code, not a tap). */
    public void setExpanded(boolean expanded) {
        if (expanded != mExpanded) {
            mExpanded = expanded;
            notifyChanged();
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        final Context c = getContext();
        final View root = holder.itemView;
        if (root != mBoundView && mAnim != null) mAnim.cancel();
        mBoundView = root;

        final View card = holder.findViewById(R.id.health_card);
        final View header = holder.findViewById(R.id.health_card_header);
        final ViewGroup details = (ViewGroup) holder.findViewById(R.id.health_card_details);
        final ImageView chevron = (ImageView) holder.findViewById(R.id.health_card_chevron);
        final ImageView icon = (ImageView) holder.findViewById(android.R.id.icon);
        final TextView title = (TextView) holder.findViewById(android.R.id.title);
        final TextView summary = (TextView) holder.findViewById(android.R.id.summary);

        final float r = KiwiStyle.dp(c, KiwiStyle.CARD_RADIUS_DP);
        card.setBackground(KiwiStyle.rounded(c, KiwiStyle.CARD_RADIUS_DP,
                KiwiStyle.surfaceContainer(c)));
        // Ripple on the header only, shaped like the card's top (all corners when closed).
        header.setBackground(KiwiStyle.ripple(c, new ColorDrawable(0),
                KiwiStyle.radii(r, mExpanded ? 0 : r)));
        title.setTextColor(KiwiStyle.onSurface(c));
        summary.setTextColor(KiwiStyle.onSurfaceVariant(c));
        icon.setImageTintList(android.content.res.ColorStateList.valueOf(KiwiStyle.onSurface(c)));

        final GradientDrawable chevronBg = new GradientDrawable();
        chevronBg.setShape(GradientDrawable.OVAL);
        chevronBg.setColor(KiwiStyle.surfaceContainerHighest(c));
        chevron.setBackground(chevronBg);
        chevron.setImageTintList(
                android.content.res.ColorStateList.valueOf(KiwiStyle.onSurface(c)));

        if (details.getChildCount() == 0) {
            LayoutInflater.from(c).inflate(getDetailsLayout(), details, true);
        }
        onBindDetails(details);

        // Final state for this bind (no animation running for this view).
        if (mAnim == null || !mAnim.isRunning()) {
            details.setVisibility(mExpanded ? View.VISIBLE : View.GONE);
            details.setAlpha(1f);
            final ViewGroup.LayoutParams lp = details.getLayoutParams();
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            details.setLayoutParams(lp);
            chevron.setRotation(mExpanded ? 180f : 0f);
        }
        applyHeaderState(icon, summary, false);

        header.setOnClickListener(v -> toggle(card, header, details, chevron, icon, summary));
        updateAccessibility(header, title, summary);
    }

    /** Spoken as "Battery capacity, 87%, collapsed". */
    private void updateAccessibility(View header, TextView title, TextView summary) {
        final Context c = getContext();
        final CharSequence s = summary.getVisibility() == View.VISIBLE ? summary.getText() : null;
        header.setContentDescription(s == null || s.length() == 0 ? title.getText()
                : title.getText() + ", " + s);
        header.setStateDescription(c.getString(mExpanded
                ? R.string.kiwi_state_expanded : R.string.kiwi_state_collapsed));
    }

    private void applyHeaderState(ImageView icon, TextView summary, boolean animate) {
        if (!hideHeaderDetailsWhenExpanded()) return;
        if (animate) {
            final AutoTransition t = new AutoTransition();
            t.setDuration(KiwiMotion.MEDIUM);
            t.setInterpolator(KiwiMotion.EMPHASIZED);
            TransitionManager.beginDelayedTransition((ViewGroup) icon.getParent(), t);
        }
        final boolean showSummary = !mExpanded && summary.getText() != null
                && summary.getText().length() > 0;
        summary.setVisibility(showSummary ? View.VISIBLE : View.GONE);
        icon.setVisibility(mExpanded ? View.GONE : View.VISIBLE);
    }

    private void toggle(View card, View header, ViewGroup details, ImageView chevron,
            ImageView icon, TextView summary) {
        // The header keeps its ripple (replacing it would cut the touch ripple short); its
        // shape follows the open state from the next bind.
        mExpanded = !mExpanded;
        if (mAnim != null) mAnim.cancel();

        final TextView title = (TextView) header.findViewById(android.R.id.title);
        if (!KiwiMotion.enabled()) {
            details.setVisibility(mExpanded ? View.VISIBLE : View.GONE);
            chevron.setRotation(mExpanded ? 180f : 0f);
            applyHeaderState(icon, summary, false);
            updateAccessibility(header, title, summary);
            return;
        }

        applyHeaderState(icon, summary, true);
        updateAccessibility(header, title, summary);
        chevron.animate().rotation(mExpanded ? 180f : 0f).setDuration(KiwiMotion.MEDIUM)
                .setInterpolator(KiwiMotion.EMPHASIZED).start();

        // Grow (or shrink) the body's height every frame; the list lays out again each frame,
        // so the cards below slide with it.
        final int width = card.getWidth() - card.getPaddingLeft() - card.getPaddingRight();
        final int from;
        final int to;
        if (mExpanded) {
            // Reopening while still closing continues from the current height.
            final boolean wasShowing = details.getVisibility() == View.VISIBLE;
            from = wasShowing ? details.getHeight() : 0;
            details.setVisibility(View.VISIBLE);
            details.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            to = details.getMeasuredHeight();
            if (!wasShowing) details.setAlpha(0f);
        } else {
            from = details.getHeight();
            to = 0;
        }
        final ViewGroup.LayoutParams lp = details.getLayoutParams();
        lp.height = from;
        details.setLayoutParams(lp);

        final boolean opening = mExpanded;
        mAnim = ValueAnimator.ofInt(from, to);
        mAnim.setDuration(opening ? KiwiMotion.LONG : KiwiMotion.MEDIUM);
        mAnim.setInterpolator(opening ? KiwiMotion.EMPHASIZED : KiwiMotion.EMPHASIZED_ACCELERATE);
        mAnim.addUpdateListener(a -> {
            lp.height = (Integer) a.getAnimatedValue();
            details.setLayoutParams(lp);
            final float f = a.getAnimatedFraction();
            // Content fades in during the second half of opening, out in the first half of
            // closing, so it never shows squashed.
            details.setAlpha(opening ? Math.max(0f, (f - 0.35f) / 0.65f)
                    : Math.max(0f, 1f - f * 2f));
        });
        mAnim.addListener(new AnimatorListenerAdapter() {
            private boolean mCancelled;

            @Override
            public void onAnimationCancel(Animator animation) {
                mCancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                details.setLayoutParams(lp);
                details.setAlpha(1f);
                if (!opening && !mCancelled) details.setVisibility(View.GONE);
            }
        });
        mAnim.start();
        if (opening) onDetailsShown(details);
    }
}
