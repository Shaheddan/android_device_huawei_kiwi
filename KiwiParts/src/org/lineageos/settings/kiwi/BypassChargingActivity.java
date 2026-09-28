package org.lineageos.settings.kiwi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreference;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;

public class BypassChargingActivity extends CollapsingToolbarBaseActivity {

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

        private SwitchPreference mBypassNow;
        private SwitchPreference mBypassAuto;
        private SeekBarPreference mThreshold;
        private BypassStatusPreference mStatus;
        private Intent mLastBattery;

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
            getPreferenceManager().setStorageDeviceProtected();
            setPreferencesFromResource(R.xml.bypass_settings, rootKey);

            mBypassNow = findPreference("bypass_now");
            mBypassAuto = findPreference("bypass_auto");
            mThreshold = findPreference("bypass_threshold");
            mStatus = findPreference("bypass_status");

            updateThresholdTitle();
            updateInterlock();
        }

        private void updateThresholdTitle() {
            if (mThreshold == null) return;
            // Android 13 style: the value sits under the title instead of inside it.
            mThreshold.setSummary(getString(R.string.bypass_level, mThreshold.getValue()));
        }

        /**
         * Shows what is actually happening, from the battery broadcast: "held" only when
         * a mode is on, the charger is plugged in and the battery reports not charging.
         */
        private void updateStatus() {
            if (mStatus == null || mLastBattery == null) return;
            final SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
            final boolean now = prefs.getBoolean("bypass_now", false);
            final boolean auto = prefs.getBoolean("bypass_auto", false);
            final int threshold = prefs.getInt("bypass_threshold", 80);

            final int level = mLastBattery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            final int scale = mLastBattery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            final int pct = scale > 0 && level >= 0 ? level * 100 / scale : level;
            final int plugged = mLastBattery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
            final int status = mLastBattery.getIntExtra(BatteryManager.EXTRA_STATUS,
                    BatteryManager.BATTERY_STATUS_UNKNOWN);
            final String levelText = pct >= 0 ? getString(R.string.bypass_level, pct) : "";

            if (plugged == 0) {
                mStatus.setStatus(BypassStatusPreference.Icon.BATTERY,
                        getString(R.string.bypass_status_unplugged_title),
                        getString(now || auto ? R.string.bypass_status_unplugged_armed
                                : R.string.bypass_status_unplugged_off),
                        levelText, false);
                return;
            }

            final boolean held = (now || auto)
                    && (status == BatteryManager.BATTERY_STATUS_NOT_CHARGING
                        || status == BatteryManager.BATTERY_STATUS_DISCHARGING);
            if (held) {
                mStatus.setStatus(BypassStatusPreference.Icon.PLUG,
                        getString(R.string.bypass_status_active_title),
                        getString(R.string.bypass_status_active_summary, pct),
                        levelText, true);
                return;
            }

            final String title = getString(status == BatteryManager.BATTERY_STATUS_FULL
                    ? R.string.bypass_status_full_title : R.string.bypass_status_charging_title);
            if (auto && !now && pct < threshold) {
                mStatus.setStatus(BypassStatusPreference.Icon.BOLT, title,
                        getString(R.string.bypass_status_charging_to, threshold),
                        levelText, false);
            } else if (now || auto) {
                // A mode is on but the service applies it with the next battery update.
                mStatus.setStatus(BypassStatusPreference.Icon.BOLT,
                        getString(R.string.bypass_status_pending_title),
                        getString(R.string.bypass_status_pending_summary),
                        levelText, false);
            } else {
                mStatus.setStatus(BypassStatusPreference.Icon.BOLT, title,
                        getString(R.string.bypass_status_charging_off), levelText, false);
            }
        }

        /**
         * The two modes are mutually exclusive: whichever is on greys out the other.
         * The level slider belongs to auto bypass, so it follows "bypass now".
         */
        private void updateInterlock() {
            if (mBypassNow == null || mBypassAuto == null || mThreshold == null) return;
            final boolean now = mBypassNow.isChecked();
            final boolean auto = mBypassAuto.isChecked();
            mBypassAuto.setEnabled(!now);
            mBypassNow.setEnabled(!auto);
            mThreshold.setEnabled(!now);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if ("bypass_threshold".equals(key)) {
                updateThresholdTitle();
            }
            updateInterlock();
            updateStatus();
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
            final Intent sticky = requireContext().registerReceiver(mBatteryReceiver,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (sticky != null) mLastBattery = sticky;
            updateThresholdTitle();
            updateInterlock();
            updateStatus();
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceManager().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
            requireContext().unregisterReceiver(mBatteryReceiver);
            // Re-evaluate immediately with the new settings
            requireContext().startService(
                    new Intent(requireContext(), BypassService.class));
        }
    }
}
