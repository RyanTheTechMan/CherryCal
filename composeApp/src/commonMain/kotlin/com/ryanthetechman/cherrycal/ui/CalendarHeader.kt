package com.ryanthetechman.cherrycal.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times

enum class CalendarViewMode {
    DAY, WEEK, MONTH, YEAR
}

@Composable
fun CalendarHeader(
    selectedView: CalendarViewMode,
    onViewSelected: (CalendarViewMode) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp
    ) {
        CalendarViewSelector(
            selectedView = selectedView,
            onViewSelected = onViewSelected
        )
    }
}

@Composable
fun CalendarViewSelector(
    selectedView: CalendarViewMode,
    onViewSelected: (CalendarViewMode) -> Unit
) {
    val options = listOf(
        CalendarViewMode.DAY,
        CalendarViewMode.WEEK,
        CalendarViewMode.MONTH,
//        CalendarViewMode.YEAR
    )
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        val screenWidth = maxWidth
        val optionWidth = screenWidth / options.size
        val selectedIndex = options.indexOf(selectedView)
        val animatedOffset by animateDpAsState(targetValue = (selectedIndex * optionWidth))

        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .offset(x = animatedOffset)
                    .height(40.dp)
                    .width(optionWidth)
                    .background(Color(0xFFBBDEFB).copy(alpha = 0.3f))
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                options.forEach { mode ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onViewSelected(mode) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = mode.name)
                    }
                }
            }
        }
    }
}