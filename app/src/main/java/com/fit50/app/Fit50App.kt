package com.fit50.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Fit50App() {
    val context = LocalContext.current
    val prefs = remember { Fit50Prefs(context) }
    val auth = remember { AuthManager(context) }
    var screen by remember {
        mutableStateOf(
            if (prefs.accountSignedIn || prefs.guest) {
                if (prefs.onboardingDone) Screen.HOME else Screen.QUESTIONNAIRE
            } else {
                Screen.AUTH
            }
        )
    }

    Fit50Theme {
        Surface(color = Cream) {
            when (screen) {
                Screen.AUTH -> AuthScreen(
                    auth = auth,
                    onAuthenticated = {
                        prefs.guest = false
                        prefs.accountSignedIn = true
                        screen = if (prefs.onboardingDone) Screen.HOME else Screen.QUESTIONNAIRE
                    },
                    onGuest = {
                        prefs.guest = true
                        prefs.accountSignedIn = false
                        screen = if (prefs.onboardingDone) Screen.HOME else Screen.QUESTIONNAIRE
                    }
                )

                Screen.QUESTIONNAIRE -> QuestionnaireScreen {
                    prefs.onboardingDone = true
                    screen = Screen.HOME
                }

                Screen.WORKOUT -> WorkoutScreen(
                    exercises = dailyWorkout,
                    onBack = { screen = Screen.HOME },
                    onFinished = {
                        prefs.completed += 1
                        prefs.minutes += 4
                        prefs.streak = (prefs.streak + 1).coerceAtMost(30)
                        screen = Screen.HOME
                    }
                )

                Screen.HOME,
                Screen.PROGRESS,
                Screen.STRETCH,
                Screen.SETTINGS -> {
                    val selectedTab = when (screen) {
                        Screen.HOME -> 0
                        Screen.PROGRESS -> 1
                        Screen.STRETCH -> 2
                        else -> 3
                    }

                    Scaffold(
                        containerColor = Cream,
                        bottomBar = {
                            NavigationBar(containerColor = Forest, tonalElevation = 0.dp) {
                                val items = listOf(
                                    Triple("בית", Icons.Default.Home, Screen.HOME),
                                    Triple("התקדמות", Icons.Default.ShowChart, Screen.PROGRESS),
                                    Triple("מתיחות", Icons.Default.SelfImprovement, Screen.STRETCH),
                                    Triple("הגדרות", Icons.Default.Settings, Screen.SETTINGS)
                                )
                                items.forEachIndexed { index, item ->
                                    NavigationBarItem(
                                        selected = selectedTab == index,
                                        onClick = { screen = item.third },
                                        icon = { Icon(item.second, null) },
                                        label = { Text(item.first, fontSize = 10.sp) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Lime,
                                            selectedTextColor = Lime,
                                            unselectedIconColor = Color.White.copy(.55f),
                                            unselectedTextColor = Color.White.copy(.55f),
                                            indicatorColor = Color.White.copy(.08f)
                                        )
                                    )
                                }
                            }
                        }
                    ) { paddingValues ->
                        Box(Modifier.padding(paddingValues)) {
                            when (screen) {
                                Screen.HOME -> HomeScreen(auth.displayName(), prefs) { screen = Screen.WORKOUT }
                                Screen.PROGRESS -> ProgressScreen(prefs)
                                Screen.STRETCH -> StretchScreen()
                                Screen.SETTINGS -> SettingsScreen(
                                    firebaseConfigured = auth.configured,
                                    isGuest = prefs.guest,
                                    onRedoQuestionnaire = { screen = Screen.QUESTIONNAIRE },
                                    onLogout = {
                                        auth.signOut()
                                        prefs.guest = false
                                        prefs.accountSignedIn = false
                                        screen = Screen.AUTH
                                    }
                                )
                                else -> Unit
                            }
                        }
                    }
                }
            }
        }
    }
}
