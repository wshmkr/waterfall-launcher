package net.wshmkr.launcher.ui.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import net.wshmkr.launcher.ui.common.calculateCenteredContentTopPadding
import net.wshmkr.launcher.ui.theme.LocalDimensions
import net.wshmkr.launcher.ui.theme.Spacing
import net.wshmkr.launcher.ui.theme.launcherScrim
import net.wshmkr.launcher.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val dimensions = LocalDimensions.current
    val contentGutter = calculateCenteredContentTopPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(launcherScrim())
            .systemBarsPadding()
            .padding(horizontal = dimensions.pagePadding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .verticalScroll(rememberScrollState())
                // After verticalScroll so the gutters scroll with the content.
                .padding(vertical = contentGutter)
        ) {
            HomeScreenSettings(
                context = context,
                navController = navController,
                viewModel = viewModel
            )

            Spacer(modifier = Modifier.height(Spacing.xLarge))

            AppearanceSettings(viewModel = viewModel)

            Spacer(modifier = Modifier.height(Spacing.xLarge))

            PermissionSettings(context = context)
        }
    }
}
