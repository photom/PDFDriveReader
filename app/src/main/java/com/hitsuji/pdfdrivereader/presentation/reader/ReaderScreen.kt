package com.hitsuji.pdfdrivereader.presentation.reader

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.hitsuji.pdfdrivereader.domain.model.PdfDocument
import com.hitsuji.pdfdrivereader.domain.model.ReadingDirection
import com.hitsuji.pdfdrivereader.presentation.theme.PdfDriveReaderTheme

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
        
        LaunchedEffect(widthPx, heightPx) {
            viewModel.updateScreenDimensions(widthPx, heightPx)
        }

        // Zoom and Pan state
        var scale by remember { mutableFloatStateOf(state.zoomLevel) }
        var offset by remember { mutableStateOf(Offset.Zero) }

        val currentScale by rememberUpdatedState(scale)
        val currentZoomLevel by rememberUpdatedState(state.zoomLevel)

        // Sync local scale with state when state changes (e.g. on load or reset)
        LaunchedEffect(state.zoomLevel) {
            if (scale != state.zoomLevel) {
                scale = state.zoomLevel
            }
        }

        val listState = rememberLazyListState(initialFirstVisibleItemIndex = state.currentPage)

        // Sync list scroll back to ViewModel page index for the slider/UI
        // Use snapshotFlow to avoid excessive triggers and only sync if not already matching
        LaunchedEffect(listState) {
            snapshotFlow { listState.firstVisibleItemIndex }
                .collect { index ->
                    if (index != state.currentPage) {
                        viewModel.onPageChanged(index)
                    }
                }
        }

        // Sync ViewModel page index changes back to list scroll
        // ONLY if the change did NOT originate from the listState itself (e.g. from slider)
        // We can check if listState is currently being scrolled to distinguish
        LaunchedEffect(state.currentPage) {
            if (!listState.isScrollInProgress && listState.firstVisibleItemIndex != state.currentPage) {
                listState.scrollToItem(state.currentPage)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            scale = 1f
                            offset = Offset.Zero
                            viewModel.resetZoom()
                        },
                        onTap = {
                            viewModel.toggleUI()
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val oldScale = scale
                        val newScale = (scale * zoom).coerceIn(1f, 5f)
                        
                        // To keep the centroid stationary:
                        // The relative position of the centroid within the scaled content must stay the same.
                        // (centroid - oldOffset) / oldScale == (centroid - newOffset) / newScale
                        // Solving for newOffset:
                        val newOffset = centroid - (centroid - offset - pan * oldScale) * (newScale / oldScale)

                        scale = newScale
                        
                        // Bounds calculation to prevent panning too far
                        val extraWidth = (newScale - 1) * size.width
                        val extraHeight = (newScale - 1) * size.height
                        val maxX = extraWidth / 2f
                        val maxY = extraHeight / 2f

                        offset = Offset(
                            newOffset.x.coerceIn(-maxX, maxX),
                            newOffset.y.coerceIn(-maxY, maxY)
                        )
                    }
                }
                .pointerInput(Unit) {
                    // Detect end of transformation to trigger high-quality re-render
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.all { !it.pressed }) {
                                if (currentScale != currentZoomLevel) {
                                    viewModel.onZoomChanged(currentScale)
                                }
                            }
                        }
                    }
                }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 32.dp) // Extra padding for high-zoom panning
                            ) {
                                items(state.document?.totalPageCount ?: 0) { index ->
                                    PdfPageDisplay(state, index)
                                }
                            }
                        }
                        ReadingDirection.RTL -> {
                            LazyRow(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                reverseLayout = true,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 32.dp)
                            ) {
                                items(state.document?.totalPageCount ?: 0) { index ->
                                    PdfPageDisplay(state, index)
                                }
                            }
                        }
                        ReadingDirection.TTB -> {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 32.dp)
                            ) {
                                items(state.document?.totalPageCount ?: 0) { index ->
                                    PdfPageDisplay(state, index)
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
                    
                    // Keep local slider synced with state when NOT dragging
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
fun PdfPageDisplay(state: ReaderState, pageIndex: Int) {
    val isVertical = state.direction == ReadingDirection.TTB
    Box(
        modifier = if (isVertical) {
            Modifier.fillMaxWidth().wrapContentHeight()
        } else {
            Modifier.fillMaxHeight().wrapContentWidth()
        },
        contentAlignment = Alignment.Center
    ) {
        val bitmap = state.pageCache[pageIndex]
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "PDF Page ${pageIndex + 1}",
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight() // Important for TTB concatenation
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp), // Placeholder height while loading next page
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
