package com.hitsuji.pdfdrivereader.presentation.reader

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.hitsuji.pdfdrivereader.domain.model.ReadingDirection
import com.hitsuji.pdfdrivereader.presentation.theme.PdfDriveReaderTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showDirectionDialog by remember { mutableStateOf(false) }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val widthPx = constraints.maxWidth
        val heightPx = constraints.maxHeight
        val containerMaxWidth = maxWidth
        val containerMaxHeight = maxHeight
        
        LaunchedEffect(widthPx, heightPx) {
            viewModel.updateScreenDimensions(widthPx, heightPx)
        }

        val scaleAnim = remember { Animatable(state.zoomLevel) }
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }
        var decayJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(state.zoomLevel) {
            if (scaleAnim.value != state.zoomLevel) {
                scaleAnim.snapTo(state.zoomLevel)
            }
        }

        val initialListIndex = remember(state.currentPage, state.visiblePages) {
            state.visiblePages.indexOf(state.currentPage).coerceAtLeast(0)
        }
        val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialListIndex)

        LaunchedEffect(listState, state.visiblePages) {
            snapshotFlow { listState.firstVisibleItemIndex }
                .collect { listIndex ->
                    if (listIndex in state.visiblePages.indices) {
                        val pageIndex = state.visiblePages[listIndex]
                        if (pageIndex != state.currentPage) {
                            viewModel.onPageChanged(pageIndex)
                        }
                    }
                }
        }

        LaunchedEffect(state.currentPage, state.visiblePages) {
            val targetListIndex = state.visiblePages.indexOf(state.currentPage)
            if (targetListIndex != -1 && !listState.isScrollInProgress && listState.firstVisibleItemIndex != targetListIndex) {
                listState.scrollToItem(targetListIndex)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .readerGestures(
                    state = state,
                    scaleAnim = scaleAnim,
                    getOffsetX = { offsetX },
                    getOffsetY = { offsetY },
                    setOffsetX = { offsetX = it },
                    setOffsetY = { offsetY = it },
                    getDecayJob = { decayJob },
                    setDecayJob = { decayJob = it },
                    coroutineScope = coroutineScope,
                    listState = listState,
                    onZoomChanged = viewModel::onZoomChanged
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            coroutineScope.launch {
                                decayJob?.cancel()
                                launch { scaleAnim.animateTo(1f) }
                                launch { androidx.compose.animation.core.animate(offsetX, 0f) { value, _ -> offsetX = value } }
                                launch { androidx.compose.animation.core.animate(offsetY, 0f) { value, _ -> offsetY = value } }
                                viewModel.resetZoom()
                            }
                        },
                        onTap = {
                            if (state.textSelection != null) {
                                viewModel.clearSelection()
                            } else {
                                viewModel.toggleUI()
                            }
                        }
                    )
                }
                .graphicsLayer(
                    scaleX = scaleAnim.value,
                    scaleY = scaleAnim.value,
                    translationX = offsetX,
                    translationY = offsetY
                )
        ) {
            when {
                state.isLoading -> {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Black
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                }
                state.errorMessage != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.errorMessage!!, color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { 
                            state.document?.id?.let { viewModel.loadDocument(it) }
                        }) {
                            Text("Retry")
                        }
                    }
                }
                else -> {
                    when (state.direction) {
                        ReadingDirection.LTR -> {
                            LazyRow(
                                state = listState,
                                userScrollEnabled = state.textSelection == null,
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(state.visiblePages.size) { listIndex ->
                                    val pageIndex = state.visiblePages[listIndex]
                                    PdfPageDisplay(
                                        state = state, 
                                        pageIndex = pageIndex, 
                                        containerWidth = containerMaxWidth, 
                                        containerHeight = containerMaxHeight,
                                        onDragStartHandle = { page, x, y -> viewModel.updateSelectionStart(page, x.toInt(), y.toInt()) },
                                        onDragStopHandle = { page, x, y -> viewModel.updateSelectionStop(page, x.toInt(), y.toInt()) },
                                        onLongPress = { page, x, y -> viewModel.selectTextAt(page, x.toInt(), y.toInt()) },
                                        onTap = { _, x, y ->
                                            viewModel.onDocumentTapped(x.toInt(), y.toInt())
                                        }
                                    )
                                }
                            }
                        }
                        ReadingDirection.RTL -> {
                            LazyRow(
                                state = listState,
                                userScrollEnabled = state.textSelection == null,
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                reverseLayout = true,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(state.visiblePages.size) { listIndex ->
                                    val pageIndex = state.visiblePages[listIndex]
                                    PdfPageDisplay(
                                        state = state, 
                                        pageIndex = pageIndex, 
                                        containerWidth = containerMaxWidth, 
                                        containerHeight = containerMaxHeight,
                                        onDragStartHandle = { page, x, y -> viewModel.updateSelectionStart(page, x.toInt(), y.toInt()) },
                                        onDragStopHandle = { page, x, y -> viewModel.updateSelectionStop(page, x.toInt(), y.toInt()) },
                                        onLongPress = { page, x, y -> viewModel.selectTextAt(page, x.toInt(), y.toInt()) },
                                        onTap = { _, x, y ->
                                            viewModel.onDocumentTapped(x.toInt(), y.toInt())
                                        }
                                    )
                                }
                            }
                        }
                        ReadingDirection.TTB -> {
                            LazyColumn(
                                state = listState,
                                userScrollEnabled = state.textSelection == null,
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(state.visiblePages.size) { listIndex ->
                                    val pageIndex = state.visiblePages[listIndex]
                                    PdfPageDisplay(
                                        state = state, 
                                        pageIndex = pageIndex, 
                                        containerWidth = containerMaxWidth, 
                                        containerHeight = containerMaxHeight,
                                        onDragStartHandle = { page, x, y -> viewModel.updateSelectionStart(page, x.toInt(), y.toInt()) },
                                        onDragStopHandle = { page, x, y -> viewModel.updateSelectionStop(page, x.toInt(), y.toInt()) },
                                        onLongPress = { page, x, y -> viewModel.selectTextAt(page, x.toInt(), y.toInt()) },
                                        onTap = { _, x, y ->
                                            viewModel.onDocumentTapped(x.toInt(), y.toInt())
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = state.isUiVisible,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            TopAppBar(
                title = { Text(state.document?.fileName ?: "Reader") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Reading Direction") },
                                onClick = {
                                    showMenu = false
                                    showDirectionDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Toggle UI") },
                                onClick = {
                                    showMenu = false
                                    viewModel.toggleUI()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Close Reader") },
                                onClick = {
                                    showMenu = false
                                    onBackClick()
                                }
                            )
                        }
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = state.isUiVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    var sliderValue by remember { mutableFloatStateOf(0f) }
                    
                    LaunchedEffect(state.currentPage, state.visiblePages) {
                        sliderValue = state.visiblePages.indexOf(state.currentPage).coerceAtLeast(0).toFloat()
                    }

                    val displayPage = state.visiblePages.getOrNull(sliderValue.toInt()) ?: 0

                    Text(
                        "Page ${displayPage + 1} of ${state.document?.totalPageCount ?: 0}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${displayPage + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.width(32.dp)
                        )
                        val maxSlider = (state.visiblePages.size - 1).coerceAtLeast(0).toFloat()
                        Slider(
                            value = sliderValue,
                            onValueChange = { sliderValue = it },
                            onValueChangeFinished = {
                                val targetPage = state.visiblePages.getOrNull(sliderValue.toInt()) ?: 0
                                viewModel.onPageChanged(targetPage)
                            },
                            valueRange = 0f..maxSlider,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${state.document?.totalPageCount ?: 0}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.width(32.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    }
                }
            }
        }

        if (showDirectionDialog) {
            ReadingDirectionDialog(
                currentDirection = state.direction,
                onDirectionSelected = {
                    viewModel.onDirectionChanged(it)
                    showDirectionDialog = false
                },
                onDismiss = { showDirectionDialog = false }
            )
        }

        if (state.textSelection != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 72.dp) // Below the top app bar if visible
                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Button(onClick = {
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(state.textSelection!!.text))
                    viewModel.clearSelection()
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy")
                }
            }
        }
    }
}

@Composable
fun PdfPageDisplay(
    state: ReaderState, 
    pageIndex: Int, 
    containerWidth: androidx.compose.ui.unit.Dp, 
    containerHeight: androidx.compose.ui.unit.Dp,
    onDragStartHandle: (Int, Float, Float) -> Unit,
    onDragStopHandle: (Int, Float, Float) -> Unit,
    onLongPress: (Int, Float, Float) -> Unit,
    onTap: (Int, Float, Float) -> Unit
) {
    val isVertical = state.direction == ReadingDirection.TTB
    
    // Calculate aspect ratio based on loaded page sizes
    val pageSizes = state.document?.pageSizes
    val pdfSize = if (!pageSizes.isNullOrEmpty() && pageIndex < pageSizes.size) {
        pageSizes[pageIndex]
    } else null
    
    val aspectRatio = if (pdfSize != null) {
        pdfSize.width.toFloat() / pdfSize.height.toFloat()
    } else {
        containerWidth.value / containerHeight.value
    }

    val pageModifier = if (isVertical) {
        Modifier.fillMaxWidth().aspectRatio(aspectRatio)
    } else {
        Modifier.fillMaxHeight().aspectRatio(aspectRatio)
    }

    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = pageModifier
            .onSizeChanged { boxSize = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        if (pdfSize != null && boxSize.width > 0 && boxSize.height > 0) {
                            val pdfX = (offset.x / boxSize.width) * pdfSize.width
                            val pdfY = (offset.y / boxSize.height) * pdfSize.height
                            onTap(pageIndex, pdfX, pdfY)
                        }
                    },
                    onLongPress = { offset ->
                        if (pdfSize != null && boxSize.width > 0 && boxSize.height > 0) {
                            val pdfX = (offset.x / boxSize.width) * pdfSize.width
                            val pdfY = (offset.y / boxSize.height) * pdfSize.height
                            onLongPress(pageIndex, pdfX, pdfY)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val bitmap = state.pageCache[pageIndex]
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "PDF Page ${pageIndex + 1}",
                modifier = Modifier.fillMaxSize()
            )
            
            // Draw text selection bounds if they exist for this page
            if (state.textSelection != null && state.textSelection.pageIndex == pageIndex) {
                val startHandle = state.textSelection.startHandle
                val stopHandle = state.textSelection.stopHandle

                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()
                    .pointerInput(state.textSelection) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                if (startHandle != null && stopHandle != null && pdfSize != null && boxSize.width > 0) {
                                    val startX = (startHandle.x / pdfSize.width) * boxSize.width
                                    val startY = (startHandle.y / pdfSize.height) * boxSize.height
                                    val stopX = (stopHandle.x / pdfSize.width) * boxSize.width
                                    val stopY = (stopHandle.y / pdfSize.height) * boxSize.height
                                    val handleRadius = 24.dp.toPx()
                                    val isStart = kotlin.math.hypot(down.position.x - startX, down.position.y - startY) <= handleRadius
                                    val isStop = kotlin.math.hypot(down.position.x - stopX, down.position.y - stopY) <= handleRadius
                                    
                                    if (isStart || isStop) {
                                        down.consume()
                                        var dragEvent = awaitPointerEvent()
                                        while (dragEvent.changes.any { it.pressed }) {
                                            val change = dragEvent.changes.firstOrNull()
                                            if (change != null) {
                                                change.consume()
                                                val pdfX = (change.position.x / boxSize.width) * pdfSize.width
                                                val pdfY = (change.position.y / boxSize.height) * pdfSize.height
                                                if (isStart) onDragStartHandle(pageIndex, pdfX, pdfY)
                                                else onDragStopHandle(pageIndex, pdfX, pdfY)
                                            }
                                            dragEvent = awaitPointerEvent()
                                        }
                                    }
                                }
                            }
                        }
                    }
                ) {
                    state.textSelection.bounds.forEach { rectF ->
                        val left = (rectF.left / pdfSize!!.width) * size.width
                        val top = (rectF.top / pdfSize.height) * size.height
                        val right = (rectF.right / pdfSize.width) * size.width
                        val bottom = (rectF.bottom / pdfSize.height) * size.height
                        drawRect(
                            color = Color(0x66137EC2), // Chrome-like light blue with alpha
                            topLeft = Offset(left, top),
                            size = androidx.compose.ui.geometry.Size(right - left, bottom - top)
                        )
                    }

                    if (startHandle != null && stopHandle != null) {
                        val startX = (startHandle.x / pdfSize!!.width) * size.width
                        val startY = (startHandle.y / pdfSize.height) * size.height
                        val stopX = (stopHandle.x / pdfSize.width) * size.width
                        val stopY = (stopHandle.y / pdfSize.height) * size.height
                        
                        val r = 11.dp.toPx()
                        val handleColor = Color(0xFF137EC2) // Chrome-like solid blue
                        
                        // Left handle (start): teardrop pointing top-right
                        // Point is at (startX, startY)
                        val startCx = startX - r
                        val startCy = startY + r
                        drawCircle(color = handleColor, radius = r, center = Offset(startCx, startCy))
                        drawRect(color = handleColor, topLeft = Offset(startCx, startY), size = androidx.compose.ui.geometry.Size(r, r))
                        
                        // Right handle (stop): teardrop pointing top-left
                        // Point is at (stopX, stopY)
                        val stopCx = stopX + r
                        val stopCy = stopY + r
                        drawCircle(color = handleColor, radius = r, center = Offset(stopCx, stopCy))
                        drawRect(color = handleColor, topLeft = Offset(stopX, stopY), size = androidx.compose.ui.geometry.Size(r, r))
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.Gray)
            }
        }
    }
}

