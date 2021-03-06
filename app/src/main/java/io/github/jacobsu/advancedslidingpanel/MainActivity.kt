package io.github.jacobsu.advancedslidingpanel

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.drawable.LayerDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
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


        val arrowWidth = resources.getDimensionPixelSize(R.dimen.arrowViewHeight) / 2

        val layerDrawable = LayerDrawable(arrayOf(ArrowDrawable(), ArrowDrawable()))
        layerDrawable.setLayerInset(0, 0, 0, arrowWidth * 2, 0)
        layerDrawable.setLayerInset(1, arrowWidth * 2, 0, 0, 0)
        layerDrawable.getDrawable(0).alpha = 100

        arrowView.background = layerDrawable

        hintBar.setOnClickListener {
            val intent = Intent(this, SecondActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        startAnimation()
        startArrowAnimation()
    }

    override fun onPause() {
        stopArrowAnimation()
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

    private fun startArrowAnimation() {
        val arrowWidth = resources.getDimensionPixelSize(R.dimen.arrowViewHeight) / 2

        val layerDrawable = arrowView.background as LayerDrawable

        val alphaAnimator = ValueAnimator.ofInt(0, 255)
        alphaAnimator.interpolator = LinearInterpolator()
        alphaAnimator.repeatMode = ValueAnimator.RESTART
        alphaAnimator.repeatCount = ValueAnimator.INFINITE
        alphaAnimator.duration = 900
        alphaAnimator.addUpdateListener {
            (it?.animatedValue as? Int)?.also {
                layerDrawable.getDrawable(0).alpha = it
                layerDrawable.getDrawable(1).alpha = 255 - it
                layerDrawable.invalidateSelf()
            }
        }

        val translationAnimator = ValueAnimator.ofInt(0, arrowWidth * 2)
        translationAnimator.interpolator = LinearInterpolator()
        translationAnimator.repeatMode = ValueAnimator.RESTART
        translationAnimator.repeatCount = ValueAnimator.INFINITE
        translationAnimator.duration = 900
        translationAnimator.addUpdateListener {
            (it?.animatedValue as? Int)?.also {
                layerDrawable.setLayerInset(0, it, 0, arrowWidth * 2 - it, 0)
                layerDrawable.setLayerInset(1, arrowWidth * 2  - it, 0, it, 0)
                layerDrawable.invalidateSelf()
            }
        }

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(alphaAnimator, translationAnimator)
        arrowView.tag = animatorSet

        animatorSet.start()
    }

    private fun stopArrowAnimation() {
        arrowView.tag?.also {
            (it as? Animator)?.cancel()
        }

        arrowView.tag = null
    }
}
