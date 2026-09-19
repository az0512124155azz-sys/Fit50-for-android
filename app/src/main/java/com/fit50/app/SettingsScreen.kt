package com.fit50.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    firebaseConfigured: Boolean,
    isGuest: Boolean,
    onRedoQuestionnaire: () -> Unit,
    onLogout: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Spacer(Modifier.height(26.dp))
        Text("הגדרות", fontSize = 32.sp, fontWeight = FontWeight.Black)
        Text("Fit50+", color = Orange, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        SettingRow(Icons.Default.Person, "חשבון", if (isGuest) "מצב אורח" else "מחובר")
        SettingRow(Icons.Default.CloudDone, "Firebase", if (firebaseConfigured) "מוגדר" else "דורש הגדרה")
        SettingRow(Icons.Default.Refresh, "בניית תוכנית מחדש", "חזרה לשאלון", onRedoQuestionnaire)
        Spacer(Modifier.height(14.dp))
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("בריאות ובטיחות", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "האפליקציה מיועדת לכושר כללי ואינה תחליף לייעוץ רפואי. במקרה של כאב חד יש לעצור ולהתייעץ עם איש מקצוע.",
                    color = Muted,
                    fontSize = 13.sp
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Logout, null)
            Spacer(Modifier.width(8.dp))
            Text("התנתקות")
        }
        Spacer(Modifier.height(110.dp))
    }
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, subtitle: String, onClick: (() -> Unit)? = null) {
    val modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 5.dp)
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    Card(modifier, shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(42.dp).clip(CircleShape),
                shape = CircleShape,
                color = Forest.copy(.08f)
            ) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = Forest) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Muted, fontSize = 12.sp)
            }
            if (onClick != null) Icon(Icons.Default.ChevronLeft, null, tint = Muted)
        }
    }
}
