package org.lineageos.settings.kiwi;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.SeekBarPreference;
import android.content.Intent;
import android.content.SharedPreferences;

public class BypassChargingActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {

        private Preference mThresholdPref;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceManager().setStorageDeviceProtected();
            addPreferencesFromResource(R.xml.bypass_settings);
            mThresholdPref = findPreference("bypass_threshold");
            updateThresholdTitle();
        }

        private void updateThresholdTitle() {
            int value = getPreferenceManager().getSharedPreferences()
                    .getInt("bypass_threshold", 80);
            mThresholdPref.setTitle(getString(R.string.bypass_threshold_title)
                    + ": " + value + "%");
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if ("bypass_threshold".equals(key)) {
                updateThresholdTitle();
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
            updateThresholdTitle();
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceManager().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
            // Re-evaluate immediately with new settings
            getContext().startService(new Intent(getContext(), BypassService.class));
        }
    }
}
