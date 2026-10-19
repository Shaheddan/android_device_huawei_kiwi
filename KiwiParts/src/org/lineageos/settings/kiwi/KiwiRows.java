package org.lineageos.settings.kiwi;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;

import androidx.preference.PreferenceViewHolder;

/** Shared styling for the grouped rows on the bypass charging page. */
final class KiwiRows {

    private KiwiRows() {}

    /**
     * Turns a stock SettingsLib row into a grouped card row. Only background, padding and
     * the icon are touched, so the row keeps its own widgets (switch, seek bar).
     */
    static void styleRow(Context c, PreferenceViewHolder holder, int position,
            boolean clickable) {
        final View row = holder.itemView;
        row.setBackground(KiwiStyle.groupedRowBackground(c, position, clickable));
        // 16dp card margin + 16dp inside the card on both sides.
        final int side = Math.round(KiwiStyle.dp(c, 32));
        final int vertical = Math.round(KiwiStyle.dp(c, 6));
        row.setPaddingRelative(side, vertical, side, vertical);

        final View icon = holder.findViewById(android.R.id.icon);
        if (icon instanceof ImageView) {
            final int pad = Math.round(KiwiStyle.dp(c, 8));
            icon.setBackground(KiwiStyle.iconCircle(c));
            icon.setPadding(pad, pad, pad, pad);
            ((ImageView) icon).setImageTintList(
                    android.content.res.ColorStateList.valueOf(
                            KiwiStyle.onPrimaryContainer(c)));
        }
    }

    /** First SeekBar under a row, found by type so no library resource id is needed. */
    static SeekBar findSeekBar(View view) {
        if (view instanceof SeekBar) return (SeekBar) view;
        if (view instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                final SeekBar found = findSeekBar(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }
}
