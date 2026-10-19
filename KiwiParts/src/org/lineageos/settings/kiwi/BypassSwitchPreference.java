package org.lineageos.settings.kiwi;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreference;

/**
 * A mode switch drawn as an Android 14 style grouped row (rounded card, icon in a tonal
 * circle). Still an androidx SwitchPreference with the SettingsLib switch widget, so the
 * key, storage and behaviour are exactly those of the old rows.
 */
public class BypassSwitchPreference extends SwitchPreference {

    private int mGroupPosition = KiwiStyle.GROUP_SINGLE;

    public BypassSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setGroupPosition(int position) {
        if (position != mGroupPosition) {
            mGroupPosition = position;
            notifyChanged();
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        KiwiRows.styleRow(getContext(), holder, mGroupPosition, true);
    }
}
