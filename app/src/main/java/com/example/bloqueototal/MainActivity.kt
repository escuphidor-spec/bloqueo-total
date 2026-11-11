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

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 80, 60, 60)
        }

        val infoText = TextView(this).apply {
            text = "MODO MANUAL (Botones de Volumen):\n" +
                    "• Bloquear: Vol+, Vol+, Vol-, Vol-\n" +
                    "• Desbloquear: Vol+, Vol-, Vol+, Vol-\n"
            textSize = 15f
            setPadding(0, 0, 0, 30)
        }

        statusText = TextView(this).apply {
            textSize = 15f
            setPadding(0, 0, 0, 40)
        }
        actualizarTextoHorarios()

        val btnAccessibility = Button(this).apply {
            text = "1. Activar Permiso de Accesibilidad"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        val btnScheduleLock = Button(this).apply {
            text = "2. Programar Hora de BLOQUEO"
            setOnClickListener {
                mostrarSelectorHora(isUnlock = false)
            }
        }

        val btnScheduleUnlock = Button(this).apply {
            text = "3. Programar Hora de DESBLOQUEO"
            setOnClickListener {
                mostrarSelectorHora(isUnlock = true)
            }
        }

        val btnCancelSchedules = Button(this).apply {
            text = "4. Cancelar Bloqueo y Desbloqueo Automático"
            setOnClickListener {
                cancelarAlarmas()
            }
        }

        layout.addView(infoText)
        layout.addView(statusText)
        layout.addView(btnAccessibility)
        layout.addView(btnScheduleLock)
        layout.addView(btnScheduleUnlock)
        layout.addView(btnCancelSchedules)
        setContentView(layout)
    }

    private fun mostrarSelectorHora(isUnlock: Boolean) {
        val cal = Calendar.getInstance()
        TimePickerDialog(this, { _, hourOfDay, minute ->
            programarAlarma(hourOfDay, minute, isUnlock)
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
    }

    private fun programarAlarma(hour: Int, minute: Int, isUnlock: Boolean) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, LockAlarmReceiver::class.java).apply {
            putExtra("ACTION_TYPE", if (isUnlock) "UNLOCK" else "LOCK")
        }

        val requestCode = if (isUnlock) 2002 else 1001
        val pendingIntent = PendingIntent.getBroadcast(
            this, requestCode, intent,
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

        val horaFormateada = "%02d:%02d".format(hour, minute)
        val prefs = getSharedPreferences("horarios_prefs", Context.MODE_PRIVATE)
        if (isUnlock) {
            prefs.edit().putString("hora_desbloqueo", horaFormateada).apply()
            Toast.makeText(this, "Desbloqueo programado a las $horaFormateada", Toast.LENGTH_LONG).show()
        } else {
            prefs.edit().putString("hora_bloqueo", horaFormateada).apply()
            Toast.makeText(this, "Bloqueo programado a las $horaFormateada", Toast.LENGTH_LONG).show()
        }
        actualizarTextoHorarios()
    }

    private fun cancelarAlarmas() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intentLock = Intent(this, LockAlarmReceiver::class.java).putExtra("ACTION_TYPE", "LOCK")
        val piLock = PendingIntent.getBroadcast(
            this, 1001, intentLock,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(piLock)

        val intentUnlock = Intent(this, LockAlarmReceiver::class.java).putExtra("ACTION_TYPE", "UNLOCK")
        val piUnlock = PendingIntent.getBroadcast(
            this, 2002, intentUnlock,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(piUnlock)

        val prefs = getSharedPreferences("horarios_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        actualizarTextoHorarios()

        Toast.makeText(this, "Horarios automáticos cancelados", Toast.LENGTH_SHORT).show()
    }

    private fun actualizarTextoHorarios() {
        val prefs = getSharedPreferences("horarios_prefs", Context.MODE_PRIVATE)
        val lockTime = prefs.getString("hora_bloqueo", "Sin programar")
        val unlockTime = prefs.getString("hora_desbloqueo", "Sin programar")
        statusText.text = "HORARIOS ACTIVOS:\n" +
                "• Hora de Bloqueo: $lockTime\n" +
                "• Hora de Desbloqueo: $unlockTime"
    }
}
