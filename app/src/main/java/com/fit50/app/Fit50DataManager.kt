package com.fit50.app

import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class Fit50DataManager(private val context: Context) {
    private val auth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseFirestore.getInstance()
    private val prefs = context.getSharedPreferences("fit50_real_data", Context.MODE_PRIVATE)

    private fun uid(): String? = auth.currentUser?.uid
    private fun userDoc() = uid()?.let { db.collection("users").document(it) }
    private fun workouts() = uid()?.let { db.collection("users").document(it).collection("workouts") }

    fun bootstrapUser(done: (Boolean, String?) -> Unit) {
        val user = auth.currentUser ?: return done(false, "אין משתמש מחובר")
        val ref = userDoc() ?: return done(false, "אין משתמש מחובר")
        val base = hashMapOf<String, Any?>(
            "uid" to user.uid,
            "email" to user.email,
            "displayName" to (user.displayName ?: user.email?.substringBefore("@") ?: "מתאמן"),
            "lastSeenAt" to FieldValue.serverTimestamp()
        )
        ref.set(base, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener { done(true, null) }
            .addOnFailureListener { done(false, it.localizedMessage) }
    }


    private fun onboardingKey(): String? = uid()?.let { "onboardingComplete_$it" }

    fun localStartPage(): String {
        val key = onboardingKey()
        return if (key != null && prefs.getBoolean(key, false)) "home" else "questionnaire"
    }

    fun resolveStartPage(done: (String) -> Unit) {
        if (localStartPage() == "home") {
            done("home")
            return
        }

        val ref = userDoc() ?: return done("questionnaire")
        ref.get()
            .addOnSuccessListener { snap ->
                val questionnaire = snap.get("questionnaire") as? Map<*, *>
                val completed =
                    snap.getBoolean("onboardingComplete") == true ||
                    !questionnaire.isNullOrEmpty()

                if (completed) {
                    onboardingKey()?.let { key ->
                        prefs.edit().putBoolean(key, true).apply()
                    }

                    if (snap.getBoolean("onboardingComplete") != true) {
                        ref.set(
                            mapOf("onboardingComplete" to true),
                            com.google.firebase.firestore.SetOptions.merge()
                        )
                    }
                }

                done(if (completed) "home" else "questionnaire")
            }
            .addOnFailureListener {
                done(localStartPage())
            }
    }

    fun saveQuestionnaire(json: String, done: (Boolean, String?) -> Unit) {
        val ref = userDoc() ?: return done(false, "יש להתחבר לחשבון")
        val data = jsonObjectToMap(JSONObject(json))
        data["completedAt"] = Timestamp.now()
        val editor = prefs.edit().putString("questionnaire", json)
        onboardingKey()?.let { key -> editor.putBoolean(key, true) }
        editor.apply()

        ref.set(
            mapOf(
                "questionnaire" to data,
                "onboardingComplete" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            com.google.firebase.firestore.SetOptions.merge()
        ).addOnSuccessListener { done(true, null) }
         .addOnFailureListener { done(false, it.localizedMessage) }
    }

    fun getQuestionnaire(done: (Boolean, String?, JSONObject?) -> Unit) {
        val ref = userDoc() ?: return done(false, "יש להתחבר לחשבון", null)
        fun cached() = prefs.getString("questionnaire", null)?.let { runCatching { JSONObject(it) }.getOrNull() }
        ref.get()
            .addOnSuccessListener { snap ->
                val saved = snap.get("questionnaire") as? Map<*, *>
                val fields = saved?.entries?.mapNotNull { (key, value) ->
                    (key as? String)?.takeIf { it != "completedAt" }?.let { it to value }
                }?.toMap()
                done(true, null, if (!fields.isNullOrEmpty()) JSONObject(fields) else cached() ?: JSONObject())
            }
            .addOnFailureListener { error ->
                val local = cached()
                done(local != null, if (local == null) error.localizedMessage else null, local)
            }
    }

    fun savePausedWorkout(json: String) {
        prefs.edit().putString("pausedWorkout", json).apply()
    }

    fun getPausedWorkout(): String =
        prefs.getString("pausedWorkout", "").orEmpty()

    fun clearPausedWorkout() {
        prefs.edit().remove("pausedWorkout").apply()
    }

    fun completeWorkout(json: String, done: (Boolean, String?, JSONObject?) -> Unit) {
        val currentUid = uid() ?: return done(false, "אין משתמש מחובר", null)
        val collection = workouts() ?: return done(false, "יש להתחבר לחשבון", null)
        val obj = JSONObject(json)
        val now = Date()
        val cal = Calendar.getInstance()
        val dayIndex = cal.get(Calendar.DAY_OF_WEEK) - 1
        val dateKey = dayKey(now)
        val defaultPlanKey = "w1d" + (dayIndex + 1) + "-" + dateKey
        val planKey = obj.optString("planKey").ifBlank { defaultPlanKey }
        val completedIds = obj.optJSONArray("exerciseIds")?.let { array ->
            (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
        }.orEmpty()

        val data = hashMapOf<String, Any?>(
            "title" to obj.optString("title", "אימון"),
            "durationMins" to obj.optInt("durationMins", 0).coerceAtLeast(0),
            "exerciseCount" to obj.optInt("exerciseCount", 0).coerceAtLeast(0),
            "exerciseIds" to completedIds,
            "planKey" to planKey,
            "dateKey" to dateKey,
            "activityType" to obj.optString("activityType", "workout"),
            "completedAt" to Timestamp.now()
        )

        collection.add(data)
            .addOnSuccessListener {
                if (completedIds.isNotEmpty()) {
                    val key = "recentWorkouts:$currentUid"
                    val previous = runCatching { JSONArray(prefs.getString(key, "[]")) }.getOrDefault(JSONArray())
                    val recent = JSONArray().put(JSONObject().put("date", dateKey).put("ids", JSONArray(completedIds)))
                    for (index in 0 until minOf(2, previous.length())) previous.optJSONObject(index)?.let { recent.put(it) }
                    prefs.edit().putString(key, recent.toString()).apply()
                }
                buildProgress { ok, error, progress ->
                    done(ok, error, progress)
                }
            }
            .addOnFailureListener { done(false, it.localizedMessage, null) }
    }

    fun getDashboard(done: (Boolean, String?, JSONObject?) -> Unit) {
        val cached = prefs.getString("dashboard", null)
        if (cached != null) runCatching { done(true, null, JSONObject(cached)) }

        val ref = userDoc() ?: return done(false, "יש להתחבר לחשבון", null)
        ref.get().addOnSuccessListener { snap ->
            buildProgress { ok, error, progress ->
                if (!ok || progress == null) return@buildProgress done(false, error, null)

                val user = auth.currentUser
                val profileName = snap.getString("displayName")
                    ?: user?.displayName
                    ?: user?.email?.substringBefore("@")
                    ?: "מתאמן"
                val email = snap.getString("email") ?: user?.email.orEmpty()
                val questionnaire = snap.get("questionnaire") as? Map<*, *>
                val goal = when (val raw = questionnaire?.get("freq")) {
                    is Number -> raw.toInt()
                    is String -> raw.toIntOrNull() ?: 3
                    else -> 3
                }

                progress.put("goal", goal)
                val out = JSONObject()
                    .put("user", JSONObject()
                        .put("displayName", profileName)
                        .put("firstName", profileName.trim().split(" ").firstOrNull().orEmpty())
                        .put("email", email))
                    .put("progress", progress)

                prefs.edit().putString("dashboard", out.toString()).apply()
                done(true, null, out)
            }
        }.addOnFailureListener { error ->
            if (cached == null) done(false, error.localizedMessage, null)
        }
    }


    fun getWorkoutPlan(done: (Boolean, String?, JSONObject?) -> Unit) {
        val user = auth.currentUser ?: return done(false, "אין משתמש מחובר", null)
        val ref = userDoc() ?: return done(false, "אין משתמש מחובר", null)

        fun fromQuestionnaire(q: Map<*, *>?) {
            runCatching {
                val sessions = runCatching { JSONArray(prefs.getString("recentWorkouts:${user.uid}", "[]")) }.getOrDefault(JSONArray())
                val recent = mutableSetOf<String>()
                val today = dayKey(Date())
                for (session in 0 until sessions.length()) {
                    val entry = sessions.optJSONObject(session) ?: continue
                    if (entry.optString("date") == today) continue
                    val ids = entry.optJSONArray("ids") ?: continue
                    for (index in 0 until ids.length()) ids.optString(index).takeIf(String::isNotBlank)?.let(recent::add)
                }
                WorkoutPlanEngine.generate(q, user.uid, recentExerciseIds = recent)
            }.onSuccess { done(true, null, it) }
             .onFailure { done(false, it.localizedMessage, null) }
        }

        ref.get()
            .addOnSuccessListener { snap ->
                val q = snap.get("questionnaire") as? Map<*, *>
                if (q != null) {
                    val cached = q.entries.mapNotNull { (key, value) ->
                        (key as? String)?.let { it to if (value is Timestamp) value.toDate().toInstant().toString() else value }
                    }.toMap()
                    prefs.edit().putString("questionnaire", JSONObject(cached).toString()).apply()
                    fromQuestionnaire(q)
                } else {
                    val cached = prefs.getString("questionnaire", null)
                    if (cached != null) {
                        val obj = JSONObject(cached)
                        fromQuestionnaire(jsonObjectToMap(obj))
                    } else {
                        fromQuestionnaire(emptyMap<String, Any?>())
                    }
                }
            }
            .addOnFailureListener {
                val cached = prefs.getString("questionnaire", null)
                if (cached != null) {
                    fromQuestionnaire(jsonObjectToMap(JSONObject(cached)))
                } else {
                    fromQuestionnaire(emptyMap<String, Any?>())
                }
            }
    }

    fun getProgress(done: (Boolean, String?, JSONObject?) -> Unit) {
        val cached = prefs.getString("progress", null)
        if (cached != null) runCatching { done(true, null, JSONObject(cached)) }
        buildProgress { ok, error, data ->
            if (ok && data != null) prefs.edit().putString("progress", data.toString()).apply()
            if (!ok && cached != null) return@buildProgress
            done(ok, error, data)
        }
    }

    private fun buildProgress(done: (Boolean, String?, JSONObject?) -> Unit) {
        val collection = workouts() ?: return done(false, "יש להתחבר לחשבון", null)
        collection.orderBy("completedAt", Query.Direction.DESCENDING)
            .limit(250)
            .get()
            .addOnSuccessListener { snapshot ->
                val docs = snapshot.documents
                var total = 0
                var mins = 0
                var week = 0
                val history = JSONArray()
                val doneMap = JSONObject()
                val uniqueDates = linkedSetOf<String>()
                val weeklyCounts = intArrayOf(0, 0, 0, 0)
                val weekStarts = (3 downTo 0).map { weeksAgoStart(it) }

                docs.forEach { d ->
                    val activityType = d.getString("activityType") ?: "workout"
                    if (activityType != "workout") return@forEach

                    total++
                    val duration = (d.getLong("durationMins") ?: 0L).toInt()
                    mins += duration
                    val dateKey = d.getString("dateKey")
                        ?: d.getTimestamp("completedAt")?.toDate()?.let(::dayKey)
                        ?: return@forEach
                    uniqueDates.add(dateKey)
                    d.getString("planKey")?.let { doneMap.put(it, true) }

                    val date = parseDay(dateKey)
                    if (date != null) {
                        if (!date.before(currentWeekStart())) week++
                        val idx = weekStarts.indexOfLast { !date.before(it) }
                        if (idx in 0..3) {
                            val next = if (idx == 3) Date(Long.MAX_VALUE) else weekStarts[idx + 1]
                            if (date.before(next)) weeklyCounts[idx]++
                        }
                    }

                    if (history.length() < 20) {
                        history.put(JSONObject()
                            .put("title", d.getString("title") ?: "אימון")
                            .put("mins", duration)
                            .put("date", dateKey))
                    }
                }

                val out = JSONObject()
                    .put("total", total)
                    .put("streak", calculateCurrentStreak(uniqueDates))
                    .put("maxStreak", calculateMaxStreak(uniqueDates))
                    .put("mins", mins)
                    .put("week", week)
                    .put("weekDone", week)
                    .put("history", history)
                    .put("done", doneMap)
                    .put("weeklyCounts", JSONArray(weeklyCounts.toList()))
                    .put("exercisePRs", JSONObject())

                done(true, null, out)
            }
            .addOnFailureListener { done(false, it.localizedMessage, null) }
    }

    fun getProfile(done: (Boolean, String?, JSONObject?) -> Unit) {
        val user = auth.currentUser ?: return done(false, "אין משתמש מחובר", null)
        val cached = prefs.getString("profile", null)
        if (cached != null) runCatching { done(true, null, JSONObject(cached)) }

        userDoc()?.get()
            ?.addOnSuccessListener { snap ->
                val pref = snap.get("preferences") as? Map<*, *>
                val out = JSONObject()
                    .put("name", snap.getString("displayName") ?: user.displayName ?: user.email?.substringBefore("@") ?: "מתאמן")
                    .put("email", snap.getString("email") ?: user.email.orEmpty())
                    .put("preferences", JSONObject()
                        .put("reminders", pref?.get("reminders") as? Boolean ?: true)
                        .put("haptics", pref?.get("haptics") as? Boolean ?: true)
                        .put("sound", pref?.get("sound") as? Boolean ?: true))
                prefs.edit().putString("profile", out.toString()).apply()
                done(true, null, out)
            }
            ?.addOnFailureListener { if (cached == null) done(false, it.localizedMessage, null) }
    }

    fun savePreferences(json: String, done: (Boolean, String?) -> Unit) {
        val ref = userDoc() ?: return done(false, "יש להתחבר לחשבון")
        val obj = JSONObject(json)
        val map = mapOf(
            "reminders" to obj.optBoolean("reminders", true),
            "haptics" to obj.optBoolean("haptics", true),
            "sound" to obj.optBoolean("sound", true)
        )
        ref.set(mapOf("preferences" to map, "updatedAt" to FieldValue.serverTimestamp()),
            com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                prefs.edit().remove("profile").apply()
                done(true, null)
            }
            .addOnFailureListener { done(false, it.localizedMessage) }
    }

    fun updateProfile(name: String, newEmail: String?, done: (Boolean, String?) -> Unit) {
        val user = auth.currentUser ?: return done(false, "אין משתמש מחובר")
        val safeName = name.trim()
        if (safeName.isBlank()) return done(false, "השם לא יכול להיות ריק")

        val profile = UserProfileChangeRequest.Builder().setDisplayName(safeName).build()
        user.updateProfile(profile).addOnSuccessListener {
            val finishFirestore: () -> Unit = {
                userDoc()?.set(
                    mapOf(
                        "displayName" to safeName,
                        "email" to (newEmail?.takeIf { it.isNotBlank() } ?: user.email),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )?.addOnSuccessListener {
                    prefs.edit().remove("profile").remove("dashboard").apply()
                    done(true, null)
                }?.addOnFailureListener { done(false, it.localizedMessage) }
            }

            if (!newEmail.isNullOrBlank() && !newEmail.equals(user.email, ignoreCase = true)) {
                user.verifyBeforeUpdateEmail(newEmail)
                    .addOnSuccessListener { finishFirestore() }
                    .addOnFailureListener { done(false, it.localizedMessage) }
            } else {
                finishFirestore()
            }
        }.addOnFailureListener { done(false, it.localizedMessage) }
    }

    fun changePassword(currentPassword: String, newPassword: String, done: (Boolean, String?) -> Unit) {
        val user = auth.currentUser ?: return done(false, "אין משתמש מחובר")
        val email = user.email ?: return done(false, "החשבון הזה אינו משתמש בסיסמה")
        if (newPassword.length < 6) return done(false, "הסיסמה החדשה קצרה מדי")

        val credential = EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(newPassword)
                    .addOnSuccessListener { done(true, null) }
                    .addOnFailureListener { done(false, it.localizedMessage) }
            }
            .addOnFailureListener { done(false, it.localizedMessage) }
    }

    fun deleteAccount(done: (Boolean, String?) -> Unit) {
        val user = auth.currentUser ?: return done(false, "אין משתמש מחובר")
        val collection = workouts() ?: return done(false, "אין משתמש מחובר")

        collection.get().addOnSuccessListener { qs ->
            val batch = db.batch()
            qs.documents.forEach { batch.delete(it.reference) }
            userDoc()?.let { batch.delete(it) }
            batch.commit().addOnSuccessListener {
                user.delete()
                    .addOnSuccessListener {
                        prefs.edit().clear().apply()
                        done(true, null)
                    }
                    .addOnFailureListener { done(false, it.localizedMessage) }
            }.addOnFailureListener { done(false, it.localizedMessage) }
        }.addOnFailureListener { done(false, it.localizedMessage) }
    }

    private fun dayKey(date: Date): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }.format(date)

    private fun parseDay(value: String): Date? =
        runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                isLenient = false
                timeZone = TimeZone.getDefault()
            }.parse(value)
        }.getOrNull()

    private fun currentWeekStart(): Date {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val diff = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY + 7) % 7
        cal.add(Calendar.DAY_OF_MONTH, -diff)
        return cal.time
    }

    private fun weeksAgoStart(weeksAgo: Int): Date {
        val cal = Calendar.getInstance()
        cal.time = currentWeekStart()
        cal.add(Calendar.DAY_OF_MONTH, -7 * weeksAgo)
        return cal.time
    }

    private fun calculateCurrentStreak(keys: Set<String>): Int {
        if (keys.isEmpty()) return 0
        val cal = Calendar.getInstance()
        fun key() = dayKey(cal.time)

        if (!keys.contains(key())) {
            cal.add(Calendar.DAY_OF_MONTH, -1)
            if (!keys.contains(key())) return 0
        }

        var streak = 0
        while (keys.contains(key())) {
            streak++
            cal.add(Calendar.DAY_OF_MONTH, -1)
        }
        return streak
    }

    private fun calculateMaxStreak(keys: Set<String>): Int {
        if (keys.isEmpty()) return 0
        val dates = keys.mapNotNull(::parseDay).sorted()
        var max = 1
        var current = 1
        for (i in 1 until dates.size) {
            val diff = ((dates[i].time - dates[i - 1].time) / 86400000L).toInt()
            if (diff == 1) {
                current++
                if (current > max) max = current
            } else if (diff > 1) {
                current = 1
            }
        }
        return max
    }

    private fun jsonObjectToMap(obj: JSONObject): MutableMap<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = jsonValue(obj.opt(key))
        }
        return map
    }

    private fun jsonArrayToList(array: JSONArray): List<Any?> =
        (0 until array.length()).map { jsonValue(array.opt(it)) }

    private fun jsonValue(value: Any?): Any? = when (value) {
        JSONObject.NULL, null -> null
        is JSONObject -> jsonObjectToMap(value)
        is JSONArray -> jsonArrayToList(value)
        else -> value
    }
}
