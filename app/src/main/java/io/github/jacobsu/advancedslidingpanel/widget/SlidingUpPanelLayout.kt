package io.github.jacobsu.advancedslidingpanel.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.customview.widget.ViewDragHelper
import io.github.jacobsu.advancedslidingpanel.R
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class SlidingUpPanelLayout(context: Context, attrs: AttributeSet?, defStyle: Int) :
    ViewGroup(context, attrs, defStyle) {

    private val logTag = "SlidingUpPanel"

    private var mainView : View? = null
    private var slideableView : View? = null
    private lateinit var viewDragHelper : ViewDragHelper
    private var isScrollableViewHandlingTouch : Boolean = false
    private var isUnableToDrag : Boolean = false
    private var initMotionX : Float = 0f
    private var initMotionY : Float = 0f
    private var preMotionX : Float = 0f
    private var preMotionY : Float = 0f
    private var internalSlideableRange : Int = 0
    private val defaultMinFlingVelocity = 500
    private var minFlingVelocity = defaultMinFlingVelocity

    private var slidingPanelListeners : Set<SlidingPanelListener>  = hashSetOf()
    private var dragableViewId : Int = -1
    private var toolbarHeight : Int = 0
    private var canHandleDraggingEventOnHideableBar : Boolean = false
    private var scrollableViewId : Int = -1
    private var hideOnExpandedViewId : Int = -1
    private var slidingMethod : SlidingMethod = SlidingMethod.All_SLIDEABLE_VIEW

    private var currentObsoluteAnchor: Int? = null

    // In a range of [0.0f, 1.0f], where 0 == collapsed, 1 == expanded.
    private var internalRelativeSlideOffset = 0f
        set(value) {
            if (field == value) {
                return
            }

            field = when {
                value < 0.01f -> 0.0f
                value > 0.99f -> 1.0f
                else -> value
            }

            currentObsoluteAnchor = absoluteAnchors.getOrNull(
                absoluteAnchorsInRelativeMap.indexOf(field))

            notifySlidingOffsetChanged(field)

            hideOnExpandedView?.visibility = if (field == 1.0f) {
                View.INVISIBLE
            } else {
                View.VISIBLE
            }
        }

    private var absoluteAnchorsInRelativeMap : List<Float> = listOf()

    private var allAnchorsInRelative : List<Float> = listOf()

    private var internalPanelState : PanelState = PanelState.IDLE
        set(value) {
            if (field != value) {
                notifySlidingPanelStateChanged(field, value)
                field = value
            }
        }

    private val viewDragCallBack : ViewDragHelper.Callback = object : ViewDragHelper.Callback() {
        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            return !isUnableToDrag && child == slideableView
        }

        override fun onViewDragStateChanged(state: Int) {
            when (state) {
                ViewDragHelper.STATE_IDLE -> {
                    internalPanelState = when (internalRelativeSlideOffset) {
                        0.0f -> PanelState.COLLAPSED
                        1.0f -> PanelState.EXPANDED
                        else -> {
                            PanelState.ANCHORED
                        }
                    }
                }
                ViewDragHelper.STATE_SETTLING -> {
                    PanelState.SETTLING
                }
                ViewDragHelper.STATE_DRAGGING -> {
                    PanelState.DRAGGING
                }
            }
        }

        override fun onViewCaptured(capturedChild: View, activePointerId: Int) {
        }

        override fun onViewPositionChanged(changedView: View,
                                           left: Int, top: Int, dx: Int, dy: Int) {

            onSlidingPanelDragged(top)
            invalidate()
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {

            val offset = calculateSlidingPanelRelativeOffset(releasedChild.top)

            val top = calculateSlidingPanelTopPosition(when {
                yvel < 0 ->
                    allAnchorsInRelative.nextAnchor(offset)
                yvel > 0 ->
                    allAnchorsInRelative.previousAnchor(offset)
                else -> allAnchorsInRelative.nearbyAnchor(offset)
            })

            viewDragHelper.settleCapturedViewAt(releasedChild.left, top)
            invalidate()
        }

        override fun getViewVerticalDragRange(child: View): Int {
            return internalSlideableRange
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            val collapsedTop = calculateSlidingPanelTopPosition(0.0f)
            val expandedTop = calculateSlidingPanelTopPosition(1.0f)

            return min(max(top, expandedTop), collapsedTop)
        }

    }

    val panelState: PanelState
        get() = internalPanelState

    val relativeSlideOffset : Float
        get() = internalRelativeSlideOffset

    val slideableRange : Int
        get() = internalSlideableRange

    val currentSlidedRange : Int
        get() = (internalSlideableRange.toFloat() * internalRelativeSlideOffset).roundToInt()

    var isPanelLocked: Boolean = false
        set(value) {
            if (value != field) {
                field = value
            }
        }

    val hasAnchors: Boolean
        get() = allAnchorsInRelative.isEmpty().not()

    var relativeAnchors : List<Float> = listOf()
        set(value) {
            field = value.asSequence().sorted().filter {
                it > 0.0f && it < 1.0f
            }.distinct().toList()

            generatorAllAnchorsInRelative()
        }

    var absoluteAnchors : List<Int> = listOf()
        set(value) {
            field = value.asSequence().sorted().filter {
                it in 1..(internalSlideableRange - 1)
            }.distinct().toList()

            if (currentObsoluteAnchor?.let { field.contains(it) } == false) {
                currentObsoluteAnchor = null
            }

            generatorAllAnchorsInRelative()
        }

    var visibleBarHeight : Int = 0
        set(value) {
            field = value
            requestLayout()
        }

    var hideableBarHeight : Int = 0
    var visibleContentHeight : Int = 0
        set(value) {
            if (field != value) {
                field = value

                requestLayout()
            }
        }

    var isDragableBarBelowToolBar : Boolean = true

    var dragableView : View? = null
    var scrollableView : View? = null
    var hideOnExpandedView : View? = null

    var verticalScrollableView : IVerticalScrollableView? = null

    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SlidingUpPanelLayout)

        visibleBarHeight = typedArray?.getDimensionPixelSize(
            R.styleable.SlidingUpPanelLayout_visibleBarHeight, 0) ?: 0
        hideableBarHeight = typedArray?.getDimensionPixelSize(
            R.styleable.SlidingUpPanelLayout_hideableBarHeight, 0) ?: 0
        visibleContentHeight = typedArray?.getDimensionPixelSize(
            R.styleable.SlidingUpPanelLayout_visibleContentHeight, 0) ?: 0
        minFlingVelocity = typedArray?.getInt(
            R.styleable.SlidingUpPanelLayout_minFlingVelocity, defaultMinFlingVelocity) ?: defaultMinFlingVelocity
        dragableViewId = typedArray?.getResourceId(
            R.styleable.SlidingUpPanelLayout_dragableView, -1) ?: -1
        scrollableViewId = typedArray?.getResourceId(
            R.styleable.SlidingUpPanelLayout_scrollableView, -1) ?: -1
        hideOnExpandedViewId = typedArray?.getResourceId(
            R.styleable.SlidingUpPanelLayout_hideOnExpandedView, -1) ?: -1
        isDragableBarBelowToolBar = typedArray?.getBoolean(
            R.styleable.SlidingUpPanelLayout_isDragableBarBelowToolBar, true) ?: true
        toolbarHeight = typedArray?.getDimensionPixelSize(
            R.styleable.SlidingUpPanelLayout_toolBarHeight, 0) ?: 0
        canHandleDraggingEventOnHideableBar = typedArray?.getBoolean(
            R.styleable.SlidingUpPanelLayout_canHandleDraggingEventOnHideableBar, false) ?: false

        slidingMethod = SlidingMethod.fromInt(
            typedArray?.getInt(R.styleable.SlidingUpPanelLayout_slidingMethod,
                SlidingMethod.All_SLIDEABLE_VIEW.value)) ?: SlidingMethod.All_SLIDEABLE_VIEW

        typedArray?.recycle()

        viewDragHelper = ViewDragHelper.create(this, 0.5f, viewDragCallBack)
        viewDragHelper.minVelocity = minFlingVelocity.toFloat() * resources.displayMetrics.density
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        if (widthMode != MeasureSpec.EXACTLY && widthMode != MeasureSpec.AT_MOST) {
            throw IllegalArgumentException("Width must have an exact value or MATCH_PARENT")
        }

        if (heightMode != MeasureSpec.EXACTLY && heightMode != MeasureSpec.AT_MOST) {
            throw IllegalArgumentException("Height must have an exact value or MATCH_PARENT")
        }

        if (childCount != 2) {
            throw IllegalArgumentException("Sliding Up layout must have exactly 2 children")
        }

        val layoutHeight = heightSize - paddingTop - paddingBottom
        val layoutWidth = widthSize - paddingLeft - paddingRight

        childViews.forEach {
            val layoutParams = it.layoutParams as? LayoutParams
            layoutParams?.apply {
                width = layoutWidth - leftMargin - rightMargin
                height = layoutHeight - topMargin - bottomMargin + if (it == slideableView) {
                    hideableBarHeight
                } else {
                    0
                } - if (it == slideableView && isDragableBarBelowToolBar) {
                    toolbarHeight
                } else {
                    0
                }

                it.measure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
            }
        }

        internalSlideableRange = layoutHeight - visibleBarHeight -
                visibleContentHeight - if (isDragableBarBelowToolBar) {
            toolbarHeight
        } else {
            0
        }

        setMeasuredDimension(widthSize, heightSize)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {

        val relativeOffset = if (changed) {
            generatorAllAnchorsInRelative()

            currentObsoluteAnchor?.let {
                absoluteAnchorsInRelativeMap.getOrNull(absoluteAnchors.indexOf(it)) ?: 0f
            } ?: allAnchorsInRelative.nearbyAnchor(internalRelativeSlideOffset)
        } else {
            internalRelativeSlideOffset
        }

        internalRelativeSlideOffset = relativeOffset

        childViews.forEach {

            val layoutParams = it.layoutParams as? LayoutParams

            val left = paddingLeft + (layoutParams?.leftMargin ?: 0)
            val top = if (it == mainView) {
                paddingTop + (layoutParams?.topMargin ?: 0)
            } else {
                paddingTop + (layoutParams?.topMargin ?: 0) +
                        calculateSlidingPanelTopPosition(relativeOffset)
            }

            it.layout(left, top, left + it.measuredWidth, top + it.measuredHeight)
        }

    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        dragableView = if (dragableViewId != -1) {
            findViewById(dragableViewId)
        } else {
            null
        }

        scrollableView = if (scrollableViewId != -1) {
            findViewById(scrollableViewId)
        } else {
            null
        }

        verticalScrollableView = verticalScrollableView ?: scrollableView?.let {
            when (it) {
                is IVerticalScrollableView -> it
                else -> SimpleVerticalScrollableView(it)
            }
        }

        hideOnExpandedView = if (hideOnExpandedViewId != -1) {
            findViewById(hideOnExpandedViewId)
        } else {
            null
        }

        mainView = getChildAt(0)
        slideableView = getChildAt(1)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {

        if (isPanelLocked || !isEnabled ||
            (isUnableToDrag && ev?.action != MotionEvent.ACTION_DOWN)) {
            viewDragHelper.abort()
            return super.dispatchTouchEvent(ev)
        }

        when (ev?.action) {
            MotionEvent.ACTION_DOWN -> {
                isScrollableViewHandlingTouch = false
                preMotionX = ev.x
                preMotionY = ev.y
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.x - preMotionX
                val dy = ev.y - preMotionY
                preMotionX = ev.x
                preMotionY = ev.y

                if (dx.absoluteValue > dy.absoluteValue) {
                    return super.dispatchTouchEvent(ev)
                }

                when (slidingMethod) {
                    SlidingMethod.All_SLIDEABLE_VIEW -> {
                        verticalScrollableView?.also {
                            if (!isViewUnder(it, initMotionX.toInt(), initMotionY.toInt())) {
                                return super.dispatchTouchEvent(ev)
                            }

                            if (internalPanelState == PanelState.EXPANDED) {
                                if (dy > 0) {
                                    if (it.isAtBottomPosition || it.isAtMiddlePosition) {
                                        isScrollableViewHandlingTouch = true
                                        return super.dispatchTouchEvent(ev)
                                    }

                                    isScrollableViewHandlingTouch = false
                                    return onTouchEvent(ev)
                                }
                            }
                        }

                    }
                    SlidingMethod.WHEN_SCROLLABLE_VIEW_NO_LONGER_CONSUME_EVENT -> {
                        verticalScrollableView?.also {
                            if (!isViewUnder(it, initMotionX.toInt(), initMotionY.toInt())) {
                                return super.dispatchTouchEvent(ev)
                            }

                            if (dy > 0) { // collapsing
                                if (viewDragHelper.isDragging().not() &&
                                    (it.isAtBottomPosition || it.isAtMiddlePosition)) {
                                    isScrollableViewHandlingTouch = true
                                    return super.dispatchTouchEvent(ev)
                                }

                                isScrollableViewHandlingTouch = false
                                return onTouchEvent(ev)
                            } else if (dy < 0) { // expanding
                                if (viewDragHelper.isDragging().not() &&
                                    (it.isAtTopPosition || it.isAtMiddlePosition)) {
                                    isScrollableViewHandlingTouch = true
                                    return super.dispatchTouchEvent(ev)
                                }

                                isScrollableViewHandlingTouch = false
                                return onTouchEvent(ev)
                            }
                        }
                    }
                    SlidingMethod.ONLY_DRAGGABLE_VIEW -> {

                    }
                }

            }
        }

        return super.dispatchTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {

        if (isPanelLocked || isScrollableViewHandlingTouch) {
            viewDragHelper.abort()
            return false
        }

        val dragSlop = viewDragHelper.touchSlop

        val x = ev?.x ?: 0f
        val y = ev?.y ?: 0f
        val dx = (x - initMotionX).absoluteValue
        val dy = (y - initMotionY).absoluteValue

        when (ev?.action) {
            MotionEvent.ACTION_DOWN -> {
                isUnableToDrag = false
                initMotionX = ev.x
                initMotionY = ev.y

                when (slidingMethod) {
                    SlidingMethod.All_SLIDEABLE_VIEW,
                    SlidingMethod.WHEN_SCROLLABLE_VIEW_NO_LONGER_CONSUME_EVENT -> {
                        slideableView?.also {
                            if (!isViewUnder(it, ev.x.toInt(), ev.y.toInt())) {
                                viewDragHelper.cancel()
                                isUnableToDrag = true
                                return false
                            }
                        }

                        if (!canHandleDraggingEventOnHideableBar) {
                            hideOnExpandedView?.also {
                                if (isViewUnder(it, ev.x.toInt(), ev.y.toInt())) {
                                    viewDragHelper.cancel()
                                    isUnableToDrag = true
                                    return false
                                }
                            }
                        }
                    }
                    SlidingMethod.ONLY_DRAGGABLE_VIEW -> {
                        dragableView?.also {
                            if (!isViewUnder(it, ev.x.toInt(), ev.y.toInt())) {
                                viewDragHelper.cancel()
                                isUnableToDrag = true
                                return false
                            }
                        }
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isUnableToDrag) {
                    return false
                }

                if (dy > dragSlop && dx > dy) {
                    viewDragHelper.cancel()
                    isUnableToDrag = true
                    return false
                }
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                if (viewDragHelper.isDragging()) {
                    viewDragHelper.processTouchEvent(ev)
                    return true
                }
            }
            else -> {

            }
        }

        return ev?.let { viewDragHelper.shouldInterceptTouchEvent(it) } ?: false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {

        if (isPanelLocked || !isEnabled || isUnableToDrag) {
            return super.onTouchEvent(event)
        }

        event?.also { ev ->
            viewDragHelper.processTouchEvent(ev)
            return true
        }

        return super.onTouchEvent(event)
    }

    override fun computeScroll() {
        super.computeScroll()
        if (viewDragHelper.continueSettling(true)) {
            if (!isEnabled) {
                viewDragHelper.abort()
                return
            }

            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams): LayoutParams {
        return LayoutParams(p)
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return LayoutParams()
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return LayoutParams(context, attrs)
    }

    fun clearAnchors() {
        relativeAnchors = listOf()
        absoluteAnchors = listOf()
    }

    fun smoothSlideTo(relativeOffSet: Float) {
        val offset = allAnchorsInRelative.nearbyAnchor(relativeOffSet)

        slideableView?.apply {
            viewDragHelper.smoothSlideViewTo(this, left,
                calculateSlidingPanelTopPosition(offset))
        }

        invalidate()
    }

    fun smoothSlideTo(absoluteOffSet : Int) {
        val relativeOffset = convertToRelativeOffset(absoluteOffSet)

        smoothSlideTo(relativeOffset)
    }

    fun jumpTo(relativeOffSet: Float) {
        internalRelativeSlideOffset = allAnchorsInRelative.nearbyAnchor(relativeOffSet)
        invalidate()

        internalPanelState = when (internalRelativeSlideOffset) {
            0.0f -> PanelState.COLLAPSED
            1.0f -> PanelState.EXPANDED
            else -> {
                PanelState.ANCHORED
            }
        }
    }

    fun jumpTo(absoluteOffSet : Int) {
        val relativeOffset = convertToRelativeOffset(absoluteOffSet)

        jumpTo(relativeOffset)
    }

    fun insertAnchor(absoluteOffset : Int) {
        absoluteAnchors += absoluteOffset
    }

    fun insertAnchor(relativeOffSet : Float) {
        relativeAnchors += relativeOffSet
    }

    fun addSlidingPanelListener(listener : SlidingPanelListener) {
        slidingPanelListeners += listener
    }

    fun removeSlidingPanelListener(listener : SlidingPanelListener) {
        slidingPanelListeners -= listener
    }

    private fun calculateSlidingPanelTopPosition(relativeOffSet : Float) : Int {

        return measuredHeight - paddingBottom - visibleBarHeight - hideableBarHeight -
                visibleContentHeight - (internalSlideableRange * relativeOffSet).toInt()
    }

    private fun calculateSlidingPanelRelativeOffset(newTop : Int) : Float {
        return (measuredHeight - visibleBarHeight -
                visibleContentHeight -
                hideableBarHeight - newTop).toFloat() / internalSlideableRange.toFloat()
    }

    private fun onSlidingPanelDragged(newTop : Int) {
        internalPanelState = PanelState.DRAGGING
        internalRelativeSlideOffset = calculateSlidingPanelRelativeOffset(newTop)
        hideOnExpandedView?.alpha = calculateHideOnCollapsedViewAlpha(newTop)
        calculateHideOnCollapsedViewScale(newTop).also {
            hideOnExpandedView?.scaleX = it
            hideOnExpandedView?.scaleY = it
        }
    }

    private fun calculateHideOnCollapsedViewAlpha(top : Int) : Float {
        return if (top > toolbarHeight) {
            1.0f
        } else {
            (top - toolbarHeight + hideableBarHeight).toFloat() / hideableBarHeight.toFloat()
        }
    }

    private fun calculateHideOnCollapsedViewScale(top : Int) : Float {
        return if (top > toolbarHeight) {
            1.0f
        } else {
            val t = (top - toolbarHeight + hideableBarHeight).toFloat() / hideableBarHeight.toFloat()
            0.5f + t / 2.0f
        }
    }

    private fun notifySlidingOffsetChanged(offset: Float) {
        slideableView?.also { view ->
            slidingPanelListeners.forEach {
                it.onPanelSliding(view, offset)
            }
        }
    }

    private fun notifySlidingPanelStateChanged(oldState : PanelState, newState : PanelState) {
        slideableView?.also { view ->
            slidingPanelListeners.forEach {
                it.onPanelStateChanged(view, oldState, newState)
            }
        }
    }

    private fun isViewUnder(view : View, x : Int, y : Int) : Boolean {
        val (viewLocationX, viewLocationY) = view.leftTopOnScreen
        val (parentLocationX, parentLocationY) = leftTopOnScreen

        val screenX = parentLocationX + x
        val screenY = parentLocationY + y

        return screenX >= viewLocationX && screenX < viewLocationX + view.width &&
                screenY >= viewLocationY && screenY < viewLocationY + view.height
    }

    private fun isViewUnder(view : IVerticalScrollableView, x : Int, y : Int) : Boolean =
        isViewUnder(view.verticalScrollableView, x, y)

    private fun convertToRelativeOffset(absoluteOffset : Int) : Float {
        return when {
            absoluteOffset in (0 .. internalSlideableRange) -> absoluteOffset.toFloat() / internalSlideableRange.toFloat()
            absoluteOffset > internalRelativeSlideOffset -> 1.0f
            else -> 0.0f
        }
    }

    private fun generatorAllAnchorsInRelative() {
        absoluteAnchorsInRelativeMap = absoluteAnchors.map {
            convertToRelativeOffset(it)
        }

        allAnchorsInRelative = absoluteAnchorsInRelativeMap.asSequence().plus(relativeAnchors)
            .distinct()
            .sorted().filter {
                it > 0.0f && it < 1.0f
            }.toList()
    }

    private fun convertToAbsoluteOffset(relativeOffset : Float) : Int? {
        return if (relativeOffset < 0.0f && relativeOffset > 1.0f) {
            null
        } else {
            (relativeOffset * internalSlideableRange).toInt()
        }
    }

    class LayoutParams : ViewGroup.MarginLayoutParams {

        constructor() : super(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT)

        constructor(width: Int, height: Int) : super(width, height)

        constructor(source: ViewGroup.LayoutParams) : super(source)

        constructor(source: ViewGroup.MarginLayoutParams) : super(source)

        constructor(source: LayoutParams) : super(source)

        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    }

    class SimpleVerticalScrollableView(val view : View) : IVerticalScrollableView {
        override val isAtTopPosition: Boolean
            get() = view.isAtTopPosition
        override val isAtBottomPosition: Boolean
            get() = view.isAtBottomPosition
        override val isAtMiddlePosition: Boolean
            get() = view.isAtMiddlePosition
        override val isUnScrollable: Boolean
            get() = view.isUnScrollable
        override val verticalScrollerState: VerticalScrollerState
            get() = view.verticalScrollerState
        override val verticalScrollableView: View
            get() = view
    }

    enum class PanelState {
        IDLE,
        EXPANDED,
        COLLAPSED,
        ANCHORED,
        DRAGGING,
        SETTLING
    }

    enum class SlidingMethod(val value : Int) {
        All_SLIDEABLE_VIEW(0),
        WHEN_SCROLLABLE_VIEW_NO_LONGER_CONSUME_EVENT(1),
        ONLY_DRAGGABLE_VIEW(2);

        companion object {
            private val map =
                SlidingMethod.values().associateBy {
                        slidingMethod: SlidingMethod -> slidingMethod.value
                }

            fun fromInt(type: Int?) : SlidingMethod? {
                return map[type]
            }
        }
    }

    interface SlidingPanelListener {
        fun onPanelSliding(panel: View, slideOffset : Float)
        fun onPanelStateChanged(panel: View, preState: PanelState, newState : PanelState)
    }
}

private val ViewGroup.childViews : List<View>
    get() = (0 until childCount).map { getChildAt(it) }

private fun ViewDragHelper.isDragging() : Boolean =
    viewDragState == ViewDragHelper.STATE_DRAGGING

// get View's (left, top) point based on screen coordinate
private val View.leftTopOnScreen : Pair<Int, Int>
    get() {
        val viewLocation = intArrayOf(0, 0)
        getLocationOnScreen(viewLocation)

        return Pair(viewLocation[0], viewLocation[1])
    }

private fun List<Float>.nextAnchor(offset : Float) : Float =
    firstOrNull { it > offset } ?: 1.0f

private fun List<Float>.previousAnchor(offset: Float) : Float =
    reversed().firstOrNull { it < offset } ?: 0.0f

private fun List<Float>.nearbyAnchor(offset: Float) : Float {
    return plus(1.0f).asSequence().mapIndexed { index, value ->
        if (index == 0) {
            Pair(0.0f, value)
        } else {
            Pair(get(index - 1), value)
        }
    }.firstOrNull {
        offset >= it.first && offset <= it.second
    }?.let {
        if (offset <= (it.first / 2 + it.second / 2)) {
            it.first
        } else {
            it.second
        }
    } ?: if (offset < 0) 0.0f else 1.0f
}