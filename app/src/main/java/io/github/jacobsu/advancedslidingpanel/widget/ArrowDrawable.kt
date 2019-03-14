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

    var strokeWidth: Float = defaultStrokeWidth
        set(value) {
            if (field != value) {
                field = value

                strokePaint.strokeWidth = value
                invalidateSelf()
            }
        }

    @ColorInt
    var strokeColor: Int = defaultStrokeColor
        set(value) {
            if (field != value) {
                field = value

                strokePaint.color = value
                invalidateSelf()
            }
        }

    override fun draw(canvas: Canvas) {
        canvas.drawPath(path, strokePaint)
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

    override fun onBoundsChange(bounds: Rect?) {
        resetArrowPath()
    }

    private fun resetArrowPath() {
        path.reset()

        val boundsWithStroke = Rect(bounds).apply {
            inset((strokeWidth / 2).toInt(), (strokeWidth / 2).toInt())
        }

        boundsWithStroke.apply {
            path.moveTo(left.toFloat(), top.toFloat())
            path.lineTo(right.toFloat(), (bottom / 2 + top / 2).toFloat())
            path.lineTo(left.toFloat(), bottom.toFloat())
        }
    }
}