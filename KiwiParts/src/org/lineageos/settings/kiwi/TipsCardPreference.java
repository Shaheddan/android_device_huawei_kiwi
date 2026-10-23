package org.lineageos.settings.kiwi;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * "Battery health tips": temperature, not staying full, storage charge and everyday drain.
 * The tips rise in one after another as the card opens.
 */
public class TipsCardPreference extends HealthCardPreference {

    private static final int[][] TIPS = {
        { R.drawable.ic_tip_heat, R.string.tip_heat_title, R.string.tip_heat_body },
        { R.drawable.ic_tip_full, R.string.tip_full_title, R.string.tip_full_body },
        { R.drawable.ic_tip_storage, R.string.tip_storage_title, R.string.tip_storage_body },
        { R.drawable.ic_tip_drain, R.string.tip_drain_title, R.string.tip_drain_body },
    };

    public TipsCardPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setIcon(R.drawable.ic_health_tips);
        setTitle(R.string.tips_title);
    }

    @Override
    protected int getDetailsLayout() {
        return R.layout.health_tips_details;
    }

    @Override
    protected void onBindDetails(ViewGroup details) {
        final Context c = getContext();
        final ViewGroup list = (ViewGroup) details.findViewById(R.id.tips_list);
        if (list.getChildCount() == 0) {
            final LayoutInflater inflater = LayoutInflater.from(c);
            for (int[] tip : TIPS) {
                inflater.inflate(R.layout.health_tip_row, list, true);
            }
        }
        for (int i = 0; i < TIPS.length; i++) {
            final View row = list.getChildAt(i);
            // A bind (new or recycled view) shows the tips settled, never mid-animation.
            row.animate().cancel();
            row.setAlpha(1f);
            row.setTranslationY(0f);
            final ImageView icon = (ImageView) row.findViewById(R.id.tip_icon);
            final GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(KiwiStyle.secondaryContainer(c));
            icon.setBackground(circle);
            icon.setImageResource(TIPS[i][0]);
            icon.setImageTintList(ColorStateList.valueOf(KiwiStyle.onSecondaryContainer(c)));
            final TextView title = (TextView) row.findViewById(R.id.tip_title);
            title.setText(TIPS[i][1]);
            title.setTextColor(KiwiStyle.onSurface(c));
            final TextView body = (TextView) row.findViewById(R.id.tip_body);
            body.setText(TIPS[i][2]);
            body.setTextColor(KiwiStyle.onSurfaceVariant(c));
        }
    }

    @Override
    protected void onDetailsShown(ViewGroup details) {
        final ViewGroup list = (ViewGroup) details.findViewById(R.id.tips_list);
        final float distance = KiwiStyle.dp(getContext(), 16);
        for (int i = 0; i < list.getChildCount(); i++) {
            KiwiMotion.rise(list.getChildAt(i), 120 + 60L * i, distance);
        }
    }
}
