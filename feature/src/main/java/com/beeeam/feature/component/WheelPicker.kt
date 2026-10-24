package com.beeeam.feature.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beeeam.feature.util.AccentOrange
import com.beeeam.feature.util.SetupTextSecondary
import com.beeeam.feature.util.unitValueText
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.abs

/** 휠 한 줄의 높이. 선택된 줄의 하이라이트 밴드도 이 높이에 맞춘다. */
internal val PickerItemHeight = 48.dp

/**
 * 위아래로 스크롤해 [values] 중 하나를 고르는 휠 형태의 Picker.
 * 스크롤이 멈춘 시점에 가운데에 위치한 값을 [onValueChange] 로 전달한다.
 * 선택된 줄의 배경은 그리지 않으므로, 필요하면 바깥에서 [PickerItemHeight] 높이의 밴드를 깔아준다.
 *
 * @param unit 각 값 옆에 붙는 단위. (분, 초, 세트)
 */
@Composable
internal fun WheelPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    values: List<Int>,
    unit: String,
    accent: Color,
    modifier: Modifier = Modifier,
    format: (Int) -> String = Int::toString,
    itemHeight: Dp = PickerItemHeight,
    visibleCount: Int = 3,
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
        modifier = modifier.height(itemHeight * visibleCount),
        contentAlignment = Alignment.Center,
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight * (visibleCount / 2)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            itemsIndexed(values) { index, item ->
                val selected = index == centerIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .clickable(interactionSource = null, indication = null) {
                            scope.launch { listState.animateScrollToItem(index) }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = unitValueText(
                            parts = listOf(format(item) to " $unit"),
                            numberStyle = SpanStyle(
                                fontSize = if (selected) 20.sp else 16.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontFeatureSettings = "tnum",
                            ),
                            unitStyle = SpanStyle(
                                fontSize = if (selected) 16.sp else 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            ),
                        ),
                        color = if (selected) accent else SetupTextSecondary.copy(alpha = 0.3f),
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
