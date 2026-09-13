package com.zenith.launcher.ui.home.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Powers hold-and-drag reordering inside a [LazyListState] - the same interaction the stock
 * Android home screen uses to let you move a widget: long-press it, drag, drop it in its new
 * spot. [onMove] is called live as the dragged item crosses a neighbor so the caller (backed by
 * a StateFlow) can update the real ordering; the visual item then just follows the finger via
 * [draggingItemOffset] until released.
 *
 * The item being dragged is identified by its *list index* (known up front by the caller via
 * `itemsIndexed`), not by hit-testing a touch coordinate - a pointerInput attached to a single
 * item only ever sees coordinates local to that item, not the list's viewport, so matching
 * against [LazyListState.layoutInfo] offsets by touch position would silently pick the wrong
 * item.
 */
class DragDropListState(
    val listState: LazyListState,
    private val onMove: (from: Int, to: Int) -> Unit
) {
    private var draggedDistance by mutableStateOf(0f)
    private var draggingItem by mutableStateOf<LazyListItemInfo?>(null)

    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set

    /** How far (in px) the currently-dragged item should be visually offset from its slot. */
    val draggingItemOffset: Float
        get() {
            val item = draggingItem ?: return 0f
            val current = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == draggingItemIndex }
                ?: return draggedDistance
            return (item.offset - current.offset) + draggedDistance
        }

    fun isDragging(index: Int): Boolean = index == draggingItemIndex

    fun onDragStart(index: Int) {
        val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return
        draggingItem = info
        draggingItemIndex = index
        draggedDistance = 0f
    }

    fun onDrag(deltaY: Float) {
        val item = draggingItem ?: return
        draggedDistance += deltaY

        val startOffset = item.offset + draggedDistance
        val endOffset = item.offset + item.size + draggedDistance
        val middle = (startOffset + endOffset) / 2f

        val target = listState.layoutInfo.visibleItemsInfo.firstOrNull { candidate ->
            candidate.index != draggingItemIndex && middle.toInt() in candidate.offset..(candidate.offset + candidate.size)
        }

        if (target != null) {
            val from = draggingItemIndex ?: item.index
            onMove(from, target.index)
            draggingItemIndex = target.index
        }
    }

    fun onDragEnd() {
        draggingItem = null
        draggingItemIndex = null
        draggedDistance = 0f
    }
}

@Composable
fun rememberDragDropListState(
    listState: LazyListState,
    onMove: (from: Int, to: Int) -> Unit
): DragDropListState {
    return remember(listState) { DragDropListState(listState, onMove) }
}

/**
 * Attach to a reorderable item (at its known [index] within the list) to make it hold-and-
 * draggable. A short haptic tick fires on drag start so the affordance feels the same as a
 * real launcher's "pick up a widget" gesture.
 */
@Composable
fun Modifier.dragToReorder(state: DragDropListState, index: Int): Modifier {
    val haptics = LocalHapticFeedback.current
    return this.pointerInput(state, index) {
        detectDragGesturesAfterLongPress(
            onDragStart = {
                state.onDragStart(index)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            onDrag = { change, dragAmount ->
                change.consume()
                state.onDrag(dragAmount.y)
            },
            onDragEnd = { state.onDragEnd() },
            onDragCancel = { state.onDragEnd() }
        )
    }
}
