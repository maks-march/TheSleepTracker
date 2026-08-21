package com.example.sleeptracker.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.sleeptracker.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Экран обрезки фона: картинку можно двигать пальцем и масштабировать щипком,
 * рамка соответствует пропорциям экрана. Сохраняется ровно та часть,
 * что попала в рамку.
 */
@Composable
fun ImageCropperDialog(
    sourceUri: Uri,
    onCancel: () -> Unit,
    onCropped: (Bitmap) -> Unit,
) {
    val context = LocalContext.current

    var source by remember { mutableStateOf<Bitmap?>(null) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(sourceUri) {
        source = withContext(Dispatchers.IO) {
            try {
                // ужимаем до разумного размера: иначе фото на 50 Мп положит приложение
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(input, null, bounds)

                    val sample = sampleSizeFor(bounds.outWidth, bounds.outHeight, 2048)
                    context.contentResolver.openInputStream(sourceUri)?.use { retry ->
                        BitmapFactory.decodeStream(
                            retry,
                            null,
                            BitmapFactory.Options().apply { inSampleSize = sample },
                        )
                    }
                }
            } catch (e: Exception) {
                null
            }
        }
        if (source == null) failed = true
    }

    LaunchedEffect(failed) {
        if (failed) onCancel()
    }

    // положение и масштаб картинки внутри рамки
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var frameW by remember { mutableFloatStateOf(0f) }
    var frameH by remember { mutableFloatStateOf(0f) }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            Text(
                stringResource(R.string.crop_title),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(16.dp),
            )
            Text(
                stringResource(R.string.crop_hint),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(12.dp))

            val bitmap = source
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
                    .background(Color(0xFF111111))
                    .onSizeChanged {
                        frameW = it.width.toFloat()
                        frameH = it.height.toFloat()
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (bitmap == null) {
                    CircularProgressIndicator(color = Color.White)
                } else {
                    // базовый масштаб: картинка покрывает рамку целиком
                    val baseScale = remember(bitmap, frameW, frameH) {
                        if (frameW == 0f || frameH == 0f) 1f
                        else max(frameW / bitmap.width, frameH / bitmap.height)
                    }

                    androidx.compose.foundation.Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(bitmap, frameW, frameH) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 5f)

                                    // не даём утащить картинку за края рамки
                                    val drawnW = bitmap.width * baseScale * scale
                                    val drawnH = bitmap.height * baseScale * scale
                                    val maxX = ((drawnW - frameW) / 2f).coerceAtLeast(0f)
                                    val maxY = ((drawnH - frameH) / 2f).coerceAtLeast(0f)

                                    offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                                    offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                                }
                            }
                            .graphicsLayer {
                                // Image рисует картинку вписанной в рамку (Fit);
                                // domScale приводит её к «покрытию» рамки, scale — зум пальцами
                                val fitScale =
                                    minOf(frameW / bitmap.width, frameH / bitmap.height)
                                val factor =
                                    if (fitScale > 0f) baseScale * scale / fitScale else scale
                                scaleX = factor
                                scaleY = factor
                                translationX = offsetX
                                translationY = offsetY
                            },
                    )
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.crop_cancel), color = Color.White)
                }
                Button(
                    onClick = {
                        val bmp = source ?: return@Button
                        val baseScale = max(frameW / bmp.width, frameH / bmp.height)
                        val cropped = cropVisibleRegion(
                            source = bmp,
                            totalScale = baseScale * scale,
                            offsetX = offsetX,
                            offsetY = offsetY,
                            frameW = frameW,
                            frameH = frameH,
                        )
                        onCropped(cropped)
                    },
                    enabled = source != null && frameW > 0f,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.crop_apply))
                }
            }
        }
    }
}

/**
 * Вырезает из [source] ту область, которая видна в рамке.
 *
 * Экранные координаты переводятся в пиксели исходника делением на общий масштаб;
 * центр рамки совпадает с центром картинки, сдвинутым на offset.
 */
private fun cropVisibleRegion(
    source: Bitmap,
    totalScale: Float,
    offsetX: Float,
    offsetY: Float,
    frameW: Float,
    frameH: Float,
): Bitmap {
    if (totalScale <= 0f) return source

    val visibleW = frameW / totalScale
    val visibleH = frameH / totalScale

    val centerX = source.width / 2f - offsetX / totalScale
    val centerY = source.height / 2f - offsetY / totalScale

    var left = (centerX - visibleW / 2f).roundToInt()
    var top = (centerY - visibleH / 2f).roundToInt()
    var width = visibleW.roundToInt()
    var height = visibleH.roundToInt()

    // страхуемся от выхода за границы после округлений
    left = left.coerceIn(0, (source.width - 1).coerceAtLeast(0))
    top = top.coerceIn(0, (source.height - 1).coerceAtLeast(0))
    width = width.coerceIn(1, source.width - left)
    height = height.coerceIn(1, source.height - top)

    return Bitmap.createBitmap(source, left, top, width, height)
}

/** Коэффициент уменьшения, чтобы большая сторона не превышала [target]. */
private fun sampleSizeFor(width: Int, height: Int, target: Int): Int {
    var sample = 1
    var maxSide = max(width, height)
    while (maxSide / 2 >= target) {
        maxSide /= 2
        sample *= 2
    }
    return sample
}
