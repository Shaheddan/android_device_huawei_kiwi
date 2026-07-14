package org.lineageos.settings.kiwi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
            // Always restore normal charging when unplugged
            BypassService.writeNode("1");
            context.stopService(new Intent(context, BypassService.class));
        } else {
            // BOOT_COMPLETED or POWER_CONNECTED: restore sane state, start monitor
            BypassService.writeNode("1");
            context.startService(new Intent(context, BypassService.class));
        }
    }
}
