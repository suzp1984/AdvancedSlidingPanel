package io.github.jacobsu.advancedslidingpanel.widget

import android.view.View
import android.widget.ListView

enum class VerticalScrollerState {
    Unscrollable,
    TopPosition,
    BottomPosition,
    MiddlePosition
}

interface IVerticalScrollableView {
    val isAtTopPosition : Boolean
    val isAtBottomPosition : Boolean
    val isAtMiddlePosition : Boolean
    val isUnScrollable : Boolean
    val verticalScrollerState : VerticalScrollerState
    val verticalScrollableView : View
}

// ListView.verticalScrollerState is accurate than View.verticalScrollerState
val ListView.verticalScrollerState : VerticalScrollerState
    get() {
        if (adapter == null || childCount == 0) {
            return VerticalScrollerState.Unscrollable
        }

        val firstChild = getChildAt(0)
        val lastChild = getChildAt(childCount - 1)


        if (firstVisiblePosition == 0 && lastVisiblePosition == childCount - 1 &&
            firstChild.top == paddingTop && (lastChild.bottom + paddingBottom) <= height) {
            return VerticalScrollerState.Unscrollable
        }

        if (firstVisiblePosition == 0 && firstChild.top == paddingTop) {
            return VerticalScrollerState.TopPosition
        }

        if (lastVisiblePosition == count - 1 && (lastChild.bottom + paddingBottom) <= height) {
            return VerticalScrollerState.BottomPosition
        }

        return VerticalScrollerState.MiddlePosition
    }

val View.verticalScrollerState : VerticalScrollerState
    get() {
        val canScrollUp = canScrollVertically(-1)
        val canScrollDown = canScrollVertically(1)

        return if (!canScrollDown && !canScrollUp) {
            VerticalScrollerState.Unscrollable
        } else if (canScrollDown && canScrollUp) {
            VerticalScrollerState.MiddlePosition
        } else if (!canScrollDown && canScrollUp) {
            VerticalScrollerState.BottomPosition
        } else if (canScrollDown && !canScrollUp) {
            VerticalScrollerState.TopPosition
        } else {
            VerticalScrollerState.Unscrollable
        }
    }

val View.isAtTopPosition : Boolean
    get() = when (verticalScrollerState) {
        VerticalScrollerState.TopPosition -> {
            true
        }
        else -> {
            false
        }
    }

val View.isAtBottomPosition : Boolean
    get() = when (verticalScrollerState) {
        VerticalScrollerState.BottomPosition -> {
            true
        }
        else -> {
            false
        }
    }

val View.isAtMiddlePosition : Boolean
    get() = when (verticalScrollerState) {
        VerticalScrollerState.MiddlePosition -> {
            true
        }
        else -> {
            false
        }
    }

val View.isUnScrollable : Boolean
    get() = when (verticalScrollerState) {
        VerticalScrollerState.Unscrollable -> {
            true
        }
        else -> {
            false
        }
    }