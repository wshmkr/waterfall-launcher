package net.wshmkr.launcher.ui.feature.home.widgets

import android.graphics.Bitmap
import android.media.session.MediaController
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.wshmkr.launcher.model.MediaInfo
import net.wshmkr.launcher.ui.common.icons.MusicNoteIcon
import net.wshmkr.launcher.ui.common.icons.PauseCircleIcon
import net.wshmkr.launcher.ui.common.icons.PlayCircleIcon
import net.wshmkr.launcher.ui.common.icons.SkipNextIcon
import net.wshmkr.launcher.ui.common.icons.SkipPreviousIcon


@Composable
fun MediaControls(
    mediaInfo: MediaInfo,
    mediaController: MediaController?,
    onMediaAppClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onMediaAppClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MediaAlbumArt(albumArt = mediaInfo.albumArt)

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MediaInfoDisplay(
                title = mediaInfo.title,
                artist = mediaInfo.artist
            )

            MediaControlButtons(
                isPlaying = mediaInfo.isPlaying,
                mediaController = mediaController
            )
        }
    }
}

@Composable
private fun MediaAlbumArt(albumArt: Bitmap?) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        albumArt?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Album art",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } ?: Icon(
            painter = MusicNoteIcon(),
            contentDescription = "No album art",
            tint = Color.White.copy(alpha = 0.3f),
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
private fun MediaInfoDisplay(title: String?, artist: String?) {
    Text(
        text = title ?: "No title",
        fontSize = 16.sp,
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
        fontSize = 14.sp,
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
    mediaController: MediaController?
) {
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        IconButton(
            onClick = {
                mediaController?.transportControls?.skipToPrevious()
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                painter = SkipPreviousIcon(),
                contentDescription = "Previous",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        IconButton(
            onClick = {
                mediaController?.let { controller ->
                    if (isPlaying) {
                        controller.transportControls.pause()
                    } else {
                        controller.transportControls.play()
                    }
                }
            },
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                painter = if (isPlaying) PauseCircleIcon() else PlayCircleIcon(),
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        IconButton(
            onClick = {
                mediaController?.transportControls?.skipToNext()
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                painter = SkipNextIcon(),
                contentDescription = "Next",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
