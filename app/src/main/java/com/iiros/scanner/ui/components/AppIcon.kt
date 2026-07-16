package com.iiros.scanner.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap

@Composable
fun AppIcon(drawable: Drawable?, modifier: Modifier = Modifier) {
    if (drawable == null) return
    val bitmap = remember(drawable) {
        val width = drawable.intrinsicWidth.coerceAtLeast(1)
        val height = drawable.intrinsicHeight.coerceAtLeast(1)
        drawable.toBitmap(width, height).asImageBitmap()
    }
    Image(bitmap = bitmap, contentDescription = null, modifier = modifier.size(40.dp))
}
