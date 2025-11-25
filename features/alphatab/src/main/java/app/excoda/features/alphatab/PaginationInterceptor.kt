package app.excoda.features.alphatab

import android.view.MotionEvent
import android.view.View
import app.excoda.core.logging.LxLog

class PaginationInterceptor(
    private val onPreviousPage: () -> Unit,
    private val onNextPage: () -> Unit,
    private val onCenterZoneTap: () -> Unit
) : View.OnTouchListener {
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) {
            return false // Let other events pass through
        }

        val viewWidth = v.width
        val tapX = event.x

        // Calculate tap zones: 25% - 50% - 25%
        val leftZoneEnd = viewWidth * 0.25f
        val rightZoneStart = viewWidth * 0.75f

        when {
            tapX < leftZoneEnd -> {
                LxLog.d("AlphaTabPagination", "Left zone tap (25%) - previous page")
                onPreviousPage()
                return true // Consume event
            }
            tapX > rightZoneStart -> {
                LxLog.d("AlphaTabPagination", "Right zone tap (25%) - next page")
                onNextPage()
                return true // Consume event
            }
            else -> {
                // Center zone (50%) - toggle UI visibility
                LxLog.d("AlphaTabPagination", "Center zone tap (50%) - toggle UI")
                onCenterZoneTap()
                return true // Consume event
            }
        }
    }
}