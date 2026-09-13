package com.zenith.launcher.ui.home.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Powers hold-and-drag reordering across the Home screen's 3-column widget grid - the same
 * interaction the stock Android home screen uses to move a widget: long-press it, drag it
 * anywhere (including into another column), drop it in its new spot.
 *
 * Unlike a single [androidx.compose.foundation.lazy.LazyColumn], a plain 3-column layout has no
 * built-in notion of "the item under my finger", so this class tracks every widget's on-screen
 * bounds (via [onGloballyPositioned]) and every column's bounds, and does its own hit-testing
 * each time the finger moves: the nearest column by horizontal center decides *which* column,
 * and how many of that column's other widgets sit above the finger decides *what index*.
 *
 * [columns] and [onMove] are read through plain `var`s that [rememberGridDragDropState] refreshes
 * on every recomposition, rather than being captured once into a `remember`-cached lambda - a
 * lambda captured only at creation time would keep reading the widget arrangement from the
 * moment the drag state was first created, silently mis-computing every drop target after the
 * very first move.
 */
class GridDragDropState {
    /** Always the latest column arrangement - refreshed every recomposition. */
    var columns: List<List<String>> = emptyList()

    /** Always the latest move callback - refreshed every recomposition. */
    var onMove: (id: String, toColumn: Int, toIndex: Int) -> Unit = { _, _, _ -> }

    private val itemBounds = mutableStateMapOf<String, Rect>()
    private val columnBounds = mutableStateMapOf<Int, Rect>()

    var draggingId by mutableStateOf<String?>(null)
        private set

    private var dragStartBounds: Rect? = null
    private var draggedDelta by mutableStateOf(Offset.Zero)
    private var pointerStartRoot = Offset.Zero
    private var lastTarget: Pair<Int, Int>? = null

    fun isDragging(id: String): Boolean = id == draggingId

    /** Called from every grid item's `onGloballyPositioned` to keep hit-testing data current. */
    fun reportItemBounds(id: String, bounds: Rect) {
        itemBounds[id] = bounds
    }

    /** Last known on-screen (root-space) bounds for widget [id], if it's been laid out yet. */
    fun boundsOf(id: String): Rect? = itemBounds[id]

    /** Called from every column container's `onGloballyPositioned`. */
    fun reportColumnBounds(column: Int, bounds: Rect) {
        columnBounds[column] = bounds
    }

    /** How far (in px, x and y) the currently-dragged item should be visually offset. */
    val draggingItemOffset: Offset
        get() {
            val id = draggingId ?: return Offset.Zero
            val start = dragStartBounds ?: return Offset.Zero
            val current = itemBounds[id] ?: return draggedDelta
            // Compensate for the item having already jumped to a new slot mid-drag (because the
            // backing order changed), so visually it still tracks the finger with no jump.
            return (start.topLeft - current.topLeft) + draggedDelta
        }

    fun onDragStart(id: String, pointerRoot: Offset) {
        draggingId = id
        dragStartBounds = itemBounds[id]
        draggedDelta = Offset.Zero
        pointerStartRoot = pointerRoot
        val column = columns.indexOfFirst { it.contains(id) }
        lastTarget = if (column >= 0) column to columns[column].indexOf(id) else null
    }

    fun onDrag(delta: Offset) {
        if (draggingId == null) return
        draggedDelta += delta

        val pointer = pointerStartRoot + draggedDelta
        val targetColumn = columnBounds.entries
            .minByOrNull { (_, bounds) -> kotlin.math.abs(bounds.center.x - pointer.x) }
            ?.key ?: return

        val itemsInColumn = columns.getOrNull(targetColumn) ?: return
        val id = draggingId ?: return
        val targetIndex = itemsInColumn.count { otherId ->
            otherId != id && (itemBounds[otherId]?.center?.y ?: Float.MAX_VALUE) < pointer.y
        }

        val target = targetColumn to targetIndex
        if (target != lastTarget) {
            lastTarget = target
            onMove(id, targetColumn, targetIndex)
        }
    }

    fun onDragEnd() {
        draggingId = null
        dragStartBounds = null
        draggedDelta = Offset.Zero
        lastTarget = null
    }
}

/**
 * Remembers a single [GridDragDropState] for the lifetime of the composition, but refreshes its
 * [GridDragDropState.columns]/[GridDragDropState.onMove] on every call so the drag logic always
 * sees the latest widget arrangement - see the class doc for why that indirection matters.
 */
@Composable
fun rememberGridDragDropState(
    columns: List<List<String>>,
    onMove: (id: String, toColumn: Int, toIndex: Int) -> Unit
): GridDragDropState {
    val state = remember { GridDragDropState() }
    state.columns = columns
    state.onMove = onMove
    return state
}

/** Attach to a column's container to register its bounds for cross-column hit-testing. */
fun Modifier.reportColumnBounds(state: GridDragDropState, column: Int): Modifier =
    this.onGloballyPositioned { coordinates -> state.reportColumnBounds(column, coordinates.boundsInRoot()) }

/**
 * Attach to a reorderable grid item (with its known widget [id]) to make it hold-and-draggable
 * across the whole 3-column grid. A short haptic tick fires on drag start so the affordance
 * feels the same as a real launcher's "pick up a widget" gesture.
 */
@Composable
fun Modifier.gridDragToReorder(state: GridDragDropState, id: String): Modifier {
    val haptics = LocalHapticFeedback.current
    return this
        .onGloballyPositioned { coordinates -> state.reportItemBounds(id, coordinates.boundsInRoot()) }
        .pointerInput(state, id) {
            detectDragGesturesAfterLongPress(
                onDragStart = { localOffset ->
                    // onGloballyPositioned always runs before a drag can start, so this item's
                    // root-space top-left is already known; add the touch's local offset to it
                    // to get the touch-down point in the same root-space coordinates as bounds.
                    val itemTopLeft = state.boundsOf(id)?.topLeft ?: Offset.Zero
                    state.onDragStart(id, itemTopLeft + localOffset)
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    state.onDrag(dragAmount)
                },
                onDragEnd = { state.onDragEnd() },
                onDragCancel = { state.onDragEnd() }
            )
        }
}
