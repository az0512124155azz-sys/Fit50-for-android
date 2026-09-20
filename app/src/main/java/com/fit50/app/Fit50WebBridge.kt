package com.fit50.app

import android.content.Intent
import android.net.Uri
import android.os.Build
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

    private fun authCallback(action: String, ok: Boolean, message: String?) {
        jsCallback("fit50NativeResult", action, ok, message ?: "")
    }

    @JavascriptInterface
    fun login(email: String, password: String) {
        activity.runOnUiThread {
            auth.signIn(email, password) { ok, error ->
                if (ok) data.bootstrapUser { _, _ -> }
                authCallback("login", ok, if (ok) "התחברת בהצלחה" else error ?: "ההתחברות נכשלה")
            }
        }
    }

    @JavascriptInterface
    fun register(email: String, password: String) {
        activity.runOnUiThread {
            auth.register(email, password) { ok, error ->
                if (ok) data.bootstrapUser { _, _ -> }
                authCallback("register", ok, if (ok) "החשבון נוצר בהצלחה" else error ?: "ההרשמה נכשלה")
            }
        }
    }

    @JavascriptInterface
    fun resetPassword(email: String) {
        activity.runOnUiThread {
            auth.resetPassword(email) { ok, error ->
                authCallback("reset", ok, if (ok) "נשלח אליך קישור לאיפוס הסיסמה" else error ?: "שליחת הקישור נכשלה")
            }
        }
    }

    @JavascriptInterface
    fun googleSignIn() {
        activity.runOnUiThread {
            activity.lifecycleScope.launch {
                auth.signInWithGoogle(activity) { ok, error ->
                    if (ok) data.bootstrapUser { _, _ -> }
                    authCallback("google", ok, if (ok) "התחברת עם Google" else error ?: "Google Sign-In נכשל")
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
        authCallback("logout", true, "התנתקת בהצלחה")
    }

    @JavascriptInterface
    fun shareText(text: String) {
        activity.runOnUiThread {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            activity.startActivity(Intent.createChooser(intent, "שיתוף"))
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
