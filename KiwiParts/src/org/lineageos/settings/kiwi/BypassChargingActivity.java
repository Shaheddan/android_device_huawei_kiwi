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

        // Battery at or below this is drawn in the error colour while on battery.
        private static final int LOW_LEVEL = 15;

        private BypassSwitchPreference mBypassNow;
        private BypassSwitchPreference mBypassAuto;
        private BypassLevelPreference mThreshold;
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

            // The three rows share one rounded card.
            if (mBypassNow != null) mBypassNow.setGroupPosition(KiwiStyle.GROUP_TOP);
            if (mBypassAuto != null) mBypassAuto.setGroupPosition(KiwiStyle.GROUP_MIDDLE);
            if (mThreshold != null) mThreshold.setGroupPosition(KiwiStyle.GROUP_BOTTOM);

            updateThresholdTitle();
            updateInterlock();
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            // The rounded rows and the gaps between them separate items; no dividers.
            setDivider(null);
        }

        private void updateThresholdTitle() {
            if (mThreshold == null) return;
            // The value sits under the title instead of inside it.
            mThreshold.setSummary(getString(R.string.bypass_threshold_summary,
                    mThreshold.getValue()));
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
                // A mode is on but the service applies it with the next battery update.
                mStatus.setStatus(state, pct, false,
                        getString(R.string.bypass_chip_starting),
                        getString(R.string.bypass_status_pending_title),
                        getString(R.string.bypass_status_pending_summary));
            } else {
                mStatus.setStatus(state, pct, false, getString(chip), title,
                        getString(R.string.bypass_status_charging_off));
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
