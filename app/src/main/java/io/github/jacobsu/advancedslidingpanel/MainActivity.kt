package io.github.jacobsu.advancedslidingpanel

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import io.github.jacobsu.advancedslidingpanel.widget.ArrowDrawable
import io.github.jacobsu.advancedslidingpanel.widget.IVerticalScrollableView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportFragmentManager.findFragmentById(R.id.itemFragment)?.let {
            if (it is IVerticalScrollableView) {
                slidingUpLayout.verticalScrollableView = it
            }
        }

        arrowView.background = ArrowDrawable()
    }

    override fun onResume() {
        super.onResume()

        startAnimation()
    }

    override fun onPause() {
        stopAnimation()

        super.onPause()
    }

    private fun startAnimation() {
        val bounceHeight = resources.getDimensionPixelSize(R.dimen.hintBarHeight)

        val firstAnims = ValueAnimator.ofFloat(0f, (-bounceHeight * 3).toFloat())
        firstAnims.interpolator = DecelerateInterpolator()
        firstAnims.repeatMode = ValueAnimator.REVERSE
        firstAnims.repeatCount = 1
        firstAnims.duration = 400
        firstAnims.addUpdateListener {
            (it?.animatedValue as? Float)?.also { hight ->
                slidingPanel.translationY = hight
            }
        }

        val secondAnims = ValueAnimator.ofFloat(0f, -(bounceHeight * 2).toFloat())
        secondAnims.interpolator = DecelerateInterpolator()
        secondAnims.duration = 200
        secondAnims.repeatCount = 0
        secondAnims.addUpdateListener {
            (it?.animatedValue as? Float)?.also { hight ->
                slidingPanel.translationY = hight
            }
        }

        val bounceAnims = ValueAnimator.ofFloat(-(bounceHeight * 2).toFloat(), 0f)
        bounceAnims.interpolator = BounceInterpolator()
        bounceAnims.duration = 600
        bounceAnims.repeatCount = 0
        bounceAnims.addUpdateListener {
            (it?.animatedValue as? Float)?.also { hight ->
                slidingPanel.translationY = hight
            }
        }

        val animatorSet = AnimatorSet()
        animatorSet.startDelay = 600
        animatorSet.playSequentially(firstAnims, secondAnims, bounceAnims)
        animatorSet.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {
            }

            override fun onAnimationEnd(animation: Animator?) {
            }

            override fun onAnimationCancel(animation: Animator?) {
                slidingPanel.translationY = 0.0f
            }

            override fun onAnimationStart(animation: Animator?) {
            }

        })

        slidingUpLayout.tag = animatorSet
        animatorSet.start()
    }

    private fun stopAnimation() {
        slidingUpLayout.tag?.also {
            (it as? Animator)?.cancel()
        }

        slidingUpLayout.tag = null
    }
}
