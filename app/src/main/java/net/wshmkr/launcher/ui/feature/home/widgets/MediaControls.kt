package net.wshmkr.launcher.ui.feature.home.widgets

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.delay
import net.wshmkr.launcher.model.MediaInfo
import net.wshmkr.launcher.ui.common.icons.MusicNoteIcon
import net.wshmkr.launcher.ui.common.icons.PauseIcon
import net.wshmkr.launcher.ui.common.icons.PlayArrowIcon
import net.wshmkr.launcher.ui.common.icons.SkipNextIcon
import net.wshmkr.launcher.ui.common.icons.SkipPreviousIcon
import net.wshmkr.launcher.ui.theme.Corners
import net.wshmkr.launcher.ui.theme.LocalDimensions

@Composable
fun MediaControls(
    mediaInfo: MediaInfo,
    isPlaying: Boolean,
    canSkipNext: Boolean,
    canSkipPrevious: Boolean,
    onMediaAppClick: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    val dimensions = LocalDimensions.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clip(Corners.small)
            .clickable(onClick = onMediaAppClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MediaAlbumArt(
            albumArt = mediaInfo.albumArt,
            artExpected = mediaInfo.artExpected,
            ownerPackage = mediaInfo.packageName,
        )

        Spacer(modifier = Modifier.width(dimensions.gutterSmall))

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MediaInfoDisplay(
                title = mediaInfo.title,
                artist = mediaInfo.artist
            )

            MediaControlButtons(
                isPlaying = isPlaying,
                canSkipNext = canSkipNext,
                canSkipPrevious = canSkipPrevious,
                onPlay = onPlay,
                onPause = onPause,
                onNext = onNext,
                onPrevious = onPrevious,
            )
        }
    }
}

@Composable
private fun MediaAlbumArt(albumArt: Bitmap?, artExpected: Boolean, ownerPackage: String?) {
    // Keep the previous art while the new track's art loads, instead of flashing the placeholder.
    // Keyed on the app so held art never survives a switch to a different one.
    val lastArt = remember(ownerPackage) { ArtHolder(null) }
    val displayedArt = if (albumArt == null && artExpected) lastArt.art else albumArt
    lastArt.art = displayedArt

    val context = LocalContext.current
    val appIcon = remember(ownerPackage) {
        ownerPackage?.let { runCatching { context.packageManager.getApplicationIcon(it) }.getOrNull() }
    }

    val dimensions = LocalDimensions.current
    Box(
        modifier = Modifier
            .size(dimensions.albumArtSize)
            .clip(Corners.small)
            .background(Color.White.copy(alpha = 0.1f))
    ) {
        Crossfade(targetState = displayedArt, label = "albumArt") { art ->
            if (art != null) {
                Image(
                    bitmap = art.asImageBitmap(),
                    contentDescription = "Album art",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (appIcon != null) {
                        Image(
                            painter = rememberDrawablePainter(appIcon),
                            contentDescription = "App icon",
                            modifier = Modifier.size(dimensions.playButtonSize)
                        )
                    } else {
                        Icon(
                            painter = MusicNoteIcon(),
                            contentDescription = "No album art",
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(dimensions.iconLarge)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaInfoDisplay(title: String?, artist: String?) {
    val dimensions = LocalDimensions.current
    Text(
        text = title ?: "No title",
        fontSize = dimensions.fontMedium,
        fontWeight = FontWeight.Medium,
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Left,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(2.dp))

    Text(
        text = artist ?: "Unknown artist",
        fontSize = dimensions.fontSmall,
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Left,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun MediaControlButtons(
    isPlaying: Boolean,
    canSkipNext: Boolean,
    canSkipPrevious: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    val dimensions = LocalDimensions.current
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        IconButton(
            onClick = onPrevious,
            enabled = canSkipPrevious,
            modifier = Modifier.size(dimensions.iconMedium)
        ) {
            Icon(
                painter = SkipPreviousIcon(),
                contentDescription = "Previous",
                tint = Color.White.copy(alpha = if (canSkipPrevious) 1f else 0.3f),
                modifier = Modifier.size(dimensions.iconSmall)
            )
        }

        PlayPauseButton(
            isPlaying = isPlaying,
            onPlay = onPlay,
            onPause = onPause,
        )

        IconButton(
            onClick = onNext,
            enabled = canSkipNext,
            modifier = Modifier.size(dimensions.iconMedium)
        ) {
            Icon(
                painter = SkipNextIcon(),
                contentDescription = "Next",
                tint = Color.White.copy(alpha = if (canSkipNext) 1f else 0.3f),
                modifier = Modifier.size(dimensions.iconSmall)
            )
        }
    }
}

// Isolated so play/pause flips don't invalidate the surrounding row.
@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
) {
    val dimensions = LocalDimensions.current
    // Flip the icon optimistically on tap; the real state change (or a timeout) settles it.
    var pendingPlaying by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(isPlaying) { pendingPlaying = null }
    LaunchedEffect(pendingPlaying) {
        if (pendingPlaying != null) {
            delay(PENDING_PLAYING_TIMEOUT_MS)
            pendingPlaying = null
        }
    }
    val shownPlaying = pendingPlaying ?: isPlaying

    IconButton(
        onClick = {
            // Dispatch from the shown state so a tap always means the icon the user saw.
            val startPlaying = !shownPlaying
            pendingPlaying = startPlaying
            if (startPlaying) onPlay() else onPause()
        },
        modifier = Modifier.size(dimensions.playButtonSize)
    ) {
        Icon(
            painter = if (shownPlaying) PauseIcon() else PlayArrowIcon(),
            contentDescription = if (shownPlaying) "Pause" else "Play",
            tint = Color.White,
            modifier = Modifier.size(dimensions.iconLarge)
        )
    }
}

// Nothing observes this, so snapshot state is unnecessary.
private class ArtHolder(var art: Bitmap?)

private const val PENDING_PLAYING_TIMEOUT_MS = 2_000L
