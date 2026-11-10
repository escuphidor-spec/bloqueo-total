package com.example.bloqueototal

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.util.Calendar

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 100, 60, 60)
        }

        val infoText = TextView(this).apply {
            text = "INSTRUCCIONES DE USO:\n\n" +
                    "• Para BLOQUEAR manualmente:\n  Presiona Vol+, Vol+, Vol-, Vol-\n\n" +
                    "• Para DESBLOQUEAR:\n  Presiona Vol+, Vol-, Vol+, Vol-\n"
            textSize = 16f
            setPadding(0, 0, 0, 40)
        }

        val btnAccessibility = Button(this).apply {
            text = "1. Activar Permiso de Accesibilidad"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        val btnSchedule = Button(this).apply {
            text = "2. Programar Hora de Bloqueo"
            setOnClickListener {
                mostrarSelectorHora()
            }
        }

        val btnCancelSchedule = Button(this).apply {
            text = "Cancelar Horario Programado"
            setOnClickListener {
                cancelarAlarma()
            }
        }

        layout.addView(infoText)
        layout.addView(btnAccessibility)
        layout.addView(btnSchedule)
        layout.addView(btnCancelSchedule)
        setContentView(layout)
    }

    private fun mostrarSelectorHora() {
        val cal = Calendar.getInstance()
        TimePickerDialog(this, { _, hourOfDay, minute ->
            programarAlarma(hourOfDay, minute)
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
    }

    private fun programarAlarma(hour: Int, minute: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, LockAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 1001, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
            }
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )

        Toast.makeText(
            this,
            "Bloqueo programado para las %02d:%02d".format(hour, minute),
            Toast.LENGTH_LONG
        ).show()
    }

    private fun cancelarAlarma() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, LockAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 1001, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Toast.makeText(this, "Horario cancelado", Toast.LENGTH_SHORT).show()
    }
}
