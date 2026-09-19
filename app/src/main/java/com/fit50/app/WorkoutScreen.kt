package com.fit50.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun WorkoutScreen(exercises: List<Exercise>, onBack: () -> Unit, onFinished: () -> Unit) {
    var index by remember { mutableIntStateOf(0) }
    var remaining by remember { mutableIntStateOf(exercises.first().seconds) }
    var running by remember { mutableStateOf(false) }
    var rest by remember { mutableStateOf(false) }
    val current = exercises[index]

    LaunchedEffect(running, remaining) {
        if (running && remaining > 0) {
            delay(1000)
            remaining--
        } else if (running && remaining == 0) {
            running = false
        }
    }

    Column(
        Modifier.fillMaxSize().background(Forest).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.Close, null, tint = Color.White) }
            Text(
                "אימון ${index + 1}/${exercises.size}",
                color = Color.White,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(48.dp))
        }
        Spacer(Modifier.height(26.dp))
        Text(
            if (rest) "מנוחה קצרה" else current.name,
            color = Lime,
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (rest) "נשימה עמוקה, מים אם צריך" else current.subtitle,
            color = Color.White.copy(.7f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(40.dp))
        Box(
            Modifier.size(210.dp).clip(CircleShape).background(Color.White.copy(.08f)),
            contentAlignment = Alignment.Center
        ) {
            Text(String.format("%02d", remaining), color = Color.White, fontSize = 70.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(34.dp))
        Button(
            onClick = { running = !running },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Orange),
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(if (running) Icons.Default.Pause else Icons.Default.PlayArrow, null)
            Spacer(Modifier.width(8.dp))
            Text(if (running) "השהה" else "התחל", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = {
                if (!rest && index < exercises.lastIndex) {
                    rest = true
                    running = false
                    remaining = 20
                } else if (rest) {
                    rest = false
                    index++
                    running = false
                    remaining = exercises[index].seconds
                } else {
                    onFinished()
                }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(if (!rest && index == exercises.lastIndex) "סיום אימון" else if (rest) "לתרגיל הבא" else "סיימתי את התרגיל")
        }
        Spacer(Modifier.weight(1f))
        Text(
            "אפשר לעבוד בקצב שלך. איכות התנועה חשובה יותר מהמהירות.",
            color = Color.White.copy(.5f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
    }
}
