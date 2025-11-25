package app.excoda.core.fab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SentimentDissatisfied
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun FaceTrackingIndicator(
    isTracking: Boolean,
    isVisible: Boolean,
    isCenterZoneHidden: Boolean = false,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        // Alpha: 0f when hidden, 0.3f when visible (normal transparent state)
        val alpha = if (isCenterZoneHidden) 0f else 0.3f

        Surface(
            shape = androidx.compose.foundation.shape.CircleShape,
            color = if (isTracking) Color(0xFF4CAF50) else Color(0xFFF44336),
            tonalElevation = 6.dp,
            shadowElevation = 6.dp,
            modifier = Modifier
                .size(56.dp)
                .alpha(alpha)
        ) {
            Icon(
                imageVector = if (isTracking) {
                    Icons.Rounded.SentimentSatisfied
                } else {
                    Icons.Rounded.SentimentDissatisfied
                },
                contentDescription = if (isTracking) "Face tracking" else "Face not detected",
                tint = Color.White,
                modifier = Modifier.size(56.dp)
            )
        }
    }
}