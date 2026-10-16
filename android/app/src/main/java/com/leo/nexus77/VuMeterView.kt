package com.leo.nexus77

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.sin

/**
 * Medidor estilo VU para o cabeçalho: 10 cubos com trilho esmaecido, preenchimento
 * em degradê e "cap" aceso no topo, animados por uma onda suave (zigue-zague).
 */
class VuMeterView(context: Context) : View(context) {

    private val barCount = 10
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val capPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    private var strong = 0xFF00E5FF.toInt()
    private var dim = 0x3300E5FF
    private var trackColor = 0x1AFFFFFF
    private var phase = 0f
    private var running = false

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1400L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            phase = it.animatedFraction
            invalidate()
        }
    }

    /** Cor forte (topo do preenchimento), cor fraca (base) e a cor do trilho de fundo. */
    fun setColors(strongColor: Int, dimColor: Int, trackCol: Int) {
        strong = strongColor
        dim = dimColor
        trackColor = trackCol
        invalidate()
    }

    fun start() {
        visibility = VISIBLE
        if (running) return
        running = true
        if (animator.isStarted) animator.resume() else animator.start()
    }

    fun stop() {
        running = false
        if (animator.isStarted) animator.pause()
        visibility = GONE
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val gap = w * 0.06f
        val barW = (w - gap * (barCount - 1)) / barCount
        val radius = barW * 0.42f
        val p = phase * 2.0 * Math.PI
        trackPaint.color = trackColor

        for (i in 0 until barCount) {
            val left = i * (barW + gap)
            val right = left + barW

            // trilho de fundo (mostra o "caminho" da barra)
            rect.set(left, 0f, right, h)
            canvas.drawRoundRect(rect, radius, radius, trackPaint)

            // onda suave (soma de senos) + defasagem por barra -> zigue-zague orgânico
            val a = sin(p + i * 0.55)
            val b = sin(p * 1.7 + i * 0.9)
            val wave = ((0.55 * a + 0.45 * b) * 0.5 + 0.5).coerceIn(0.0, 1.0)
            val eased = wave * wave * (3.0 - 2.0 * wave)   // smoothstep
            val level = (0.16 + 0.84 * eased).toFloat()

            val barH = h * level
            val top = h - barH

            // preenchimento em degradê: base fraca -> topo forte
            fillPaint.shader = LinearGradient(left, h, left, top, dim, strong, Shader.TileMode.CLAMP)
            rect.set(left, top, right, h)
            canvas.drawRoundRect(rect, radius, radius, fillPaint)

            // "cap" aceso no topo da barra
            capPaint.color = strong
            val capH = (barW * 0.5f).coerceAtLeast(2f)
            rect.set(left, top, right, (top + capH).coerceAtMost(h))
            canvas.drawRoundRect(rect, radius, radius, capPaint)
        }
    }
}
