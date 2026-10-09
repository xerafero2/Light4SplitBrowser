package com.example.light4split

import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

class MainActivity : AppCompatActivity() {

    private lateinit var webViews: List<WebView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val webViewIds = listOf(R.id.webview1, R.id.webview2, R.id.webview3, R.id.webview4)
        val targetUrl = "https://example.com"
        val profileNames = listOf("session_a", "session_b", "session_c", "session_d")

        webViews = webViewIds.map { findViewById<WebView>(it) }

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
            // Fallback: tanpa isolasi profil (untuk perangkat lawas)
            webViews.forEach { wv ->
                wv.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    setSupportZoom(false)
                    builtInZoomControls = false
                    displayZoomControls = false
                    allowFileAccess = false
                    allowContentAccess = false
                    mediaPlaybackRequiresUserGesture = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                }
                wv.webViewClient = WebViewClient()
                wv.loadUrl(targetUrl)
            }
            return
        }

        val profileStore = ProfileStore.getInstance()

        webViews.forEachIndexed { index, webView ->
            // Buat dan terapkan profil terisolasi
            val profile = profileStore.getOrCreateProfile(profileNames[index])
            WebViewCompat.setProfile(webView, profile.name)

            webView.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
                allowFileAccess = false
                allowContentAccess = false
                mediaPlaybackRequiresUserGesture = true
                cacheMode = WebSettings.LOAD_DEFAULT
            }

            profile.cookieManager.setAcceptCookie(true)
            webView.webViewClient = WebViewClient()
            webView.loadUrl(targetUrl)
        }
    }

    override fun onDestroy() {
        webViews.forEach { it.destroy() }
        super.onDestroy()
    }
}
