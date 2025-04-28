import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ryanthetechman.cherrycal.calendar_interaction.Calendar

@Composable
fun CalendarSelectorDrawer(
    calendars: List<Calendar>,
    selectedCalendar: Calendar?,
    onCalendarSelected: (Calendar) -> Unit,
) {
    var open by remember { mutableStateOf(false) }

    // Animate width
    val collapsedWidth = 24.dp
    val expandedWidthPercent = 0.20f // % of parent
    BoxWithConstraints(
        modifier = Modifier.fillMaxHeight()
    ) {
        val maxW = maxWidth
        val expandedWidth = maxW * expandedWidthPercent
        val targetWidth = if (open) expandedWidth else collapsedWidth
        val animatedWidth by animateDpAsState(targetValue = targetWidth)

        Surface(
            elevation = 8.dp,
            modifier = Modifier
                .width(animatedWidth)
                .fillMaxHeight()
                .background(Color(0xFFF5F5F5))
                .shadow(if (open) 8.dp else 2.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Edge tab/handle
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(collapsedWidth)
                        .clickable { open = !open },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (open) Icons.AutoMirrored.Filled.ArrowBack else Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = if (open) "Close menu" else "Open menu",
                        tint = Color(0xFF616161)
                    )
                }
                // Menu content (slide in/out)
                if (open) {
                    // List of calendars to select
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(expandedWidth - collapsedWidth)
                            .padding(8.dp)
                    ) {
                        Text(
                            "Calendars",
                            style = MaterialTheme.typography.subtitle1,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Divider()
                        Spacer(Modifier.height(4.dp))
                        if (calendars.isEmpty()) {
                            Text("No calendars", color = Color.Gray, style = MaterialTheme.typography.caption)
                        } else {
                            calendars.forEach { cal ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onCalendarSelected(cal) }
                                        .padding(vertical = 6.dp)
                                        .background(
                                            if (cal == selectedCalendar)
                                                MaterialTheme.colors.primary.copy(alpha = 0.1f)
                                            else
                                                Color.Transparent
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        Modifier
                                            .size(8.dp, 8.dp)
                                            .background(
                                                if (cal == selectedCalendar) MaterialTheme.colors.primary else Color.LightGray,
                                                shape = MaterialTheme.shapes.small
                                            )
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        cal.name,
                                        style = MaterialTheme.typography.body2,
                                        color = if (cal == selectedCalendar)
                                            MaterialTheme.colors.primary else Color.Unspecified
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}