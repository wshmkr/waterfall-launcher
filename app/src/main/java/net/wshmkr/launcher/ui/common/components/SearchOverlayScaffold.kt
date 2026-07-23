package net.wshmkr.launcher.ui.common.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private val ScrimColor = Color(0f, 0f, 0f, 0.5f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchOverlayScaffold(
    query: () -> String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    onDismiss: () -> Unit,
    onSearch: (String) -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    val offsetY = remember { Animatable(0f) }

    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val dismissOverlay = {
        keyboardController?.hide()
        currentOnDismiss()
    }
    val currentDismissOverlay by rememberUpdatedState(dismissOverlay)

    BackHandler {
        currentDismissOverlay()
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            var totalDragY = 0f

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val isCurrentlyAtTop = listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset == 0

                if (isCurrentlyAtTop && available.y > 0) {
                    totalDragY += available.y
                    coroutineScope.launch {
                        offsetY.snapTo(totalDragY)
                    }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                return if (totalDragY > 0) available else Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (totalDragY > VERTICAL_SWIPE_SENSITIVITY) {
                    currentDismissOverlay()
                } else if (totalDragY > 0) {
                    offsetY.animateTo(
                        targetValue = 0f,
                        animationSpec = spring()
                    )
                }
                totalDragY = 0f
                return Velocity.Zero
            }
        }
    }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScrimColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = verticalDragFeedback(offsetY.value)
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    initialOffsetY = { fullHeight -> -fullHeight }
                ),
            ) {
                SearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = query(),
                            onQueryChange = onQueryChange,
                            onSearch = onSearch,
                            expanded = false,
                            onExpandedChange = { },
                            modifier = Modifier.focusRequester(focusRequester),
                            placeholder = {
                                Text(
                                    text = placeholder,
                                    color = Color.Gray
                                )
                            }
                        )
                    },
                    expanded = false,
                    onExpandedChange = { },
                    modifier = Modifier.padding(vertical = 8.dp)
                ) { }
            }
            LazyColumn(
                state = listState,
                contentPadding = WindowInsets.ime
                    .union(WindowInsets.navigationBars)
                    .asPaddingValues(),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .nestedScroll(nestedScrollConnection)
            ) {
                content()
            }
        }
    }
}
