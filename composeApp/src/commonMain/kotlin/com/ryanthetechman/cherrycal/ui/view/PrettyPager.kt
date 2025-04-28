package com.ryanthetechman.cherrycal.ui.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.launch

/** Direction of swipe‐paging. */
enum class PagerOrientation { Horizontal, Vertical }

@Composable
fun PrettyPager(
    orientation: PagerOrientation = PagerOrientation.Horizontal,
    initialPage: Int = Int.MAX_VALUE / 2,
    pageCount: () -> Int = { Int.MAX_VALUE },
    modifier: Modifier = Modifier,
    onPageChanged: (Int) -> Unit = {},
    externalPage: Int? = null,
    scrollForwardIsNext: Boolean = true,
    reversePageDirection: Boolean = false,
    animationSpec: AnimationSpec<Float> = tween(300),
    content: @Composable BoxScope.(page: Int, pageOffset: Float) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = pageCount)
    val scope      = rememberCoroutineScope()

    if (externalPage != null) {
        LaunchedEffect(externalPage) {
            pagerState.scrollToPage(externalPage)
        }
    }

    // Intercept vertical-wheel ticks → one page hop
    val wheelModifier = modifier.pointerInput(scrollForwardIsNext) {
        awaitPointerEventScope {
            while (true) {
                val ev = awaitPointerEvent()
                val ch = ev.changes.firstOrNull() ?: continue
                val dy = ch.scrollDelta.y
                if (dy == 0f) continue

                // ignore if an animation/drag is in progress
                if (pagerState.currentPageOffsetFraction != 0f) {
                    ch.consume()
                    continue
                }

                val forward = dy < 0f
                val target  = if (forward == scrollForwardIsNext)
                    pagerState.currentPage + 1
                else
                    pagerState.currentPage - 1

                scope.launch {
                    pagerState.animateScrollToPage(target, 0f, animationSpec)
                }
                ch.consume()
            }
        }
    }.fillMaxSize()

    // Render the actual pager
    if (orientation == PagerOrientation.Horizontal) {
        HorizontalPager(state = pagerState, modifier = wheelModifier) { page ->
            Box(Modifier.fillMaxSize()) {
                val raw = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                val pageOffset = if (reversePageDirection) -raw else raw
                content(page, pageOffset)
            }
        }
    } else {
        VerticalPager(state = pagerState, modifier = wheelModifier) { page ->
            Box(Modifier.fillMaxSize()) {
                val raw = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                val pageOffset = if (reversePageDirection) -raw else raw
                content(page, pageOffset)
            }
        }
    }

    // Notify when a page settles
    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
    }
}

@Suppress("UnusedReceiverParameter")
@Composable
fun BoxScope.ParallelText(
    text: String,
    orientation: PagerOrientation,
    pageOffset: Float,
    insetStart: Float = 0f,    // e.g. time‑column width in px
    slideFraction: Float = 0.35f,
    modifier: Modifier = Modifier,
    reversePageDirection: Boolean = false,
) {
    if (pageOffset == 0f) return

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
        ) {
            // guide lines
            if (orientation == PagerOrientation.Horizontal) {
                Box(Modifier.fillMaxHeight().width(1.dp).background(Color.White).align(Alignment.CenterStart))
                Box(Modifier.fillMaxHeight().width(1.dp).background(Color.White).align(Alignment.CenterEnd))
            } else {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White).align(Alignment.TopCenter))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White).align(Alignment.BottomCenter))
            }

            // sliding label
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val density = LocalDensity.current
                val maxSlide = with(density) {
                    if (orientation == PagerOrientation.Horizontal) {
                        (maxWidth.toPx() - insetStart) * slideFraction
                    } else {
                        maxHeight.toPx() * -slideFraction
                    }
                }

                var translation = if (orientation == PagerOrientation.Horizontal) {
                    if (pageOffset < 0f) lerp(-maxSlide, 0f, 1f + pageOffset)
                    else              lerp(maxSlide,  0f, 1f - pageOffset)
                } else {
                    if (pageOffset < 0f) lerp(maxSlide,  0f, 1f + pageOffset)
                    else                 lerp(-maxSlide, 0f, 1f - pageOffset)
                }
                if (reversePageDirection) translation = -translation

                Text(
                    text = text,
                    color = Color.White,
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .graphicsLayer {
                            if (orientation == PagerOrientation.Horizontal) translationX = translation
                            else                                           translationY = translation
                        }
                )
            }
        }
    }
}