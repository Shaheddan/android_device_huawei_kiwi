package org.lineageos.settings.kiwi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Bundle;
import android.view.View;

import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;

/**
 * Charging optimization (opened from Battery health): bypass charging in the Android 15
 * layout. A main switch turns it on, and two radio modes, like Pixel's Adaptive Charging /
 * Limit to 80 %, choose how:
 *  - Limit to N %: charge normally up to the limit, then run from the charger (bypass_auto);
 *  - Hold current level: stop charging now and run from the charger (bypass_now).
 * The keys and values are the ones BypassService has always read, so the service is
 * unchanged apart from applying a change at once.
 */
public class BypassChargingActivity extends CollapsingToolbarBaseActivity {

    static final String KEY_NOW = "bypass_now";
    static final String KEY_AUTO = "bypass_auto";
    static final String KEY_THRESHOLD = "bypass_threshold";
    /** The mode the main switch brings back when turned on again. */
    static final String KEY_LAST_MODE = "bypass_last_mode";
    static final String MODE_AUTO = "auto";
    static final String MODE_NOW = "now";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bypass_content);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.bypass_container, new SettingsFragment())
                    .commit();
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat
            implements SharedPreferences.OnSharedPreferenceChangeListener {

        // Battery at or below this is drawn in the error colour while on battery.
        private static final int LOW_LEVEL = 15;

        private BypassStatusPreference mStatus;
        private BypassMainSwitchPreference mMain;
        private BypassModePreference mModeLimit;
        private BypassModePreference mModeHold;
        private BypassLevelPreference mThreshold;
        private Intent mLastBattery;
        private boolean mFirstShow;

        // Keeps the status card live while the page is open.
        private final BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mLastBattery = intent;
                updateStatus();
            }
        };

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            final PreferenceManager manager = getPreferenceManager();
            manager.setStorageDeviceProtected();
            // Lets the list animate the limit slider in and out when the mode changes.
            manager.setPreferenceComparisonCallback(
                    new PreferenceManager.SimplePreferenceComparisonCallback());
            setPreferencesFromResource(R.xml.bypass_settings, rootKey);
            mFirstShow = savedInstanceState == null;

            mStatus = findPreference("bypass_status");
            mMain = findPreference("bypass_main");
            mModeLimit = findPreference("bypass_mode_limit");
            mModeHold = findPreference("bypass_mode_hold");
            mThreshold = findPreference(KEY_THRESHOLD);

            // Limit, its slider (only while Limit is picked) and Hold share one card.
            mModeLimit.setGroupPosition(KiwiStyle.GROUP_TOP);
            mThreshold.setGroupPosition(KiwiStyle.GROUP_MIDDLE);
            mModeHold.setGroupPosition(KiwiStyle.GROUP_BOTTOM);

            mMain.setOnPreferenceChangeListener((p, value) -> {
                setOn((Boolean) value);
                return true;
            });
            mModeLimit.setOnPreferenceClickListener(p -> {
                selectMode(MODE_AUTO);
                return true;
            });
            mModeHold.setOnPreferenceClickListener(p -> {
                selectMode(MODE_NOW);
                return true;
            });
            updateControls();
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            // The rounded rows and the gaps between them separate items; no dividers.
            setDivider(null);
            KiwiPage.setUpList(getListView(), mFirstShow);
        }

        private SharedPreferences prefs() {
            return getPreferenceManager().getSharedPreferences();
        }

        /** Main switch: off clears both modes; on brings back the mode used last. */
        private void setOn(boolean on) {
            final SharedPreferences p = prefs();
            if (on) {
                final boolean now = MODE_NOW.equals(p.getString(KEY_LAST_MODE, MODE_AUTO));
                p.edit().putBoolean(KEY_NOW, now).putBoolean(KEY_AUTO, !now).apply();
            } else {
                p.edit().putBoolean(KEY_NOW, false).putBoolean(KEY_AUTO, false).apply();
            }
            applyNow();
        }

        private void selectMode(String mode) {
            final boolean now = MODE_NOW.equals(mode);
            prefs().edit().putString(KEY_LAST_MODE, mode)
                    .putBoolean(KEY_NOW, now).putBoolean(KEY_AUTO, !now).apply();
            applyNow();
        }

        /** BypassService applies the settings as soon as it is started. */
        private void applyNow() {
            requireContext().startService(new Intent(requireContext(), BypassService.class));
        }

        private String currentMode(SharedPreferences p) {
            if (p.getBoolean(KEY_NOW, false)) return MODE_NOW;
            if (p.getBoolean(KEY_AUTO, false)) return MODE_AUTO;
            return p.getString(KEY_LAST_MODE, MODE_AUTO);
        }

        private void updateControls() {
            final SharedPreferences p = prefs();
            final boolean on = p.getBoolean(KEY_NOW, false) || p.getBoolean(KEY_AUTO, false);
            final String mode = currentMode(p);
            final int threshold = p.getInt(KEY_THRESHOLD, 80);

            mMain.setChecked(on);
            mModeLimit.setChecked(MODE_AUTO.equals(mode));
            mModeHold.setChecked(MODE_NOW.equals(mode));
            // Like Pixel: the options stay visible but greyed out while switched off.
            mModeLimit.setEnabled(on);
            mModeHold.setEnabled(on);
            mModeLimit.setTitle(getString(R.string.bypass_limit_title, threshold));
            mThreshold.setSummary(getString(R.string.bypass_threshold_summary, threshold));
            // The slider belongs to Limit: it slides in under it only when Limit is in use.
            mThreshold.setVisible(on && MODE_AUTO.equals(mode));
        }

        /**
         * Shows what is actually happening, from the battery broadcast: "held" only when
         * a mode is on, the charger is plugged in and the battery reports not charging.
         */
        private void updateStatus() {
            if (mStatus == null || mLastBattery == null) return;
            final SharedPreferences prefs = prefs();
            final boolean now = prefs.getBoolean(KEY_NOW, false);
            final boolean auto = prefs.getBoolean(KEY_AUTO, false);
            final int threshold = prefs.getInt(KEY_THRESHOLD, 80);

            final int level = mLastBattery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            final int scale = mLastBattery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            final int pct = scale > 0 && level >= 0 ? level * 100 / scale : level;
            final int plugged = mLastBattery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
            final int status = mLastBattery.getIntExtra(BatteryManager.EXTRA_STATUS,
                    BatteryManager.BATTERY_STATUS_UNKNOWN);
            if (plugged == 0) {
                mStatus.setStatus(BatteryAnimationView.State.DISCHARGING, pct,
                        pct >= 0 && pct <= LOW_LEVEL,
                        getString(R.string.bypass_chip_battery),
                        getString(R.string.bypass_status_unplugged_title),
                        getString(now || auto ? R.string.bypass_status_unplugged_armed
                                : R.string.bypass_status_unplugged_off));
                return;
            }

            final boolean held = (now || auto)
                    && (status == BatteryManager.BATTERY_STATUS_NOT_CHARGING
                        || status == BatteryManager.BATTERY_STATUS_DISCHARGING);
            if (held) {
                mStatus.setStatus(BatteryAnimationView.State.BYPASS, pct, false,
                        getString(R.string.bypass_chip_bypass),
                        getString(R.string.bypass_status_active_title),
                        getString(R.string.bypass_status_active_summary, pct));
                return;
            }

            // Plugged in and not held: animate only what the battery really does.
            final BatteryAnimationView.State state;
            final int chip;
            if (status == BatteryManager.BATTERY_STATUS_FULL) {
                state = BatteryAnimationView.State.FULL;
                chip = R.string.bypass_chip_full;
            } else if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
                state = BatteryAnimationView.State.CHARGING;
                chip = R.string.bypass_chip_charging;
            } else {
                state = BatteryAnimationView.State.DISCHARGING;
                chip = R.string.bypass_chip_plugged;
            }
            final String title = getString(status == BatteryManager.BATTERY_STATUS_FULL
                    ? R.string.bypass_status_full_title : R.string.bypass_status_charging_title);
            if (auto && !now && pct < threshold) {
                mStatus.setStatus(state, pct, false, getString(chip), title,
                        getString(R.string.bypass_status_charging_to, threshold));
            } else if (now || auto) {
                // A mode is on; the battery reports the change with its next update.
                mStatus.setStatus(state, pct, false,
                        getString(R.string.bypass_chip_starting),
                        getString(R.string.bypass_status_pending_title),
                        getString(R.string.bypass_status_pending_summary));
            } else {
                mStatus.setStatus(state, pct, false, getString(chip), title,
                        getString(R.string.bypass_status_charging_off));
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            updateControls();
            updateStatus();
        }

        @Override
        public void onResume() {
            super.onResume();
            prefs().registerOnSharedPreferenceChangeListener(this);
            final Intent sticky = requireContext().registerReceiver(mBatteryReceiver,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (sticky != null) mLastBattery = sticky;
            updateControls();
            updateStatus();
        }

        @Override
        public void onPause() {
            super.onPause();
            prefs().unregisterOnSharedPreferenceChangeListener(this);
            requireContext().unregisterReceiver(mBatteryReceiver);
            // Re-evaluate with the final settings (the slider writes as it moves).
            applyNow();
        }
    }
}
