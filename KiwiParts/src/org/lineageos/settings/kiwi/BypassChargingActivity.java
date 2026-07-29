package org.lineageos.settings.kiwi;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreferenceCompat;

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

        private SwitchPreferenceCompat mBypassNow;
        private SwitchPreferenceCompat mBypassAuto;
        private SeekBarPreference mThreshold;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setStorageDeviceProtected();
            setPreferencesFromResource(R.xml.bypass_settings, rootKey);

            mBypassNow = findPreference("bypass_now");
            mBypassAuto = findPreference("bypass_auto");
            mThreshold = findPreference("bypass_threshold");

            updateThresholdTitle();
            updateInterlock();
        }

        private void updateThresholdTitle() {
            if (mThreshold == null) return;
            mThreshold.setTitle(getString(R.string.bypass_threshold_title)
                    + ": " + mThreshold.getValue() + "%");
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
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
            updateThresholdTitle();
            updateInterlock();
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceManager().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
            // Re-evaluate immediately with the new settings
            requireContext().startService(
                    new Intent(requireContext(), BypassService.class));
        }
    }
}
