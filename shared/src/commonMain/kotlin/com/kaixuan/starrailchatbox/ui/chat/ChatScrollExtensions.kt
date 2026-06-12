package com.kaixuan.starrailchatbox.ui.chat

import androidx.compose.foundation.lazy.LazyListState
import kotlin.math.abs

/**
 * 智能滚动到指定项。
 * 如果滚动的估计像素距离大于半个屏幕，就使用无动画滚动。
 * 否则，使用有动画滚动。
 */
suspend fun LazyListState.smartScrollToItem(index: Int, scrollOffset: Int = 0) {
    val visibleItems = layoutInfo.visibleItemsInfo
    val viewportHeight = layoutInfo.viewportSize.height

    if (visibleItems.isEmpty() || viewportHeight <= 0) {
        scrollToItem(index, scrollOffset)
        return
    }

    val averageHeight = visibleItems.sumOf { it.size } / visibleItems.size
    if (averageHeight <= 0) {
        scrollToItem(index, scrollOffset)
        return
    }

    val distance = abs(index - firstVisibleItemIndex) * averageHeight
    if (distance > viewportHeight / 2) {
        scrollToItem(index, scrollOffset)
    } else {
        animateScrollToItem(index, scrollOffset)
    }
}
