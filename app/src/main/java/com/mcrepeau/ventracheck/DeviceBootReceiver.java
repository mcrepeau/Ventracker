package com.mcrepeau.ventracheck;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * This BroadcastReceiver automatically (re)starts the alarm when the device is
 * rebooted. This receiver is set to be disabled (android:enabled="false") in the
 * application's manifest file. When the user sets the alarm, the receiver is enabled.
 * When the user cancels the alarm, the receiver is disabled, so that rebooting the
 * device will not trigger this receiver.
 */

public class DeviceBootReceiver extends BroadcastReceiver {
    UpdateAlarmReceiver alarm = new UpdateAlarmReceiver();
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED"))
        {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            alarm.setAlarm(context, Integer.parseInt(prefs.getString("sync_frequency", "30")));
        }
    }
}
