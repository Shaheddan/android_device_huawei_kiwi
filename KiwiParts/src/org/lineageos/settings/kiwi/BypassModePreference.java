package org.lineageos.settings.kiwi;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

/**
 * One bypass mode as a radio row, the way Pixel lists "Adaptive Charging" and "Limit to
 * 80%" under Charging optimization. Not persistent: the page writes the bypass_now /
 * bypass_auto keys that BypassService reads when a row is picked.
 */
public class BypassModePreference extends Preference {

    private int mGroupPosition = KiwiStyle.GROUP_SINGLE;
    private boolean mChecked;
    private View mBoundView;

    public BypassModePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.bypass_mode_row);
        setPersistent(false);
    }

    public void setGroupPosition(int position) {
        if (position != mGroupPosition) {
            mGroupPosition = position;
            notifyChanged();
        }
    }

    public void setChecked(boolean checked) {
        if (checked != mChecked) {
            mChecked = checked;
            notifyChanged();
        }
    }

    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        final Context c = getContext();
        final boolean animate = holder.itemView == mBoundView;
        mBoundView = holder.itemView;
        KiwiRows.styleRow(c, holder, mGroupPosition, true);

        final RadioDotView radio = (RadioDotView) holder.findViewById(R.id.mode_radio);
        radio.setColors(KiwiStyle.primary(c), KiwiStyle.onSurfaceVariant(c));
        radio.setChecked(mChecked, animate);
        // Disabled rows (main switch off) fade like the rest of Settings.
        radio.setAlpha(isEnabled() ? 1f : 0.38f);

        // Read as a radio button: "Limit to 80%, selected".
        holder.itemView.setStateDescription(c.getString(mChecked
                ? R.string.kiwi_state_selected : R.string.kiwi_state_not_selected));
    }
}
