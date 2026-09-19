package com.fit50.app

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProgressScreen(prefs: Fit50Prefs) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Spacer(Modifier.height(26.dp))
        Text("ההתקדמות שלך", fontSize = 32.sp, fontWeight = FontWeight.Black)
        Text("כל אימון קטן מצטבר", color = Muted)
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("${prefs.completed}", "אימונים", Modifier.weight(1f))
            StatCard("${prefs.minutes}", "דקות", Modifier.weight(1f))
            StatCard("${prefs.streak}", "רצף", Modifier.weight(1f))
        }
        Spacer(Modifier.height(18.dp))
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Forest)) {
            Column(Modifier.padding(20.dp)) {
                Text("יעד שבועי", color = Lime, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { (prefs.completed % 4) / 4f },
                    modifier = Modifier.fillMaxWidth(),
                    color = Orange,
                    trackColor = Color.White.copy(.12f)
                )
                Spacer(Modifier.height(8.dp))
                Text("${prefs.completed % 4} מתוך 4 אימונים", color = Color.White.copy(.8f))
            }
        }
        Spacer(Modifier.height(18.dp))
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("הישג אחרון", fontWeight = FontWeight.Bold)
                Text(
                    if (prefs.completed > 0) "כבר התחלת לבנות שגרה. המשך כך." else "האימון הראשון שלך מחכה לך.",
                    color = Muted
                )
                Spacer(Modifier.height(14.dp))
                OutlinedButton(
                    onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "סיימתי ${prefs.completed} אימונים ב-Fit50+ 💪")
                        }
                        context.startActivity(Intent.createChooser(send, "שיתוף ההתקדמות"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, null)
                    Spacer(Modifier.width(8.dp))
                    Text("שתף התקדמות")
                }
            }
        }
        Spacer(Modifier.height(110.dp))
    }
}
