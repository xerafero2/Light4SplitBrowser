package com.example.light4split

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

class MainActivity : AppCompatActivity() {

    private data class Quadrant(val webView: WebView, val urlBar: EditText)
    private val quadrants = mutableListOf<Quadrant>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        setContentView(root)

        val targetUrl = "https://example.com"
        val profileNames = listOf("session_a", "session_b", "session_c", "session_d")

        // Ambil 4 profil perangkat Android yang berbeda secara acak
        val deviceProfiles = FingerprintSpoofer.pickDistinctProfiles(4)

        val useProfiles = WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)
        val profileStore = if (useProfiles) ProfileStore.getInstance() else null

        for (row in 0 until 2) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
                )
            }
            for (col in 0 until 2) {
                val idx = row * 2 + col
                rowLayout.addView(
                    buildQuadrant(
                        idx,
                        profileNames[idx],
                        targetUrl,
                        profileStore,
                        deviceProfiles[idx]
                    )
                )
            }
            root.addView(rowLayout)
        }

        quadrants.forEach { it.webView.loadUrl(targetUrl) }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun buildQuadrant(
        index: Int,
        profileName: String,
        initialUrl: String,
        profileStore: ProfileStore?,
        deviceProfile: FingerprintSpoofer.Profile
    ): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
        }

        // ---- Toolbar ----
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(34)
            )
            setBackgroundColor(Color.parseColor("#EEEEEE"))
        }

        // ---- WebView ----
        val webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }

        // 1. Isolasi sesi (cookie/cache/DOM) — harus dipanggil sebelum WebView lain
        if (profileStore != null) {
            runCatching {
                val profile = profileStore.getOrCreateProfile(profileName)
                WebViewCompat.setProfile(webView, profile.name)
                profile.cookieManager.setAcceptCookie(true)
            }
        }

        // 2. Fingerprint spoofing (UA + JS injection)
        FingerprintSpoofer.apply(webView, deviceProfile)

        // ---- URL Bar ----
        val urlBar = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            hint = "URL"
            textSize = 9f
            maxLines = 1
            inputType = InputType.TYPE_TEXT_VARIATION_URI
            imeOptions = EditorInfo.IME_ACTION_GO
            setPadding(dp(6), 0, dp(6), 0)
            setText(initialUrl)
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    var url = text.toString().trim()
                    if (url.isNotEmpty() && !url.startsWith("http")) url = "https://$url"
                    webView.loadUrl(url)
                    clearFocus()
                    true
                } else false
            }
        }

        val backBtn = makeNavButton("←") { if (webView.canGoBack()) webView.goBack() }
        val fwdBtn = makeNavButton("→") { if (webView.canGoForward()) webView.goForward() }
        val refreshBtn = makeNavButton("⟳") { webView.reload() }

        toolbar.addView(backBtn)
        toolbar.addView(fwdBtn)
        toolbar.addView(refreshBtn)
        toolbar.addView(urlBar)

        container.addView(toolbar)
        container.addView(webView)

        // ---- WebView settings ringan ----
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
            loadWithOverviewMode = true
            useWideViewPort = true
        }

        // WebViewClient hanya untuk update URL bar
        // (spoofing script sudah di-inject via DOCUMENT_START_SCRIPT)
        if (webView.webViewClient !is WebViewClient) {
            webView.webViewClient = WebViewClient()
        } else {
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (!urlBar.hasFocus() && !url.isNullOrEmpty()) urlBar.setText(url)
                }
            }
        }

        quadrants.add(Quadrant(webView, urlBar))
        return container
    }

    private fun makeNavButton(label: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(30), ViewGroup.LayoutParams.MATCH_PARENT)
            text = label
            textSize = 14f
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            setBackgroundResource(android.R.drawable.list_selector_background)
            setOnClickListener { onClick() }
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        quadrants.forEach { it.webView.destroy() }
        super.onDestroy()
    }
}
