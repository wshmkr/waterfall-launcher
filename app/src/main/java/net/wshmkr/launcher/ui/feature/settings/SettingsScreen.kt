package net.wshmkr.launcher.ui.feature.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import net.wshmkr.launcher.MainActivity
import net.wshmkr.launcher.ui.Screen
import net.wshmkr.launcher.viewmodel.SettingsViewModel
import net.wshmkr.launcher.viewmodel.WidgetViewModel

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
    widgetViewModel: WidgetViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.setBackgroundUri(it)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Background Settings",
                fontSize = 18.sp,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Button(
                onClick = {
                    imagePickerLauncher.launch(arrayOf("image/*"))
                },
            ) {
                Text(
                    text = if (viewModel.backgroundUri != null) "Change Background" else "Select Background",
                    fontSize = 16.sp
                )
            }

            Button(
                onClick = {
                    viewModel.removeBackground()
                    navController.navigate(Screen.Home.route)
                },
                enabled = viewModel.backgroundUri != null,
            ) {
                Text(
                    text = "Remove Background",
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Widget Controls",
                fontSize = 18.sp,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Button(
                onClick = {
                    navController.navigate(Screen.WidgetList.route)
                }
            ) {
                Text(
                    text = "Add Widget",
                    fontSize = 16.sp
                )
            }

            Button(
                onClick = { widgetViewModel.removeAllWidgets() }
            ) {
                Text(
                    text = "Remove All Widgets",
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { navController.navigate(Screen.Home.route) },
            ) {
                Text(
                    text = "Close",
                    fontSize = 16.sp
                )
            }
        }
    }
}
