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
import com.hitsuji.pdfdrivereader.domain.model.ReadingDirection
import com.hitsuji.pdfdrivereader.presentation.theme.PdfDriveReaderTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showDirectionDialog by remember { mutableStateOf(false) }

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
        val offsetXAnim = remember { Animatable(0f) }
        val offsetYAnim = remember { Animatable(0f) }
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(state.zoomLevel) {
            if (scaleAnim.value != state.zoomLevel) {
                scaleAnim.snapTo(state.zoomLevel)
            }
        }

        val listState = rememberLazyListState(initialFirstVisibleItemIndex = state.currentPage)

        LaunchedEffect(listState) {
            snapshotFlow { listState.firstVisibleItemIndex }
                .collect { index ->
                    if (index != state.currentPage) {
                        viewModel.onPageChanged(index)
                    }
                }
        }

        LaunchedEffect(state.currentPage) {
            if (!listState.isScrollInProgress && listState.firstVisibleItemIndex != state.currentPage) {
                listState.scrollToItem(state.currentPage)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state.direction) {
                    coroutineScope {
                        awaitEachGesture {
                            val velocityTracker = VelocityTracker()
                            var isPinching = false
                            
                            awaitFirstDown(requireUnconsumed = false)
                            launch { 
                                offsetXAnim.stop()
                                offsetYAnim.stop()
                            }

                            do {
                                val event = awaitPointerEvent()
                                val canceled = event.changes.any { it.isConsumed }
                                if (!canceled) {
                                    val zoomChange = event.calculateZoom()
                                    val panChange = event.calculatePan()
                                    val centroid = event.calculateCentroid(useCurrent = false)

                                    if (zoomChange != 1f || panChange != Offset.Zero) {
                                        if (zoomChange != 1f) isPinching = true
                                        val oldScale = scaleAnim.value
                                        val newScale = (oldScale * zoomChange).coerceIn(1f, 5f)
                                        launch { scaleAnim.snapTo(newScale) }

                                        val extraWidth = (newScale - 1) * size.width
                                        val extraHeight = (newScale - 1) * size.height
                                        val maxX = extraWidth / 2f
                                        val maxY = extraHeight / 2f
                                        val isHorizontalMain = state.direction != ReadingDirection.TTB

                                        if (centroid != Offset.Unspecified) {
                                            val targetOffsetX = centroid.x - (centroid.x - offsetXAnim.value - panChange.x * oldScale) * (newScale / oldScale)
                                            val targetOffsetY = centroid.y - (centroid.y - offsetYAnim.value - panChange.y * oldScale) * (newScale / oldScale)
                                            val clampedX = targetOffsetX.coerceIn(-maxX, maxX)
                                            val clampedY = targetOffsetY.coerceIn(-maxY, maxY)
                                            val spillX = targetOffsetX - clampedX
                                            val spillY = targetOffsetY - clampedY

                                            launch {
                                                offsetXAnim.snapTo(clampedX)
                                                offsetYAnim.snapTo(clampedY)
                                                if (isHorizontalMain) {
                                                    val scrollAmount = if (state.direction == ReadingDirection.RTL) spillX else -spillX
                                                    listState.scrollBy(scrollAmount)
                                                } else {
                                                    listState.scrollBy(-spillY)
                                                }
                                            }
                                        } else {
                                            val targetOffsetX = offsetXAnim.value + panChange.x * oldScale
                                            val targetOffsetY = offsetYAnim.value + panChange.y * oldScale
                                            val clampedX = targetOffsetX.coerceIn(-maxX, maxX)
                                            val clampedY = targetOffsetY.coerceIn(-maxY, maxY)
                                            val spillX = targetOffsetX - clampedX
                                            val spillY = targetOffsetY - clampedY

                                            launch {
                                                offsetXAnim.snapTo(clampedX)
                                                offsetYAnim.snapTo(clampedY)
                                                if (isHorizontalMain) {
                                                    val scrollAmount = if (state.direction == ReadingDirection.RTL) spillX else -spillX
                                                    listState.scrollBy(scrollAmount)
                                                } else {
                                                    listState.scrollBy(-spillY)
                                                }
                                            }
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
                                val extraWidth = (scaleAnim.value - 1) * size.width
                                val extraHeight = (scaleAnim.value - 1) * size.height
                                val maxX = extraWidth / 2f
                                val maxY = extraHeight / 2f
                                val isHorizontalMain = state.direction != ReadingDirection.TTB

                                launch {
                                    offsetXAnim.animateDecay(initialVelocity = velocity.x, animationSpec = exponentialDecay()) {
                                        val clampedValue = value.coerceIn(-maxX, maxX)
                                        val overscroll = value - clampedValue
                                        if (overscroll != 0f) {
                                            launch { 
                                                offsetXAnim.snapTo(clampedValue)
                                                if (isHorizontalMain) {
                                                    val scrollAmount = if (state.direction == ReadingDirection.RTL) overscroll else -overscroll
                                                    listState.scrollBy(scrollAmount)
                                                }
                                            }
                                        }
                                    }
                                }
                                launch {
                                    offsetYAnim.animateDecay(initialVelocity = velocity.y, animationSpec = exponentialDecay()) {
                                        val clampedValue = value.coerceIn(-maxY, maxY)
                                        val overscroll = value - clampedValue
                                        if (overscroll != 0f) {
                                            launch { 
                                                offsetYAnim.snapTo(clampedValue)
                                                if (!isHorizontalMain) {
                                                    listState.scrollBy(-overscroll)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (scaleAnim.value != state.zoomLevel) {
                                viewModel.onZoomChanged(scaleAnim.value)
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            coroutineScope.launch {
                                launch { scaleAnim.animateTo(1f) }
                                launch { offsetXAnim.animateTo(0f) }
                                launch { offsetYAnim.animateTo(0f) }
                                viewModel.resetZoom()
                            }
                        },
                        onTap = {
                            viewModel.toggleUI()
                        }
                    )
                }
                .graphicsLayer(
                    scaleX = scaleAnim.value,
                    scaleY = scaleAnim.value,
                    translationX = offsetXAnim.value,
                    translationY = offsetYAnim.value
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
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(state.document?.totalPageCount ?: 0) { index ->
                                    PdfPageDisplay(state, index, containerMaxWidth, containerMaxHeight)
                                }
                            }
                        }
                        ReadingDirection.RTL -> {
                            LazyRow(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                reverseLayout = true,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(state.document?.totalPageCount ?: 0) { index ->
                                    PdfPageDisplay(state, index, containerMaxWidth, containerMaxHeight)
                                }
                            }
                        }
                        ReadingDirection.TTB -> {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(state.document?.totalPageCount ?: 0) { index ->
                                    PdfPageDisplay(state, index, containerMaxWidth, containerMaxHeight)
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
                    var sliderValue by remember { mutableFloatStateOf(state.currentPage.toFloat()) }
                    
                    LaunchedEffect(state.currentPage) {
                        sliderValue = state.currentPage.toFloat()
                    }

                    Text(
                        "Page ${sliderValue.toInt() + 1} of ${state.document?.totalPageCount ?: 0}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${sliderValue.toInt() + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.width(32.dp)
                        )
                        Slider(
                            value = sliderValue,
                            onValueChange = { sliderValue = it },
                            onValueChangeFinished = {
                                viewModel.onPageChanged(sliderValue.toInt())
                            },
                            valueRange = 0f..(state.document?.totalPageCount?.minus(1)?.toFloat() ?: 0f),
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
    }
}

@Composable
fun PdfPageDisplay(state: ReaderState, pageIndex: Int, containerWidth: androidx.compose.ui.unit.Dp, containerHeight: androidx.compose.ui.unit.Dp) {
    val isVertical = state.direction == ReadingDirection.TTB
    
    // Calculate aspect ratio based on loaded page sizes
    val pageSizes = state.document?.pageSizes
    val aspectRatio = if (!pageSizes.isNullOrEmpty() && pageIndex < pageSizes.size) {
        val size = pageSizes[pageIndex]
        size.width.toFloat() / size.height.toFloat()
    } else {
        containerWidth.value / containerHeight.value
    }

    val pageModifier = if (isVertical) {
        Modifier.fillMaxWidth().aspectRatio(aspectRatio)
    } else {
        Modifier.fillMaxHeight().aspectRatio(aspectRatio)
    }

    Box(
        modifier = pageModifier,
        contentAlignment = Alignment.Center
    ) {
        val bitmap = state.pageCache[pageIndex]
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "PDF Page ${pageIndex + 1}",
                modifier = Modifier.fillMaxSize()
            )
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
