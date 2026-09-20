package com.fit50.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private lateinit var bridge: Fit50WebBridge

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this).apply {
            setBackgroundColor(android.graphics.Color.rgb(31, 43, 36))
            overScrollMode = View.OVER_SCROLL_NEVER

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
            webViewClient = WebViewClient()

            bridge = Fit50WebBridge(this@MainActivity, this)
            addJavascriptInterface(
                bridge,
                "Fit50Native"
            )

            loadUrl("file:///android_asset/fit50/splash.html")
        }

        setContentView(webView)
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
