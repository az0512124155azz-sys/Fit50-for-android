package com.fit50.app

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(auth: AuthManager, onAuthenticated: () -> Unit, onGuest: () -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(AuthMode.LOGIN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().background(Forest).verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(46.dp))
        BrandHeader()
        Spacer(Modifier.height(24.dp))
        Card(shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    when (mode) {
                        AuthMode.LOGIN -> "ברוכים הבאים"
                        AuthMode.REGISTER -> "יוצרים חשבון"
                        AuthMode.FORGOT -> "איפוס סיסמה"
                    }, fontWeight = FontWeight.Bold, fontSize = 26.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    when (mode) {
                        AuthMode.LOGIN -> "מתחילים לזוז טוב יותר, יום אחרי יום"
                        AuthMode.REGISTER -> "כמה שניות ואתם בפנים"
                        AuthMode.FORGOT -> "נשלח קישור איפוס לכתובת האימייל"
                    }, color = Muted, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(22.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("אימייל") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (mode != AuthMode.FORGOT) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("סיסמה") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
                message?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        it,
                        color = if (it.startsWith("נשלח")) Forest2 else Orange,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = {
                        if (email.isBlank() || (mode != AuthMode.FORGOT && password.length < 6)) {
                            message = "בדקו אימייל וסיסמה של לפחות 6 תווים"
                            return@Button
                        }
                        loading = true
                        message = null
                        when (mode) {
                            AuthMode.LOGIN -> auth.signIn(email, password) { ok, err ->
                                loading = false
                                if (ok) onAuthenticated() else message = err ?: "הכניסה נכשלה"
                            }
                            AuthMode.REGISTER -> auth.register(email, password) { ok, err ->
                                loading = false
                                if (ok) onAuthenticated() else message = err ?: "ההרשמה נכשלה"
                            }
                            AuthMode.FORGOT -> auth.resetPassword(email) { ok, result ->
                                loading = false
                                message = result ?: if (ok) {
                                    "בקשת האיפוס התקבלה."
                                } else {
                                    "לא הצלחנו ליצור בקשת איפוס"
                                }
                            }
                        }
                    },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Forest),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Lime)
                    else Text(if (mode == AuthMode.FORGOT) "שלחו קישור" else if (mode == AuthMode.REGISTER) "הרשמה" else "כניסה")
                }

                if (mode == AuthMode.LOGIN) {
                    TextButton(onClick = { mode = AuthMode.FORGOT; message = null }) { Text("שכחתי את הסיסמה") }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(Modifier.weight(1f))
                        Text("  או  ", color = Muted)
                        HorizontalDivider(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            loading = true
                            message = null
                            val hostActivity = activity
                            if (hostActivity == null) {
                                loading = false
                                message = "לא ניתן לפתוח כרגע את חלון Google. נסו שוב."
                            } else {
                                scope.launch {
                                    auth.signInWithGoogle(hostActivity) { ok, err ->
                                        loading = false
                                        if (ok) onAuthenticated() else message = err
                                    }
                                }
                            }
                        },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.AccountCircle, null)
                        Spacer(Modifier.width(8.dp))
                        Text("המשך עם Google")
                    }
                    TextButton(onClick = { mode = AuthMode.REGISTER }) { Text("אין חשבון? הרשמה") }
                    TextButton(onClick = onGuest) { Text("כניסה כאורח") }
                } else {
                    TextButton(onClick = { mode = AuthMode.LOGIN; message = null }) { Text("חזרה לכניסה") }
                }

                if (!auth.configured) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Firebase עדיין לא הוגדר. ניתן לבדוק את כל האפליקציה במצב אורח.",
                        color = Muted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}


private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
