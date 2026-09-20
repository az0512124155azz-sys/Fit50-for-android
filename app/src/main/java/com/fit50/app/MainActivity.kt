package com.fit50.app

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView

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
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    setBackgroundColor(android.graphics.Color.rgb(31, 43, 36))
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)

                    // Android WebView can occasionally miss iframe/srcdoc load events.
                    // Never allow the branded boot screen to remain forever.
                    postDelayed({
                        evaluateJavascript(
                            """
                            (function(){
                              try {
                                var b=document.getElementById('boot');
                                if(b){b.classList.add('hide');}
                                var f=document.getElementById('appFrame');
                                if(f && !f.srcdoc && typeof navigate==='function'){navigate('login',true);}
                              } catch(e) {}
                            })();
                            """.trimIndent(),
                            null
                        )
                        setBackgroundColor(android.graphics.Color.rgb(250, 247, 242))
                    }, 2600)
                }
            }

            addJavascriptInterface(
                Fit50WebBridge(this@MainActivity, this),
                "Fit50Native"
            )

            loadUrl("file:///android_asset/fit50/index.html")
        }

        setContentView(webView)
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
