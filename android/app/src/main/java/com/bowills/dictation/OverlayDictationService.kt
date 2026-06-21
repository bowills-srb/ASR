package com.bowills.dictation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Foreground service that shows a draggable floating mic button over other apps.
 *
 * Tap to start recording, tap again to stop + transcribe. Until the accessibility
 * text injector (slice 5) lands, the cleaned text is copied to the clipboard and
 * surfaced via a toast — enough to demo the system-wide trigger end to end.
 *
 * Requires the "display over other apps" permission (SYSTEM_ALERT_WINDOW) and
 * RECORD_AUDIO, both gated in MainActivity before the service is started.
 */
class OverlayDictationService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val engine = DictationEngine()

    private lateinit var windowManager: WindowManager
    private var bubble: Button? = null

    private enum class State { IDLE, RECORDING, SENDING }
    private var state = State.IDLE

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startAsForeground()
        addBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun startAsForeground() {
        val channelId = "dictation_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            if (mgr.getNotificationChannel(channelId) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(
                        channelId,
                        "Dictation",
                        NotificationManager.IMPORTANCE_LOW,
                    ),
                )
            }
        }
        val notification: Notification = Notification.Builder(this, channelId)
            .setContentTitle("VoiceText")
            .setContentText("Tap the floating mic to dictate")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                0
            },
        )
    }

    private fun addBubble() {
        val button = Button(this).apply {
            text = "🎤"
            setBackgroundColor(Color.parseColor("#3949AB"))
            setTextColor(Color.WHITE)
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 240
        }
        attachDragAndTap(button, params)
        windowManager.addView(button, params)
        bubble = button
    }

    /** Distinguishes a drag (move the bubble) from a tap (toggle recording). */
    private fun attachDragAndTap(view: View, params: WindowManager.LayoutParams) {
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var dragging = false
        val touchSlop = 12

        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = params.x
                    startY = params.y
                    dragging = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downX).toInt()
                    val dy = (event.rawY - downY).toInt()
                    if (abs(dx) > touchSlop || abs(dy) > touchSlop) dragging = true
                    if (dragging) {
                        params.x = startX + dx
                        params.y = startY + dy
                        windowManager.updateViewLayout(view, params)
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (!dragging) v.performClick()
                    true
                }

                else -> false
            }
        }
        view.setOnClickListener { onBubbleTapped() }
    }

    private fun onBubbleTapped() {
        when (state) {
            State.IDLE -> startRecording()
            State.RECORDING -> stopAndSend()
            State.SENDING -> Unit // ignore taps mid-request
        }
    }

    private fun startRecording() {
        runCatching { engine.start() }
            .onSuccess {
                state = State.RECORDING
                bubble?.text = "⏹"
            }
            .onFailure { toast("Mic error: ${it.message}") }
    }

    private fun stopAndSend() {
        state = State.SENDING
        bubble?.text = "…"
        scope.launch {
            runCatching { engine.stopAndSend() }
                .onSuccess { copyToClipboard(it.text); toast("Copied: ${it.text.take(60)}") }
                .onFailure { toast("Failed: ${it.message}") }
            state = State.IDLE
            bubble?.text = "🎤"
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("dictation", text))
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        bubble?.let { runCatching { windowManager.removeView(it) } }
        bubble = null
    }

    companion object {
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, OverlayDictationService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OverlayDictationService::class.java))
        }
    }
}
