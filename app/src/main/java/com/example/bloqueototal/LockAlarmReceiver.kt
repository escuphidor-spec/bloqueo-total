package com.example.bloqueototal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class LockAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val actionType = intent?.getStringExtra("ACTION_TYPE")

        val targetAction = if (actionType == "UNLOCK") {
            "ACTION_DEACTIVATE_LOCK"
        } else {
            "ACTION_ACTIVATE_LOCK"
        }

        val broadcastIntent = Intent(targetAction).apply {
            setPackage(context.packageName)
        }
        context.sendBroadcast(broadcastIntent)
    }
}
