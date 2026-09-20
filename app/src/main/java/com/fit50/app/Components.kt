package com.fit50.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Fit50Logo(compact: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Fit",
            color = Color(0xFFF7F3EA),
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = if (compact) 42.sp else 64.sp
        )
        Text(
            text = "50",
            color = Orange,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Black,
            fontSize = if (compact) 52.sp else 82.sp
        )
        Text(
            text = "+",
            color = Color(0xFFF7F3EA),
            fontWeight = FontWeight.Bold,
            fontSize = if (compact) 28.sp else 40.sp
        )
    }
}

@Composable
fun BrandHeader(compact: Boolean = false) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Fit50Logo(compact)
    }
}

@Composable
fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Orange)
            Text(label, fontSize = 11.sp, color = Muted)
        }
    }
}

@Composable
fun TipCard(title: String, body: String) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(body, color = Muted, fontSize = 14.sp)
        }
    }
}
