package com.fit50.app

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private lateinit var bridge: Fit50WebBridge

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val systemBarColor = Color.rgb(28, 42, 34)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(systemBarColor),
            navigationBarStyle = SystemBarStyle.dark(systemBarColor)
        )

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
            webViewClient = WebViewClient()

            bridge = Fit50WebBridge(this@MainActivity, this)
            addJavascriptInterface(
                bridge,
                "Fit50Native"
            )

            loadUrl("file:///android_asset/fit50/splash.html")
        }

        // Resize the WebView itself, so every fixed element (including iframe
        // content) stays inside the safe area before a page starts rendering.
        val content = FrameLayout(this).apply {
            setBackgroundColor(systemBarColor)
            addView(webView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))
        }
        ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
            val safeArea = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout() or
                    WindowInsetsCompat.Type.ime()
            )
            view.setPadding(safeArea.left, safeArea.top, safeArea.right, safeArea.bottom)
            // The container owns the insets; WebView must not apply them again.
            WindowInsetsCompat.CONSUMED
        }
        setContentView(content)
        ViewCompat.requestApplyInsets(content)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    try {
                        onBackPressedDispatcher.onBackPressed()
                    } finally {
                        isEnabled = true
                    }
                }
            }
        })
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

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.removeJavascriptInterface("Fit50Native")
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }
}
