package net.wshmkr.launcher.ui.common.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.wshmkr.launcher.model.AppInfo
import net.wshmkr.launcher.ui.theme.LocalDimensions
import net.wshmkr.launcher.ui.theme.OnOpaqueSurface
import net.wshmkr.launcher.ui.theme.Spacing
import net.wshmkr.launcher.ui.theme.sheetDivider

@ExperimentalMaterial3Api
@Composable
fun AppSheet(
    appInfo: AppInfo,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    headerAction: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val dimensions = LocalDimensions.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
    ) {
        // Uncapped, the sheet's inset padding feeds its own expanded anchor and it oscillates.
        BoxWithConstraints {
            val topInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
            OnOpaqueSurface {
                Column(
                    modifier = modifier
                        .fillMaxWidth()
                        .heightIn(max = maxHeight - topInset)
                        .padding(horizontal = Spacing.medium)
                        .padding(top = 18.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = appInfo.icon,
                            contentDescription = null,
                            modifier = Modifier.size(dimensions.iconLarge)
                        )
                        Spacer(modifier = Modifier.width(Spacing.medium))
                        Text(
                            text = appInfo.label,
                            fontSize = dimensions.fontXLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        headerAction()
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = Spacing.small),
                        color = sheetDivider(),
                    )

                    content()

                    Spacer(modifier = Modifier.height(Spacing.small))
                }
            }
        }
    }
}