@Composable
fun ReadingDirectionDialog(
    currentDirection: ReadingDirection,
    onDirectionSelected: (ReadingDirection) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reading Direction") },
        text = {
            Column {
                ReadingDirection.values().forEach { direction ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDirectionSelected(direction) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = direction == currentDirection,
                            onClick = { onDirectionSelected(direction) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = when(direction) {
                            ReadingDirection.LTR -> "Left-to-Right"
                            ReadingDirection.RTL -> "Right-to-Left"
                            ReadingDirection.TTB -> "Top-to-Bottom"
                        })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun ReaderScreenPreview() {
    PdfDriveReaderTheme {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("PDF CONTENT PREVIEW", color = Color.White)
        }
    }
}

internal fun calculateTargetVelocity(baseVelocity: androidx.compose.ui.unit.Velocity, zoomScale: Float): androidx.compose.ui.unit.Velocity {
    // With translation strictly mapped 1:1 to screen pixels, the velocity tracker directly provides
    // the correct screen-pixel decay velocity. No scaling is needed.
    return baseVelocity
}

@kotlinx.coroutines.FlowPreview
@androidx.compose.foundation.ExperimentalFoundationApi
internal fun Modifier.readerGestures(
    state: ReaderState,
    scaleAnim: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
    getOffsetX: () -> Float,
    getOffsetY: () -> Float,
    setOffsetX: (Float) -> Unit,
    setOffsetY: (Float) -> Unit,
    getDecayJob: () -> kotlinx.coroutines.Job?,
    setDecayJob: (kotlinx.coroutines.Job?) -> Unit,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onZoomChanged: (Float) -> Unit
): Modifier = this.pointerInput(state.direction, state.textSelection != null) {
    if (state.textSelection != null) return@pointerInput
    coroutineScope.launch {
        awaitPointerEventScope {
            while (true) {
                val velocityTracker = androidx.compose.ui.input.pointer.util.VelocityTracker()
                var isPinching = false
                
                awaitFirstDown(requireUnconsumed = false)
                getDecayJob()?.cancel()

                var currentZoom = scaleAnim.value

                do {
                    val event = awaitPointerEvent()
                    val canceled = event.changes.any { it.isConsumed }
                    if (!canceled) {
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()
                        val centroid = event.calculateCentroid(useCurrent = false)

                        if (zoomChange != 1f || panChange != androidx.compose.ui.geometry.Offset.Zero) {
                            if (zoomChange != 1f) isPinching = true
                            val oldScale = currentZoom
                            val newScale = (oldScale * zoomChange).coerceIn(1f, 5f)
                            currentZoom = newScale

                            launch { scaleAnim.snapTo(newScale) }

                            val extraWidth = (newScale - 1) * size.width
                            val extraHeight = (newScale - 1) * size.height
                            val maxX = extraWidth / 2f
                            val maxY = extraHeight / 2f
                            val isHorizontalMain = state.direction != ReadingDirection.TTB

                            val targetOffsetX = if (centroid != androidx.compose.ui.geometry.Offset.Unspecified) {
                                val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                                panChange.x + (centroid.x - center.x) * (1 - newScale / oldScale) + getOffsetX() * (newScale / oldScale)
                            } else {
                                getOffsetX() + panChange.x
                            }
                            
                            val targetOffsetY = if (centroid != androidx.compose.ui.geometry.Offset.Unspecified) {
                                val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                                panChange.y + (centroid.y - center.y) * (1 - newScale / oldScale) + getOffsetY() * (newScale / oldScale)
                            } else {
                                getOffsetY() + panChange.y
                            }

                            val clampedX = targetOffsetX.coerceIn(-maxX, maxX)
                            val clampedY = targetOffsetY.coerceIn(-maxY, maxY)
                            val spillX = targetOffsetX - clampedX
                            val spillY = targetOffsetY - clampedY

                            setOffsetX(clampedX)
                            setOffsetY(clampedY)
                            
                            if (isHorizontalMain) {
                                val scrollAmount = if (state.direction == ReadingDirection.RTL) spillX else -spillX
                                listState.dispatchRawDelta(scrollAmount / newScale)
                            } else {
                                listState.dispatchRawDelta(-spillY / newScale)
                            }
                            
                            if (newScale > 1f) {
                                event.changes.forEach { 
                                    velocityTracker.addPosition(it.uptimeMillis, it.position)
                                    it.consume() 
                                }
                            }
                        }
                    }
                } while (event.changes.any { it.pressed })

                if (!isPinching && scaleAnim.value > 1f) {
                    val velocity = velocityTracker.calculateVelocity()
                    val targetVelocity = calculateTargetVelocity(velocity, scaleAnim.value)
                    val amplifiedVelocityX = targetVelocity.x
                    val amplifiedVelocityY = targetVelocity.y
                    
                    val extraWidth = (scaleAnim.value - 1) * size.width
                    val extraHeight = (scaleAnim.value - 1) * size.height
                    val maxX = extraWidth / 2f
                    val maxY = extraHeight / 2f
                    val isHorizontalMain = state.direction != ReadingDirection.TTB

                    val newJob = launch {
                        launch {
                            var lastValue = getOffsetX()
                            androidx.compose.animation.core.AnimationState(initialValue = lastValue, initialVelocity = amplifiedVelocityX).animateDecay(androidx.compose.animation.core.exponentialDecay()) {
                                val delta = value - lastValue
                                lastValue = value
                                
                                val targetX = getOffsetX() + delta
                                val clampedX = targetX.coerceIn(-maxX, maxX)
                                val spillX = targetX - clampedX
                                
                                setOffsetX(clampedX)
                                
                                if (isHorizontalMain && spillX != 0f) {
                                    val scrollAmount = if (state.direction == ReadingDirection.RTL) spillX else -spillX
                                    val consumed = listState.dispatchRawDelta(scrollAmount / scaleAnim.value)
                                    if (kotlin.math.abs(consumed) < kotlin.math.abs(scrollAmount / scaleAnim.value) - 0.5f) {
                                        cancelAnimation()
                                    }
                                } else if (!isHorizontalMain && spillX != 0f) {
                                    cancelAnimation()
                                }
                            }
                        }
                        launch {
                            var lastValue = getOffsetY()
                            androidx.compose.animation.core.AnimationState(initialValue = lastValue, initialVelocity = amplifiedVelocityY).animateDecay(androidx.compose.animation.core.exponentialDecay()) {
                                val delta = value - lastValue
                                lastValue = value
                                
                                val targetY = getOffsetY() + delta
                                val clampedY = targetY.coerceIn(-maxY, maxY)
                                val spillY = targetY - clampedY
                                
                                setOffsetY(clampedY)
                                
                                if (!isHorizontalMain && spillY != 0f) {
                                    val consumed = listState.dispatchRawDelta(-spillY / scaleAnim.value)
                                    if (kotlin.math.abs(consumed) < kotlin.math.abs(-spillY / scaleAnim.value) - 0.5f) {
                                        cancelAnimation()
                                    }
                                } else if (isHorizontalMain && spillY != 0f) {
                                    cancelAnimation()
                                }
                            }
                        }
                    }
                    setDecayJob(newJob)
                }
                if (scaleAnim.value != state.zoomLevel) {
                    onZoomChanged(scaleAnim.value)
                }
            }
        }
    }
}
