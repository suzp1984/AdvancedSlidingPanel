package io.github.jacobsu.advancedslidingpanel

import android.animation.Animator
import android.animation.ValueAnimator
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import kotlinx.android.synthetic.main.activity_second.*

class SecondActivity : AppCompatActivity() {

    private lateinit var leftPanel: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        val fakeViewWidth = resources.getDimensionPixelSize(R.dimen.fakeViewWidth)

        leftPanel = stubView.inflate()
        leftPanel.translationX = - fakeViewWidth.toFloat()

        startAnimation()
    }

    override fun onDestroy() {
        stopAnimation()

        super.onDestroy()
    }

    private fun startAnimation() {
        val fakeViewWidth = resources.getDimensionPixelSize(R.dimen.fakeViewWidth)

        val animator = ValueAnimator.ofFloat(-fakeViewWidth.toFloat(), 0f, -fakeViewWidth.toFloat())
        animator.addUpdateListener {
            (it.animatedValue as? Float)?.also {
                leftPanel.translationX = it
            }
        }
        animator.interpolator = LinearInterpolator()
        animator.duration = 1000
        animator.repeatMode = ValueAnimator.RESTART
        animator.repeatCount = 10

        leftPanel.tag = animator

        animator.start()
    }

    private fun stopAnimation() {
        (leftPanel.tag as? Animator)?.cancel()
        leftPanel.tag = null
    }
}
