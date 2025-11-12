package net.wshmkr.launcher.ui.feature.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import net.wshmkr.launcher.ui.Screen
import net.wshmkr.launcher.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
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
