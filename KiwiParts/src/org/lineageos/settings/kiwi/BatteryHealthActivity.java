package org.lineageos.settings.kiwi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;

/**
 * Settings > Battery > Battery health, laid out like Pixel's: status badge, Battery capacity
 * and Battery health tips cards, then Charging optimization, which opens the bypass
 * charging page.
 */
public class BatteryHealthActivity extends CollapsingToolbarBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bypass_content);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.bypass_container, new HealthFragment())
                    .commit();
        }
    }

    public static class HealthFragment extends PreferenceFragmentCompat
            implements SharedPreferences.OnSharedPreferenceChangeListener {

        private HealthBadgePreference mBadge;
        private CapacityCardPreference mCapacity;
        private Preference mOptimization;
        private Intent mLastBattery;
        private boolean mFirstShow;
        private int mLastStatus = -1;

        // The cycle count changes when a charge ends; keep the page current while open.
        private final BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mLastBattery = intent;
                refresh();
            }
        };

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setStorageDeviceProtected();
            setPreferencesFromResource(R.xml.health_settings, rootKey);
            mFirstShow = savedInstanceState == null;

            mBadge = findPreference("health_status");
            mCapacity = findPreference("health_capacity");
            mOptimization = findPreference("charging_optimization");
            if (mBadge != null && !mFirstShow) mBadge.skipIntro();

            if (mCapacity != null) {
                mCapacity.setOnEditHistory(() ->
                        HealthHistoryDialog.show(requireContext(), this::refresh));
            }
            if (mOptimization != null) {
                mOptimization.setOnPreferenceClickListener(p -> {
                    startActivity(new Intent(requireContext(), BypassChargingActivity.class));
                    return true;
                });
            }
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            setDivider(null);
            KiwiPage.setUpList(getListView(), mFirstShow);
        }

        private void refresh() {
            if (getContext() == null) return;
            final BatteryHealth.Estimate e = BatteryHealth.estimate(requireContext(), mLastBattery);
            if (mBadge != null) mBadge.setStatus(e.status);
            if (mCapacity != null) mCapacity.setEstimate(e);
            updateOptimizationSummary();
            if (e.status != mLastStatus) {
                // Settings > Battery shows the status too; tell it when it changes.
                mLastStatus = e.status;
                HealthSummaryProvider.notifyChanged(requireContext());
            }
        }

        /** What Charging optimization is set to, under its title like Pixel. */
        private void updateOptimizationSummary() {
            if (mOptimization == null) return;
            final SharedPreferences p = getPreferenceManager().getSharedPreferences();
            if (p.getBoolean(BypassChargingActivity.KEY_NOW, false)) {
                mOptimization.setSummary(R.string.optimization_hold);
            } else if (p.getBoolean(BypassChargingActivity.KEY_AUTO, false)) {
                mOptimization.setSummary(getString(R.string.optimization_limit,
                        p.getInt(BypassChargingActivity.KEY_THRESHOLD, 80)));
            } else {
                mOptimization.setSummary(R.string.optimization_off);
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (key != null && key.startsWith("bypass_")) updateOptimizationSummary();
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
            final Intent sticky = requireContext().registerReceiver(mBatteryReceiver,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (sticky != null) mLastBattery = sticky;
            refresh();
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceManager().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
            requireContext().unregisterReceiver(mBatteryReceiver);
        }
    }
}
