package com.fit50.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StretchScreen() {
    var period by remember { mutableIntStateOf(0) }
    val routines = listOf(
        listOf("פתיחת חזה" to "30 שניות", "סיבובי כתפיים" to "40 שניות", "מתיחת ירך אחורית" to "40 שניות"),
        listOf("פתיחת ירך" to "40 שניות", "סיבוב גב עדין" to "30 שניות", "מתיחת תאומים" to "40 שניות"),
        listOf("נשימות עמוקות" to "60 שניות", "ברכיים לחזה" to "40 שניות", "מתיחת צוואר" to "30 שניות")
    )

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Spacer(Modifier.height(26.dp))
        Text("מתיחות יומיות", fontSize = 32.sp, fontWeight = FontWeight.Black)
        Text("תנועה קלה שמחזירה מרווח לגוף", color = Muted)
        Spacer(Modifier.height(16.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf("בוקר", "צהריים", "ערב").forEachIndexed { i, title ->
                SegmentedButton(
                    selected = period == i,
                    onClick = { period = i },
                    shape = SegmentedButtonDefaults.itemShape(i, 3)
                ) { Text(title) }
            }
        }
        Spacer(Modifier.height(18.dp))
        routines[period].forEachIndexed { i, item ->
            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp), shape = RoundedCornerShape(20.dp)) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = CircleShape,
                        color = if (i == 0) Orange else Forest
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("${i + 1}", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.first, fontWeight = FontWeight.Bold)
                        Text(item.second, color = Muted, fontSize = 13.sp)
                    }
                    Icon(Icons.Default.ChevronLeft, null, tint = Muted)
                }
            }
        }
        Spacer(Modifier.height(110.dp))
    }
}
