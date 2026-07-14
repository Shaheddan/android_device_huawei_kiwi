package org.lineageos.settings.kiwi;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.FileOutputStream;

public class BypassService extends Service {
    private static final String TAG = "KiwiBypass";
    private static final String NODE = "/sys/class/power_supply/battery/factory_diag";
    private static final int HYSTERESIS = 2;

    private boolean mBypassActive = false;

    private final BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            evaluate(intent);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Intent sticky = registerReceiver(mBatteryReceiver,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (sticky != null) evaluate(sticky);
    }

    private void evaluate(Intent batteryIntent) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(createDeviceProtectedStorageContext());
        boolean now = prefs.getBoolean("bypass_now", false);
        boolean auto = prefs.getBoolean("bypass_auto", false);
        int threshold = prefs.getInt("bypass_threshold", 80);

        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);

        if (plugged == 0 || level < 0 || (!now && !auto)) {
            setBypass(false);
            if (plugged == 0) stopSelf();
            return;
        }

        if (now) {
            // Mode 1: hold at whatever level we're at, right now
            setBypass(true);
            return;
        }

        // Mode 2: charge normally until threshold, then hold there
        if (level >= threshold) {
            setBypass(true);
        } else if (level <= threshold - HYSTERESIS) {
            setBypass(false);
        }
        // Between threshold-HYSTERESIS and threshold: keep current state (no flapping)
    }

    private void setBypass(boolean on) {
        if (mBypassActive == on) return;
        mBypassActive = on;
        writeNode(on ? "0" : "1");
        Log.i(TAG, "Bypass charging " + (on ? "engaged" : "released"));
    }

    static void writeNode(String value) {
        try (FileOutputStream fos = new FileOutputStream(NODE)) {
            fos.write(value.getBytes());
        } catch (Exception e) {
            Log.e(TAG, "Failed to write " + NODE, e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mBatteryReceiver);
        writeNode("1");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
