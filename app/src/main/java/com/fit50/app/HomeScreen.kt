package com.fit50.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(name: String, prefs: Fit50Prefs, onStart: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Spacer(Modifier.height(26.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("שלום, $name", fontSize = 14.sp, color = Muted)
                Text("המסע שלך", fontSize = 32.sp, fontWeight = FontWeight.Black)
            }
            Fit50Logo(compact = true)
        }
        Spacer(Modifier.height(18.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Forest), shape = RoundedCornerShape(28.dp)) {
            Column(Modifier.padding(22.dp)) {
                Text("האימון של היום", color = Lime, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("כוח + תנועה", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("5 תרגילים • כ־4 דקות • ללא ציוד", color = Color.White.copy(.65f))
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(containerColor = Orange),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(6.dp))
                    Text("התחל אימון", fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("${prefs.completed}", "אימונים", Modifier.weight(1f))
            StatCard("${prefs.minutes}", "דקות", Modifier.weight(1f))
            StatCard("${prefs.streak}", "רצף", Modifier.weight(1f))
        }
        Spacer(Modifier.height(18.dp))
        Text("היום בשבילך", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        TipCard("תנועה קטנה עדיפה על אפס תנועה", "גם 5 דקות היום הן השקעה אמיתית בגוף שלך.")
        Spacer(Modifier.height(110.dp))
    }
}
