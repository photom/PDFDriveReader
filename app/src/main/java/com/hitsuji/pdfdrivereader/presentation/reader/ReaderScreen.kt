package com.hitsuji.pdfdrivereader.presentation.reader

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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

        val pagerState = rememberPagerState(
            initialPage = state.currentPage,
            pageCount = { state.document?.totalPageCount ?: 0 }
        )

        LaunchedEffect(pagerState.currentPage) {
            if (pagerState.currentPage != state.currentPage) {
                viewModel.onPageChanged(pagerState.currentPage)
            }
        }

        LaunchedEffect(state.currentPage) {
            if (pagerState.currentPage != state.currentPage) {
                pagerState.scrollToPage(state.currentPage)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    viewModel.toggleUI()
                }
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
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize()
                            ) { pageIndex ->
                                PdfPageDisplay(state, pageIndex)
                            }
                        }
                        ReadingDirection.RTL -> {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                                reverseLayout = true
                            ) { pageIndex ->
                                PdfPageDisplay(state, pageIndex)
                            }
                        }
                        ReadingDirection.TTB -> {
                            VerticalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize()
                            ) { pageIndex ->
                                PdfPageDisplay(state, pageIndex)
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
                title = { Text(state.document?.id?.substringAfterLast("/") ?: "Reader") },
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
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = state.pageCache[pageIndex]
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "PDF Page ${pageIndex + 1}",
                modifier = Modifier.wrapContentSize() // Preserves aspect ratio
            )
        } else {
            CircularProgressIndicator(color = Color.Gray)
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
