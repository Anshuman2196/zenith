package com.zenith.launcher.ui.home.components
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback


/**
 * Powers hold-and-drag reordering across the Home screen's 3-column widget grid, matching the
 * stock Android home screen's "lock, then hold-to-unlock-and-move" model: widgets are normal and
 * un-draggable until a long-press picks one up, which also flips [editMode] on for the rest of
 * the grid (see [HomeScreen]) so the whole screen dedicates itself to rearranging - taps, the
 * app-drawer edge-swipe, etc. all pause - until the user explicitly finishes via
 * [HomeScreen]'s "Done" affordance, tapping empty space, or the system back gesture.
 *
 * ## Why the dragged item is a floating "ghost", not an in-place offset
 * An earlier version of this class rendered the dragged item in-place and tried to compensate
 * for it having already jumped to a new column/index (because the backing order changes live as
 * you drag past other widgets) by comparing the item's bounds *before* and *after* that jump.
 * That comparison read [itemBounds] - a snapshot updated by [onGloballyPositioned], which only
 * fires once the *next* layout pass finishes - while the pointer-tracking coroutine below runs
 * every raw pointer-move event, effectively one tick ahead of it. The result was a one-frame
 * mismatch every time a reorder happened, which is often (every time the finger crosses a
 * sibling), producing a visible jitter.
 *
 * The fix used here sidesteps that race entirely: the dragged widget is rendered as a completely
 * separate, absolutely-positioned "ghost" copy (see `DragGhostOverlay` in HomeScreen.kt) that
 * tracks [dragPointerRoot] directly - nothing about its position depends on the grid's own live
 * layout, so there's nothing for a layout-timing race to disturb. The *real* copy of the widget
 * stays in the grid (so the grid still reflows correctly around it) but is invisible while
 * dragging.
 */
class GridDragDropState {
    /** Always the latest column arrangement - refreshed every recomposition. */
    var columns: List<List<String>> = emptyList()

    /** Always the latest move callback - refreshed every recomposition. */
    var onMove: (id: String, toColumn: Int, toIndex: Int) -> Unit = { _, _, _ -> }

    private val itemBounds = mutableStateMapOf<String, Rect>()
    private val columnBounds = mutableStateMapOf<Int, Rect>()

    /** True from the moment any widget is picked up until the user explicitly exits rearranging. */
    var editMode by mutableStateOf(false)
        private set

    var draggingId by mutableStateOf<String?>(null)
        private set

    /** Root-space (screen) coordinates of the finger, updated on every pointer move while dragging. */
    var dragPointerRoot by mutableStateOf(Offset.Zero)
        private set

    /** The dragged item's own on-screen size at the moment it was picked up, for sizing its ghost. */
    var draggingItemSize by mutableStateOf<Size?>(null)
        private set

    fun isDragging(id: String): Boolean = id == draggingId

    /** Last known on-screen (root-space) bounds for widget [id], if it's been laid out yet. */
    fun boundsOf(id: String): Rect? = itemBounds[id]

    fun enterEditMode() {
        editMode = true
    }

    /** Called by the "Done" pill, a tap on empty grid space, or the system back gesture. */
    fun exitEditMode() {
        editMode = false
        onDragEnd()
    }

    /** Called from every grid item's `onGloballyPositioned` to keep hit-testing data current. */
    fun reportItemBounds(id: String, bounds: Rect) {
        itemBounds[id] = bounds
    }

    /** Called from every column container's `onGloballyPositioned`. */
    fun reportColumnBounds(column: Int, bounds: Rect) {
        columnBounds[column] = bounds
    }

    fun onDragStart(id: String, pointerRoot: Offset) {
        enterEditMode()
        draggingId = id
        draggingItemSize = itemBounds[id]?.size
        dragPointerRoot = pointerRoot
    }

    fun onDrag(delta: Offset) {
        if (draggingId == null) return
        dragPointerRoot += delta

        val pointer = dragPointerRoot
        val targetColumn = columnBounds.entries
            .minByOrNull { (_, bounds) -> kotlin.math.abs(bounds.center.x - pointer.x) }
            ?.key ?: return

        val itemsInColumn = columns.getOrNull(targetColumn) ?: return
        val id = draggingId ?: return
        val targetIndex = itemsInColumn.count { otherId ->
            otherId != id && (itemBounds[otherId]?.center?.y ?: Float.MAX_VALUE) < pointer.y
        }

        val fromColumn = columns.indexOfFirst { it.contains(id) }
        val fromIndex = columns.getOrNull(fromColumn)?.indexOf(id) ?: -1
        if (targetColumn != fromColumn || targetIndex != fromIndex) {
            onMove(id, targetColumn, targetIndex)
        }
    }

    fun onDragEnd() {
        draggingId = null
        draggingItemSize = null
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
 * Attach to a reorderable grid item (with its known widget [id]) to make it hold-and-draggable.
 * The very first long-press of any session also flips the whole grid into [GridDragDropState.editMode]
 * (see the class doc) - a short haptic tick fires at that moment so picking a widget up feels the
 * same as a real launcher's "pick up a widget" gesture.
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
