package com.talat.soundtone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class AlarmRingReceiver extends BroadcastReceiver {

    private static final String TAG= "SOUNDTONE-Receiver";

    public AlarmRingReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        if (action.equals("com.android.deskclock.ALARM_ALERT") ||
                action.equals("com.motorola.blur.alarmclock.AlarmAlert")||
                action.equals("com.motorola.blur.alarmclock.AlarmClock")||
                action.equals("com.motorola.blur.alarmclock.AlarmTimerAlert")||
                action.equals("com.android.alarmclock.AlarmClock")||
                action.equals("com.android.alarmclock.ALARM_ALERT")||
                action.equals("com.android.deskclock.DeskClock")||
                action.equals("com.motorola.blur.alarmclock.AlarmAlert")||
                action.equals("com.samsung.sec.android.clockpackage.alarm.ALARM_ALERT")||
                action.equals("com.lge.alarm.alarmclocknew"))
        {
            Log.i(TAG,"The Alarm - is Ringing, action - " + action);
            SharedPreferences sharedPreferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
            int id = sharedPreferences.getInt(MainActivity.CHOSEN_PLAYLIST,-1);

            Log.i(TAG,"Starting Service with id = " +id);
            AlarmSelectorService.startActionNextAlarm(context,id);
        }
    }
}
