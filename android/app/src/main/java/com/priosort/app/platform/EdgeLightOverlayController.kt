package com.priosort.app.platform

import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.priosort.app.domain.LightEdge
import com.priosort.app.domain.LightPattern
import com.priosort.app.domain.LightRepeatPolicy
import com.priosort.app.domain.LightRule

/** A short-lived, non-interactive edge overlay. Android/OEM lock-screen policies may hide it. */
class EdgeLightOverlayController(context: Context) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var root: FrameLayout? = null
    private var animator: ValueAnimator? = null
    private var chaseRunnable: Runnable? = null
    private var removeRunnable: Runnable? = null
    private var attached = false
    private var screenReceiverRegistered = false
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) stopOnMainThread()
        }
    }

    fun canDrawOverlay(): Boolean = Settings.canDrawOverlays(appContext)

    fun show(rule: LightRule): Boolean {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { show(rule) }
            return canDrawOverlay()
        }
        stopOnMainThread()
        if (!canDrawOverlay()) return false
        return runCatching {
            val bars = createBars(rule)
            val container = FrameLayout(appContext).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                isClickable = false
                isFocusable = false
                bars.forEach(::addView)
            }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply { gravity = Gravity.TOP or Gravity.START }
            windowManager.addView(container, params)
            root = container
            attached = true
            ContextCompat.registerReceiver(appContext, screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED)
            screenReceiverRegistered = true
            animate(rule, bars)
            removeRunnable = Runnable { stopOnMainThread() }.also {
                mainHandler.postDelayed(it, rule.durationMs.coerceIn(1_000, 20_000))
            }
            true
        }.getOrElse {
            stopOnMainThread()
            false
        }
    }

    fun stop() {
        if (Looper.myLooper() == Looper.getMainLooper()) stopOnMainThread()
        else mainHandler.post { stopOnMainThread() }
    }

    private fun createBars(rule: LightRule): List<View> {
        val density = appContext.resources.displayMetrics.density
        val thickness = (5 * density).toInt().coerceAtLeast(3)
        val selectedEdges = rule.edges.ifEmpty { LightEdge.entries.toSet() }
        return LightEdge.entries.filter { it in selectedEdges }.map { edge ->
            val params = when (edge) {
                LightEdge.TOP -> FrameLayout.LayoutParams(-1, thickness, Gravity.TOP)
                LightEdge.BOTTOM -> FrameLayout.LayoutParams(-1, thickness, Gravity.BOTTOM)
                LightEdge.LEFT -> FrameLayout.LayoutParams(thickness, -1, Gravity.START)
                LightEdge.RIGHT -> FrameLayout.LayoutParams(thickness, -1, Gravity.END)
            }
            View(appContext).apply {
                layoutParams = params
                background = GradientDrawable().apply { setColor(rule.colorArgb) }
                alpha = rule.intensity.coerceIn(0.1f, 1f)
            }
        }
    }

    private fun animate(rule: LightRule, bars: List<View>) {
        if (bars.isEmpty()) return
        val speed = rule.speed.coerceIn(0.25f, 3f)
        when (rule.pattern) {
            LightPattern.STATIC -> Unit
            LightPattern.PULSE, LightPattern.BREATHING -> {
                animator = ValueAnimator.ofFloat(0.12f, rule.intensity.coerceIn(0.1f, 1f)).apply {
                    duration = (if (rule.pattern == LightPattern.BREATHING) 1800L else 700L).div(speed).toLong().coerceAtLeast(150L)
                    repeatCount = repeats(rule.repeatPolicy)
                    repeatMode = ValueAnimator.REVERSE
                    addUpdateListener { value -> bars.forEach { it.alpha = value.animatedValue as Float } }
                    start()
                }
            }
            LightPattern.DOUBLE_FLASH -> {
                animator = ValueAnimator.ofFloat(0.08f, rule.intensity, 0.08f, rule.intensity, 0.08f).apply {
                    duration = (900L / speed).toLong().coerceAtLeast(200L)
                    repeatCount = repeats(rule.repeatPolicy)
                    addUpdateListener { value -> bars.forEach { it.alpha = value.animatedValue as Float } }
                    start()
                }
            }
            LightPattern.CHASE, LightPattern.WAVE_RAINBOW -> {
                bars.forEach { it.alpha = if (rule.pattern == LightPattern.CHASE) 0.12f else rule.intensity }
                var index = 0
                var completedCycles = 0
                val colors = intArrayOf(0xFFFF4F9A.toInt(), 0xFFFFB347.toInt(), 0xFFFFFF66.toInt(), 0xFF66FF99.toInt(), 0xFF66CCFF.toInt(), 0xFFCC99FF.toInt())
                val pulse = object : Runnable {
                    override fun run() {
                        if (!attached || bars.isEmpty()) return
                        if (rule.pattern == LightPattern.CHASE) {
                            bars.forEachIndexed { i, view -> view.alpha = if (i == index % bars.size) rule.intensity else 0.12f }
                        } else {
                            bars.forEachIndexed { i, view ->
                                (view.background as? GradientDrawable)?.setColor(colors[(index + i) % colors.size])
                                view.alpha = rule.intensity
                            }
                        }
                        index++
                        if (index % bars.size == 0) {
                            completedCycles++
                            if (rule.repeatPolicy != LightRepeatPolicy.UNTIL_TIMEOUT && completedCycles >= repeatCycles(rule.repeatPolicy)) return
                        }
                        mainHandler.postDelayed(this, (220L / speed).toLong().coerceAtLeast(70L))
                    }
                }
                chaseRunnable = pulse
                mainHandler.post(pulse)
            }
        }
    }

    private fun stopOnMainThread() {
        removeRunnable?.let(mainHandler::removeCallbacks)
        removeRunnable = null
        chaseRunnable?.let(mainHandler::removeCallbacks)
        chaseRunnable = null
        animator?.cancel()
        animator = null
        if (screenReceiverRegistered) {
            runCatching { appContext.unregisterReceiver(screenOffReceiver) }
            screenReceiverRegistered = false
        }
        root?.let { view ->
            if (attached) runCatching { windowManager.removeView(view) }
        }
        root = null
        attached = false
    }

    private fun repeats(policy: LightRepeatPolicy): Int = when (policy) {
        LightRepeatPolicy.ONCE -> 0
        LightRepeatPolicy.TWICE -> 1
        LightRepeatPolicy.UNTIL_TIMEOUT -> ValueAnimator.INFINITE
    }

    private fun repeatCycles(policy: LightRepeatPolicy): Int = when (policy) {
        LightRepeatPolicy.ONCE -> 1
        LightRepeatPolicy.TWICE -> 2
        LightRepeatPolicy.UNTIL_TIMEOUT -> Int.MAX_VALUE
    }
}
