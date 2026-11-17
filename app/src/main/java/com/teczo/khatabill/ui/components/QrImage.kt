package com.teczo.khatabill.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.teczo.khatabill.util.qr.QrCode

/** A QR code drawn as vector squares on a white card (always dark-on-white so every scanner can read it). */
@Composable
fun QrImage(content: String, modifier: Modifier = Modifier) {
    val modules = remember(content) { QrCode.modules(content) }
    Canvas(
        modifier.aspectRatio(1f).background(Color.White, RoundedCornerShape(16.dp)).padding(12.dp),
    ) {
        val n = modules.size
        val cell = size.width / n
        for (r in 0 until n) for (c in 0 until n) {
            if (modules[r][c]) {
                drawRect(Color.Black, Offset(c * cell, r * cell), Size(cell + 0.5f, cell + 0.5f))
            }
        }
    }
}
