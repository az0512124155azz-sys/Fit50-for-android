package com.fit50.app

import android.content.Intent
import android.net.Uri
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.json.JSONObject

class Fit50WebBridge(
    private val activity: ComponentActivity,
    private val webView: WebView
) {
    private val auth by lazy { AuthManager(activity.applicationContext) }
    private val data by lazy { Fit50DataManager(activity.applicationContext) }
    private val translations by lazy { TranslationManager(activity.applicationContext) }
    private var pendingDisplayName: String? = null
    private var authNavigationInProgress = false

    private fun jsCallback(function: String, vararg args: Any?) {
        val serialized = args.joinToString(",") { value ->
            when (value) {
                null -> "null"
                is Boolean, is Number -> value.toString()
                is JSONObject -> value.toString()
                else -> JSONObject.quote(value.toString())
            }
        }
        val js = "if(typeof window.$function==='function'){window.$function($serialized);}"
        activity.runOnUiThread {
            if (!activity.isFinishing && !activity.isDestroyed) {
                webView.evaluateJavascript(js, null)
            }
        }
    }

    private fun authCallback(action: String, ok: Boolean, message: String?, destination: String? = null) {
        jsCallback("fit50NativeResult", action, ok, message ?: "", destination)
    }

    private fun navigateTo(destination: String) {
        val page = if (destination == "home") "home" else "questionnaire"
        activity.runOnUiThread {
            if (!activity.isFinishing && !activity.isDestroyed) {
                webView.loadUrl("file:///android_asset/fit50/${page}.html")
            }
        }
    }

    private fun finishAuthenticatedSignIn(action: String, message: String) {
        if (authNavigationInProgress) return
        authNavigationInProgress = true

        val localDestination = data.localStartPage()
        if (localDestination == "home") {
            authCallback(action, true, message, "home")
            navigateTo("home")
            data.bootstrapUser { _, _ -> }
            return
        }

        data.resolveStartPage { destination ->
            activity.runOnUiThread {
                authCallback(action, true, message, destination)
                navigateTo(destination)
                data.bootstrapUser { _, _ -> }
            }
        }
    }

    @JavascriptInterface
    fun continueSignedInSession() {
        if (!auth.isSignedIn()) return
        finishAuthenticatedSignIn("resume", "החשבון כבר מחובר")
    }

    @JavascriptInterface
    fun getDeviceLanguage(): String =
        translations.getDeviceLanguage()

    @JavascriptInterface
    fun getPreferredLanguage(): String =
        translations.getPreferredLanguage()

    @JavascriptInterface
    fun getEffectiveLanguage(): String =
        translations.getEffectiveLanguage()

    @JavascriptInterface
    fun getSupportedLanguages(): String =
        translations.supportedLanguagesJson()

    @JavascriptInterface
    fun setPreferredLanguage(language: String) {
        translations.setPreferredLanguage(language)
    }

    @JavascriptInterface
    fun translateTexts(language: String, textsJson: String) {
        translations.translateTexts(language, textsJson) { ok, target, translationsJson ->
            jsCallback(
                "fit50TranslationResult",
                ok,
                target,
                translationsJson
            )
        }
    }

    @JavascriptInterface
    fun login(email: String, password: String) {
        activity.runOnUiThread {
            auth.signIn(email, password) { ok, error ->
                if (!ok) {
                    authCallback("login", false, error ?: "ההתחברות נכשלה")
                } else {
                    finishAuthenticatedSignIn("login", "התחברת בהצלחה")
                }
            }
        }
    }

    @JavascriptInterface
    fun setPendingDisplayName(name: String) {
        pendingDisplayName = name.trim().takeIf { it.isNotBlank() }
    }

    @JavascriptInterface
    fun register(email: String, password: String) {
        activity.runOnUiThread {
            auth.register(email, password) { ok, error ->
                if (!ok) {
                    authCallback("register", false, error ?: "ההרשמה נכשלה")
                } else {
                    val name = pendingDisplayName
                    pendingDisplayName = null
                    if (!name.isNullOrBlank()) {
                        data.updateProfile(name, null) { _, _ ->
                            data.bootstrapUser { _, _ ->
                                authCallback("register", true, "החשבון נוצר. נשלח גם מייל אימות.", "questionnaire")
                            }
                        }
                    } else {
                        data.bootstrapUser { _, _ ->
                            authCallback("register", true, "החשבון נוצר. נשלח גם מייל אימות.", "questionnaire")
                        }
                    }
                }
            }
        }
    }

    @JavascriptInterface
    fun resetPassword(email: String) {
        activity.runOnUiThread {
            auth.resetPassword(email) { ok, message ->
                authCallback("reset", ok, message ?: if (ok) "בקשת האיפוס התקבלה" else "יצירת בקשת האיפוס נכשלה")
            }
        }
    }

    @JavascriptInterface
    fun googleSignIn() {
        activity.runOnUiThread {
            activity.lifecycleScope.launch {
                auth.signInWithGoogle(activity) { ok, error ->
                    if (!ok) {
                        authCallback("google", false, error ?: "Google Sign-In נכשל")
                    } else {
                        finishAuthenticatedSignIn("google", "התחברת עם Google")
                    }
                }
            }
        }
    }

    @JavascriptInterface
    fun saveQuestionnaire(json: String) {
        data.saveQuestionnaire(json) { ok, error ->
            jsCallback("fit50QuestionnaireSaved", ok, error ?: "")
        }
    }

    @JavascriptInterface
    fun getQuestionnaire() {
        data.getQuestionnaire { ok, error, questionnaire ->
            jsCallback("fit50QuestionnaireLoaded", ok, error ?: "", questionnaire)
        }
    }

    @JavascriptInterface
    fun savePausedWorkout(json: String) {
        data.savePausedWorkout(json)
    }

    @JavascriptInterface
    fun getPausedWorkout(): String =
        data.getPausedWorkout()

    @JavascriptInterface
    fun clearPausedWorkout() {
        data.clearPausedWorkout()
    }

    @JavascriptInterface
    fun completeWorkout(json: String) {
        data.completeWorkout(json) { ok, error, progress ->
            jsCallback("fit50WorkoutSaved", ok, error ?: "", progress)
        }
    }

    @JavascriptInterface
    fun getDashboard() {
        data.getDashboard { ok, error, dashboard ->
            jsCallback("fit50DashboardResult", ok, error ?: "", dashboard)
        }
    }

    @JavascriptInterface
    fun getWorkoutPlan() {
        data.getWorkoutPlan { ok, error, plan ->
            jsCallback("fit50WorkoutPlanResult", ok, error ?: "", plan)
        }
    }

    @JavascriptInterface
    fun playCountdownTone(kind: Int) {
        activity.runOnUiThread {
            runCatching {
                val tone = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                val type = if (kind >= 2) ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD else ToneGenerator.TONE_PROP_BEEP2
                val duration = if (kind >= 2) 350 else 180
                tone.startTone(type, duration)
                webView.postDelayed({ tone.release() }, (duration + 120).toLong())
            }
        }
    }

    @JavascriptInterface
    fun getProgress() {
        data.getProgress { ok, error, progress ->
            jsCallback("fit50ProgressResult", ok, error ?: "", progress)
        }
    }

    @JavascriptInterface
    fun getProfile() {
        data.getProfile { ok, error, profile ->
            jsCallback("fit50ProfileResult", ok, error ?: "", profile)
        }
    }

    @JavascriptInterface
    fun savePreferences(json: String) {
        data.savePreferences(json) { ok, error ->
            jsCallback("fit50PreferencesSaved", ok, error ?: "")
        }
    }

    @JavascriptInterface
    fun updateProfile(name: String, email: String) {
        data.updateProfile(name, email.takeIf { it.isNotBlank() }) { ok, error ->
            jsCallback("fit50ProfileSaved", ok, error ?: "")
        }
    }

    @JavascriptInterface
    fun changePassword(currentPassword: String, newPassword: String) {
        data.changePassword(currentPassword, newPassword) { ok, error ->
            jsCallback("fit50PasswordChanged", ok, error ?: "")
        }
    }

    @JavascriptInterface
    fun deleteAccount() {
        data.deleteAccount { ok, error ->
            jsCallback("fit50AccountDeleted", ok, error ?: "")
        }
    }

    @JavascriptInterface
    fun logout() {
        auth.signOut()
        authNavigationInProgress = false
        authCallback("logout", true, "התנתקת בהצלחה")
    }

    @JavascriptInterface
    fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        activity.runOnUiThread {
            activity.startActivity(Intent.createChooser(intent, "שיתוף"))
        }
    }

    @JavascriptInterface
    fun shareJourney(platform: String, payloadJson: String) {
        activity.runOnUiThread {
            val (ok, message) = JourneyShareManager(activity).share(platform, payloadJson)
            jsCallback("fit50ShareResult", ok, message)
        }
    }

    @JavascriptInterface
    fun openUrl(url: String) {
        activity.runOnUiThread {
            runCatching { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        }
    }

    @JavascriptInterface
    fun sendEmail(address: String, subject: String) {
        activity.runOnUiThread {
            val uri = Uri.parse("mailto:$address")
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra(Intent.EXTRA_SUBJECT, subject)
            }
            runCatching { activity.startActivity(intent) }
        }
    }

    @JavascriptInterface
    fun vibrate(milliseconds: Int) {
        if (milliseconds <= 0) return
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            activity.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            activity.getSystemService(Vibrator::class.java)
        } ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(milliseconds.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(milliseconds.toLong())
        }
    }
}
