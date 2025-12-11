package app.excoda.features.alphatab

import android.view.MotionEvent
import android.view.View
import android.webkit.WebView
import app.excoda.core.logging.LxLog
import kotlin.math.abs

class PaginationInterceptor(
    private val onPreviousPage: () -> Unit,
    private val onNextPage: () -> Unit,
    private val onCenterZoneTap: () -> Unit,
    private val isFabExpanded: () -> Boolean
) : View.OnTouchListener {

    private var downX = 0f
    private var downY = 0f
    private var downTime = 0L

    companion object {
        private const val TAP_THRESHOLD = 100f
        private const val SWIPE_THRESHOLD = 150f
        private const val SWIPE_TIMEOUT = 500L
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                downTime = event.eventTime
                LxLog.d("AlphaTabPagination", "DOWN at x=${event.x.toInt()}, y=${event.y.toInt()}")

                (v as? WebView)?.onTouchEvent(event)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                (v as? WebView)?.onTouchEvent(event)
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val upX = event.x
                val upY = event.y
                val deltaX = abs(upX - downX)
                val deltaY = abs(upY - downY)
                val deltaTime = event.eventTime - downTime
                val deltaXSigned = upX - downX

                LxLog.d("AlphaTabPagination", "UP at x=${upX.toInt()}, y=${upY.toInt()} | delta: x=${deltaX.toInt()}, y=${deltaY.toInt()}, time=${deltaTime}ms")

                if (isFabExpanded()) {
                    LxLog.d("AlphaTabPagination", "FAB menu is expanded - ignoring tap")
                    (v as? WebView)?.onTouchEvent(event)
                    return true
                }

                val isHorizontalSwipe = deltaX > SWIPE_THRESHOLD &&
                        deltaX > deltaY * 1.5f &&
                        deltaTime < SWIPE_TIMEOUT

                if (isHorizontalSwipe) {
                    val direction = if (deltaXSigned > 0) "RIGHT" else "LEFT"
                    LxLog.d("AlphaTabPagination", "SWIPE $direction detected")
                    (v as? WebView)?.onTouchEvent(event)
                    return true
                }

                if (deltaX > TAP_THRESHOLD || deltaY > TAP_THRESHOLD) {
                    LxLog.d("AlphaTabPagination", "Movement too large - not a tap")
                    return true
                }

                val viewWidth = v.width
                val viewHeight = v.height
                val leftZoneEnd = viewWidth * 0.25f
                val rightZoneStart = viewWidth * 0.75f
                val rightZoneVerticalLimit = viewHeight * 0.8f

                when {
                    upX < leftZoneEnd -> {
                        LxLog.d("AlphaTabPagination", "TAP: Left zone - previous page")
                        v.performClick()
                        onPreviousPage()
                    }
                    upX > rightZoneStart && upY < rightZoneVerticalLimit -> {
                        LxLog.d("AlphaTabPagination", "TAP: Right zone - next page")
                        v.performClick()
                        onNextPage()
                    }
                    else -> {
                        LxLog.d("AlphaTabPagination", "TAP: Center zone - toggle UI")
                        v.performClick()
                        onCenterZoneTap()
                    }
                }
                return true
            }
        }

        return false
    }
}