package org.lineageos.settings.kiwi;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

/**
 * Gives Settings > Battery the live summary under "Battery health" (Normal / Reduced /
 * Not set up), like Pixel. Settings reads it through the tile's
 * com.android.settings.summary_uri: it calls this provider with the first path segment as
 * the method and takes the summary from the returned bundle. It also watches the URI, so
 * {@link #notifyChanged} refreshes the summary while Settings is open.
 */
public class HealthSummaryProvider extends ContentProvider {

    static final String AUTHORITY = "org.lineageos.settings.kiwi.health";
    static final Uri SUMMARY_URI =
            Uri.parse("content://" + AUTHORITY + "/getSummary/battery_health");
    /** TileUtils.META_DATA_PREFERENCE_SUMMARY */
    private static final String KEY_SUMMARY = "com.android.settings.summary";

    static void notifyChanged(Context c) {
        c.getContentResolver().notifyChange(SUMMARY_URI, null);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        final Context c = getContext();
        final BatteryHealth.Estimate e =
                BatteryHealth.estimate(c, BatteryHealth.stickyBattery(c));
        final Bundle out = new Bundle();
        out.putString(KEY_SUMMARY, c.getString(BatteryHealth.statusLabel(e.status)));
        return out;
    }

    // Nothing is stored here: the provider only answers call().

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        return 0;
    }
}
