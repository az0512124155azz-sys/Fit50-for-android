package com.fit50.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private lateinit var bridge: Fit50WebBridge
    private var navigationBarInsetDp: Int = 0

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this).apply {
            setBackgroundColor(android.graphics.Color.rgb(31, 43, 36))
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.setSupportZoom(false)
            settings.builtInZoomControls = false
            settings.displayZoomControls = false

            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    applySystemInsetsToPage()
                }
            }

            bridge = Fit50WebBridge(this@MainActivity, this)
            addJavascriptInterface(
                bridge,
                "Fit50Native"
            )

            loadUrl("file:///android_asset/fit50/splash.html")
        }

        setContentView(webView)

        ViewCompat.setOnApplyWindowInsetsListener(webView) { _, insets ->
            val navigationBars =
                insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            navigationBarInsetDp =
                (navigationBars.bottom / resources.displayMetrics.density)
                    .roundToInt()

            applySystemInsetsToPage()
            insets
        }

        ViewCompat.requestApplyInsets(webView)
    }

    private fun applySystemInsetsToPage() {
        if (!::webView.isInitialized) return

        val bottom = navigationBarInsetDp.coerceAtLeast(0)

        webView.post {
            if (!isFinishing && !isDestroyed) {
                webView.evaluateJavascript(
                    "document.documentElement.style.setProperty('--fit50-system-bottom', '${bottom}px');" +
                        "window.dispatchEvent(new CustomEvent('fit50SystemInsetsChanged'));",
                    null
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (::webView.isInitialized && ::bridge.isInitialized) {
            webView.postDelayed({
                val currentUrl = webView.url.orEmpty()
                if (
                    currentUrl.contains("/fit50/login.html") ||
                    currentUrl.contains("/fit50/splash.html")
                ) {
                    bridge.continueSignedInSession()
                }
            }, 650)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.removeJavascriptInterface("Fit50Native")
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }
}
