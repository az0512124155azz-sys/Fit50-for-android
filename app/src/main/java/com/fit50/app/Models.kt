package com.fit50.app

enum class Screen { AUTH, QUESTIONNAIRE, HOME, WORKOUT, PROGRESS, STRETCH, SETTINGS }
enum class AuthMode { LOGIN, REGISTER, FORGOT }

data class Exercise(val name: String, val subtitle: String, val seconds: Int)

val dailyWorkout = listOf(
    Exercise("סקוואט לכיסא", "שליטה, ירידה איטית ועלייה חזקה", 40),
    Exercise("דחיפה לקיר", "חזה, כתפיים וליבה", 35),
    Exercise("גשר ישבן", "ישבן, ירך אחורית ויציבות", 40),
    Exercise("Bird Dog", "ליבה ושיווי משקל", 35),
    Exercise("צעידה במקום", "קצב נוח וסיום אנרגטי", 45)
)
