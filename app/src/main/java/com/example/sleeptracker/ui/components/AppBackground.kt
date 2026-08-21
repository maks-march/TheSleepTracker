package com.example.sleeptracker.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.io.File

/**
 * Рисует пользовательское фото под содержимым приложения.
 *
 * Поверх фото кладётся полупрозрачная заливка цветом фона темы — без неё
 * текст на пёстрой картинке нечитаем. Если фото нет, показывается обычный фон.
 */
@Composable
fun AppBackground(
    backgroundPath: String?,
    dim: Float,
    content: @Composable () -> Unit,
) {
    val bitmap: ImageBitmap? = remember(backgroundPath) {
        backgroundPath
            ?.let { path -> File(path).takeIf { it.exists() } }
            ?.let { file ->
                try {
                    // downsample: полноразмерное фото с камеры легко съедает десятки МБ
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.absolutePath, bounds)

                    val options = BitmapFactory.Options().apply {
                        inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
                    }
                    BitmapFactory.decodeFile(file.absolutePath, options)?.asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            }
    }

    Box(Modifier.fillMaxSize()) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dim.coerceIn(0f, 0.9f)))
            )
        }
        content()
    }
}

/** Ужимает картинку примерно до 1440 px по большей стороне. */
private fun calculateSampleSize(width: Int, height: Int): Int {
    val target = 1440
    var sample = 1
    var maxSide = maxOf(width, height)
    while (maxSide / 2 >= target) {
        maxSide /= 2
        sample *= 2
    }
    return sample
}

/**
 * Цвет подложки экрана: прозрачный, когда включено фоновое фото,
 * иначе обычный фон темы.
 */
@Composable
fun screenBackgroundColor(): Color =
    if (com.example.sleeptracker.settings.AppSettings.state.collectAsState().value
            .hasBackgroundImage
    ) Color.Transparent
    else androidx.compose.material3.MaterialTheme.colorScheme.background
