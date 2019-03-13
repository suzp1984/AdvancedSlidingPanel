package io.github.jacobsu.advancedslidingpanel.widget

import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt

class ArrowDrawable : Drawable() {
    private val strokePaint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val path : Path
    private val defaultStrokeWidth = 10f
    @ColorInt
    private val defaultStrokeColor: Int = Color.rgb(255, 255, 255)

    init {
        strokePaint.style = Paint.Style.STROKE
        strokePaint.color = defaultStrokeColor
        strokePaint.isDither = true
        strokePaint.strokeWidth = defaultStrokeWidth
        strokePaint.strokeCap = Paint.Cap.ROUND
        strokePaint.strokeJoin = Paint.Join.ROUND

        path = Path()
    }

    override fun draw(canvas: Canvas) {
    }

    override fun setAlpha(alpha: Int) {
        strokePaint.alpha = alpha
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun setColorFilter(cf: ColorFilter?) {
        cf?.also {
            strokePaint.colorFilter = it
        }
    }
}