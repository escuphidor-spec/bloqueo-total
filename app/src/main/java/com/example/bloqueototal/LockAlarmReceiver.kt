package com.example.bloqueototal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class LockAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val lockIntent = Intent("ACTION_ACTIVATE_LOCK").apply {
            setPackage(context.packageName)
        }
        context.sendBroadcast(lockIntent)
    }
}
