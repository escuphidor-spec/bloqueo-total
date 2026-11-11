package com.example.bloqueototal

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class TouchBlockerService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isLocked = false

    // Combinación para BLOQUEAR: Vol+, Vol+, Vol-, Vol-
    private val lockCombo = listOf(
        KeyEvent.KEYCODE_VOLUME_UP,
        KeyEvent.KEYCODE_VOLUME_UP,
        KeyEvent.KEYCODE_VOLUME_DOWN,
        KeyEvent.KEYCODE_VOLUME_DOWN
    )

    // Combinación para DESBLOQUEAR: Vol+, Vol-, Vol+, Vol-
    private val unlockCombo = listOf(
        KeyEvent.KEYCODE_VOLUME_UP,
        KeyEvent.KEYCODE_VOLUME_DOWN,
        KeyEvent.KEYCODE_VOLUME_UP,
        KeyEvent.KEYCODE_VOLUME_DOWN
    )

    private val inputBuffer = mutableListOf<Int>()
    private var lastKeyTime = 0L
    private val COMBO_TIMEOUT_MS = 3000L

    private val systemReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "ACTION_ACTIVATE_LOCK" -> {
                    vibrateShort()
                    enableLockOverlay()
                }
                "ACTION_DEACTIVATE_LOCK" -> {
                    vibrateShort()
                    disableLockOverlay()
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val filter = IntentFilter().apply {
            addAction("ACTION_ACTIVATE_LOCK")
            addAction("ACTION_DEACTIVATE_LOCK")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(systemReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(systemReceiver, filter)
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        if (keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
            return super.onKeyEvent(event)
        }

        if (event.action == KeyEvent.ACTION_DOWN) {
            val now = System.currentTimeMillis()
            if (now - lastKeyTime > COMBO_TIMEOUT_MS) {
                inputBuffer.clear()
            }
            lastKeyTime = now
            inputBuffer.add(keyCode)

            if (inputBuffer.size > 4) {
                inputBuffer.removeAt(0)
            }

            if (!isLocked && inputBuffer == lockCombo) {
                inputBuffer.clear()
                vibrateShort()
                enableLockOverlay()
                return true
            } else if (isLocked && inputBuffer == unlockCombo) {
                inputBuffer.clear()
                vibrateShort()
                disableLockOverlay()
                return true
            }
        }

        // Mientras está bloqueado, bloqueamos también el cambio de volumen
        if (isLocked) {
            return true
        }
        return super.onKeyEvent(event)
    }

    private fun enableLockOverlay() {
        if (isLocked || overlayView != null) return

        overlayView = View(this).apply {
            // Capa 100% transparente: permite ver la pantalla y el estado de carga
            setBackgroundColor(Color.TRANSPARENT)
        }

        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager?.defaultDisplay?.getRealMetrics(metrics)

        @Suppress("DEPRECATION")
        val params = WindowManager.LayoutParams(
            metrics.widthPixels,
            metrics.heightPixels + 300,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        windowManager?.addView(overlayView, params)
        isLocked = true
        Toast.makeText(this, "Pantalla Bloqueada", Toast.LENGTH_SHORT).show()
    }

    private fun disableLockOverlay() {
        if (!isLocked) return
        overlayView?.let {
            windowManager?.removeView(it)
            overlayView = null
        }
        isLocked = false
        Toast.makeText(this, "Pantalla Desbloqueada", Toast.LENGTH_SHORT).show()
    }

    private fun vibrateShort() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(150)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isLocked || event == null) return

        val pkg = event.packageName?.toString() ?: ""
        val cls = event.className?.toString() ?: ""

        // Evita que abran la cámara con doble clic o el menú de apagado con el botón lateral
        if (pkg.contains("camera") || cls.contains("GlobalActions")) {
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(systemReceiver)
    }
}
