package com.fit50.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QuestionnaireScreen(onDone: () -> Unit) {
    val questions = listOf(
        "מה המטרה המרכזית שלך?" to listOf("יותר גמישות", "יותר כוח", "פחות נםקשות", "להרגיש טוב ביום-יום"),
        "כמה פעמים בשבוע תרצה להתאמן?" to listOf("2 פעמים", "3 פעמים", "4 פעמים", "5+ פעמים"),
        "כמה זמן מתאים לאימון?" to listOf("10 דקות", "15 דקות", "20 דקות", "30 דקות"),
        "איך היית מתאר את הכושר הנוכחי?" to listOf("מתחיל", "חוזר לכושר", "בינוני", "פעיל מאוד"),
        "מה חשוב לשפר?" to listOf("גב", "ברכיים", "כתפיים", "כל הגוף"),
        "מתי הכי נוח להתאמן?" to listOf("בוקר", "צהריים", "ערב", "משתנה"),
        "מה הסגנון המועדף?" to listOf("רגוע ומדויק", "קצב בינוני", "מאתגר", "שילוב")
    )
    var step by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().background(Cream).padding(24.dp)) {
        Spacer(Modifier.height(34.dp))
        BrandHeader(compact = true)
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { (step + 1) / questions.size.toFloat() },
            modifier = Modifier.fillMaxWidth(),
            color = Orange,
            trackColor = Color(0xFFE7E1D8)
        )
        Spacer(Modifier.height(22.dp))
        Text("שלב ${step + 1} מתוך ${questions.size}", color = Muted, fontSize = 13.sp)
        Text(
            questions[step].first,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp, bottom = 18.dp)
        )
        questions[step].second.forEach { option ->
            val active = selected == option
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { selected = option },
                colors = CardDefaults.cardColors(containerColor = if (active) Forest else Color.White),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    option,
                    modifier = Modifier.padding(18.dp),
                    color = if (active) Lime else Forest,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                if (step == questions.lastIndex) onDone()
                else { step++; selected = null }
            },
            enabled = selected != null,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Orange),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(if (step == questions.lastIndex) "בואו נתחיל" else "המשך", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
    }
}
