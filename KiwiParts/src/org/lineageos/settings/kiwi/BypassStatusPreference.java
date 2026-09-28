package org.lineageos.settings.kiwi;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

/**
 * Material You status card at the top of the bypass charging page: what the charger
 * and battery are doing right now. Display only; it changes no setting.
 */
public class BypassStatusPreference extends Preference {

    public enum Icon { BATTERY, BOLT, PLUG }

    private CharSequence mStatusTitle = "";
    private CharSequence mStatusSummary = "";
    private CharSequence mLevel = "";
    private Icon mIcon = Icon.BATTERY;
    private boolean mActive;

    public BypassStatusPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.bypass_status_card);
        setSelectable(false);
        setPersistent(false);
    }

    public void setStatus(Icon icon, CharSequence title, CharSequence summary,
            CharSequence level, boolean active) {
        mIcon = icon;
        mStatusTitle = title;
        mStatusSummary = summary;
        mLevel = level;
        mActive = active;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        final boolean night = (getContext().getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        // Material You roles from the dynamic palette: the primary container while
        // bypass is holding the battery, a neutral surface otherwise.
        final int cardColor, iconBgColor, iconColor, titleColor, summaryColor;
        if (mActive) {
            cardColor = color(night ? android.R.color.system_accent1_700
                    : android.R.color.system_accent1_100);
            iconBgColor = color(night ? android.R.color.system_accent1_200
                    : android.R.color.system_accent1_600);
            iconColor = color(night ? android.R.color.system_accent1_800
                    : android.R.color.system_accent1_0);
            titleColor = color(night ? android.R.color.system_accent1_50
                    : android.R.color.system_accent1_900);
            summaryColor = color(night ? android.R.color.system_accent1_200
                    : android.R.color.system_accent1_700);
        } else {
            cardColor = color(night ? android.R.color.system_neutral2_800
                    : android.R.color.system_neutral2_100);
            iconBgColor = color(night ? android.R.color.system_accent2_700
                    : android.R.color.system_accent2_200);
            iconColor = color(night ? android.R.color.system_accent2_100
                    : android.R.color.system_accent2_800);
            titleColor = color(night ? android.R.color.system_neutral1_50
                    : android.R.color.system_neutral1_900);
            summaryColor = color(night ? android.R.color.system_neutral2_200
                    : android.R.color.system_neutral2_700);
        }

        final GradientDrawable card = new GradientDrawable();
        card.setCornerRadius(dp(28));
        card.setColor(cardColor);
        holder.findViewById(R.id.status_card).setBackground(card);

        final GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.OVAL);
        iconBg.setColor(iconBgColor);
        holder.findViewById(R.id.status_icon_bg).setBackground(iconBg);

        final ImageView icon = (ImageView) holder.findViewById(R.id.status_icon);
        icon.setImageResource(mIcon == Icon.PLUG ? R.drawable.ic_bypass_plug
                : mIcon == Icon.BOLT ? R.drawable.ic_bypass_bolt
                : R.drawable.ic_bypass_battery);
        icon.setColorFilter(iconColor);

        final TextView title = (TextView) holder.findViewById(R.id.status_title);
        title.setText(mStatusTitle);
        title.setTextColor(titleColor);

        final TextView summary = (TextView) holder.findViewById(R.id.status_summary);
        summary.setText(mStatusSummary);
        summary.setTextColor(summaryColor);

        final TextView level = (TextView) holder.findViewById(R.id.status_level);
        level.setText(mLevel);
        level.setTextColor(titleColor);
        level.setVisibility(mLevel.length() > 0 ? View.VISIBLE : View.GONE);
    }

    private int color(int res) {
        return getContext().getColor(res);
    }

    private float dp(int value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getContext().getResources().getDisplayMetrics());
    }
}
