package com.beeeam.feature.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * 위아래로 스크롤해 [values] 중 하나를 고르는 휠 형태의 Picker.
 * 스크롤이 멈춘 시점에 가운데에 위치한 값을 [onValueChange] 로 전달한다.
 */
@Composable
internal fun WheelPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    values: List<Int>,
    modifier: Modifier = Modifier,
    format: (Int) -> String = Int::toString,
    width: Dp = 64.dp,
    itemHeight: Dp = 44.dp,
    visibleCount: Int = 5,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = values.indexOfClosest(value))
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val scope = rememberCoroutineScope()
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val centerIndex by remember { derivedStateOf { listState.centerItemIndex() } }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { !it }
            .collect { currentOnValueChange(values[listState.centerItemIndex().coerceIn(values.indices)]) }
    }
    // 외부에서 값이 바뀐 경우 휠 위치를 맞춘다.
    LaunchedEffect(value) {
        val target = values.indexOfClosest(value)
        if (!listState.isScrollInProgress && listState.centerItemIndex() != target) {
            listState.scrollToItem(target)
        }
    }

    Box(
        modifier = modifier
            .width(width)
            .height(itemHeight * visibleCount),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
        )
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight * (visibleCount / 2)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            itemsIndexed(values) { index, item ->
                val distance = abs(index - centerIndex)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .clickable { scope.launch { listState.animateScrollToItem(index) } },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = format(item),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = if (distance == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (distance == 0) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = if (distance == 1) 0.5f else 0.25f)
                        },
                    )
                }
            }
        }
    }
}

/** [value] 와 같거나 가장 가까운 값의 index. */
private fun List<Int>.indexOfClosest(value: Int): Int =
    indices.minByOrNull { abs(this[it] - value) } ?: 0

/** 뷰포트 중앙에 가장 가까운 아이템의 index. */
private fun LazyListState.centerItemIndex(): Int {
    val info = layoutInfo
    val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
    return info.visibleItemsInfo
        .minByOrNull { abs(it.offset + it.size / 2 - center) }
        ?.index
        ?: firstVisibleItemIndex
}
