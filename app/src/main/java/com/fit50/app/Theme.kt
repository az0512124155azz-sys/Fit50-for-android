package com.fit50.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Forest = Color(0xFF1C2A22)
val Forest2 = Color(0xFF263A2F)
val Cream = Color(0xFFFAF7F2)
val Orange = Color(0xFFF05A2A)
val Lime = Color(0xFFD4E85C)
val Muted = Color(0xFF6D746E)
val CardColor = Color.White

@Composable
fun Fit50Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Forest,
            secondary = Orange,
            tertiary = Lime,
            background = Cream,
            surface = CardColor,
            onPrimary = Color.White,
            onBackground = Forest,
            onSurface = Forest
        ),
        content = content
    )
}
