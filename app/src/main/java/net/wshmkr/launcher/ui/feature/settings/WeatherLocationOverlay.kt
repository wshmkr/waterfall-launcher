package net.wshmkr.launcher.ui.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.wshmkr.launcher.ui.common.components.MenuOption
import net.wshmkr.launcher.ui.common.components.VERTICAL_SWIPE_SENSITIVITY
import net.wshmkr.launcher.ui.common.components.verticalDragFeedback
import net.wshmkr.launcher.ui.common.icons.LocationOnIcon
import net.wshmkr.launcher.util.WeatherHelper
import net.wshmkr.launcher.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherLocationOverlay(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    BackHandler {
        navController.popBackStack()
    }

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<WeatherHelper.GeocodingResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            results = emptyList()
            isLoading = false
            return@LaunchedEffect
        }
        delay(400)
        isLoading = true
        results = WeatherHelper.fetchGeocodingResults(trimmedQuery)
        isLoading = false
    }

    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    var totalDragY by remember { mutableFloatStateOf(0f) }
    val offsetY = remember { Animatable(0f) }

    val dismissOverlay: () -> Unit = {
        keyboardController?.hide()
        navController.popBackStack()
    }

    val nestedScrollConnection = remember(keyboardController, listState, coroutineScope) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val isCurrentlyAtTop =
                    listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0

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
                    dismissOverlay()
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

    DisposableEffect(Unit) {
        onDispose {
            query = ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0f, 0f, 0f, 0.5f))
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
                            query = query,
                            onQueryChange = { query = it },
                            onSearch = { },
                            expanded = false,
                            onExpandedChange = { },
                            modifier = Modifier.focusRequester(focusRequester),
                            placeholder = {
                                Text(
                                    text = "Enter weather location",
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .nestedScroll(nestedScrollConnection)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    MenuOption(
                        icon = LocationOnIcon(),
                        text = "Use device location",
                        color = Color.White,
                        onClick = {
                            viewModel.clearWeatherLocation()
                            dismissOverlay()
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                } else if (query.isNotBlank() && results.isEmpty()) {
                    item {
                        Text(
                            text = "No results",
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                items(results) { result ->
                    MenuOption(
                        text = result.name,
                        subtext = "${result.admin1}, ${result.country}",
                        color = Color.White,
                        onClick = {
                            viewModel.setWeatherLocation(
                                name = result.displayName,
                                latitude = result.latitude,
                                longitude = result.longitude
                            )
                            dismissOverlay()
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
