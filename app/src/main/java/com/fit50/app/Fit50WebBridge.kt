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

    private fun callback(action: String, ok: Boolean, message: String?) {
        val js = "window.fit50NativeResult(" +
            JSONObject.quote(action) + "," +
            ok + "," +
            JSONObject.quote(message ?: "") +
            ");"

        activity.runOnUiThread {
            if (!activity.isFinishing && !activity.isDestroyed) {
                webView.evaluateJavascript(js, null)
            }
        }
    }

    @JavascriptInterface
    fun login(email: String, password: String) {
        activity.runOnUiThread {
            auth.signIn(email, password) { ok, error ->
                callback("login", ok, if (ok) "התחברת בהצלחה" else error ?: "ההתחברות נכשלה")
            }
        }
    }

    @JavascriptInterface
    fun register(email: String, password: String) {
        activity.runOnUiThread {
            auth.register(email, password) { ok, error ->
                callback("register", ok, if (ok) "החשבון נוצר בהצלחה" else error ?: "ההרשמה נכשלה")
            }
        }
    }

    @JavascriptInterface
    fun resetPassword(email: String) {
        activity.runOnUiThread {
            auth.resetPassword(email) { ok, error ->
                callback("reset", ok, if (ok) "נשלח אליך קישור לאיפוס הסיסמה" else error ?: "שליחת הקישור נכשלה")
            }
        }
    }

    @JavascriptInterface
    fun googleSignIn() {
        activity.runOnUiThread {
            activity.lifecycleScope.launch {
                auth.signInWithGoogle(activity) { ok, error ->
                    callback("google", ok, if (ok) "התחברת עם Google" else error ?: "Google Sign-In נכשל")
                }
            }
        }
    }

    @JavascriptInterface
    fun logout() {
        auth.signOut()
        callback("logout", true, "התנתקת בהצלחה")
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
            runCatching {
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
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
            vibrator.vibrate(
                VibrationEffect.createOneShot(milliseconds.toLong(), VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(milliseconds.toLong())
        }
    }
}
