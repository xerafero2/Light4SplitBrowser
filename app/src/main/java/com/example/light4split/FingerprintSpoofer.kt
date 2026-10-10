package com.example.light4split

import android.webkit.WebView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import kotlin.random.Random

/**
 * Modul spoofing fingerprint untuk WebView Android.
 * Setiap WebView mendapat profil perangkat acak yang KONSISTEN
 * (UA, GPU, CPU, RAM, dll. saling cocok).
 */
object FingerprintSpoofer {

    data class Profile(
        val model: String,
        val androidVer: String,
        val chromeVer: String,
        val platform: String,
        val hardwareConcurrency: Int,
        val deviceMemory: Int,
        val maxTouchPoints: Int,
        val gpuVendor: String,
        val gpuRenderer: String,
        val language: String,
        val screenWidth: Int,
        val screenHeight: Int,
        val pixelRatio: Float
    ) {
        val userAgent: String
            get() = "Mozilla/5.0 (Linux; Android $androidVer; $model) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/$chromeVer Mobile Safari/537.36"
    }

    // Kumpulan profil perangkat Android nyata (konsisten hardware-nya)
    private val DEVICE_POOL = listOf(
        Profile(
            model = "Pixel 7", androidVer = "13", chromeVer = "118.0.0.0",
            platform = "Linux armv8l", hardwareConcurrency = 8, deviceMemory = 8,
            maxTouchPoints = 5, gpuVendor = "Qualcomm",
            gpuRenderer = "Adreno (TM) 730", language = "id-ID",
            screenWidth = 412, screenHeight = 915, pixelRatio = 2.625f
        ),
        Profile(
            model = "SM-S918B", androidVer = "14", chromeVer = "119.0.0.0",
            platform = "Linux armv8l", hardwareConcurrency = 8, deviceMemory = 8,
            maxTouchPoints = 10, gpuVendor = "Qualcomm",
            gpuRenderer = "Adreno (TM) 740", language = "en-US",
            screenWidth = 384, screenHeight = 832, pixelRatio = 2.8125f
        ),
        Profile(
            model = "Redmi Note 12", androidVer = "13", chromeVer = "117.0.0.0",
            platform = "Linux armv8l", hardwareConcurrency = 8, deviceMemory = 6,
            maxTouchPoints = 10, gpuVendor = "Qualcomm",
            gpuRenderer = "Adreno (TM) 610", language = "id-ID",
            screenWidth = 393, screenHeight = 873, pixelRatio = 2.75f
        ),
        Profile(
            model = "CPH2451", androidVer = "13", chromeVer = "118.0.0.0",
            platform = "Linux armv8l", hardwareConcurrency = 8, deviceMemory = 8,
            maxTouchPoints = 10, gpuVendor = "ARM",
            gpuRenderer = "Mali-G68 MC4", language = "id-ID",
            screenWidth = 412, screenHeight = 915, pixelRatio = 2.625f
        ),
        Profile(
            model = "V2156", androidVer = "12", chromeVer = "116.0.0.0",
            platform = "Linux armv8l", hardwareConcurrency = 8, deviceMemory = 4,
            maxTouchPoints = 10, gpuVendor = "ARM",
            gpuRenderer = "Mali-G76 MC4", language = "en-US",
            screenWidth = 360, screenHeight = 800, pixelRatio = 3.0f
        ),
        Profile(
            model = "moto g84 5G", androidVer = "13", chromeVer = "117.0.0.0",
            platform = "Linux armv8l", hardwareConcurrency = 8, deviceMemory = 8,
            maxTouchPoints = 10, gpuVendor = "Qualcomm",
            gpuRenderer = "Adreno (TM) 695", language = "en-US",
            screenWidth = 393, screenHeight = 873, pixelRatio = 2.75f
        ),
        Profile(
            model = "Pixel 6a", androidVer = "14", chromeVer = "119.0.0.0",
            platform = "Linux armv8l", hardwareConcurrency = 6, deviceMemory = 6,
            maxTouchPoints = 5, gpuVendor = "ARM",
            gpuRenderer = "Mali-G78 MP20", language = "en-US",
            screenWidth = 412, screenHeight = 892, pixelRatio = 2.625f
        ),
        Profile(
            model = "22101316G", androidVer = "12", chromeVer = "116.0.0.0",
            platform = "Linux armv8l", hardwareConcurrency = 8, deviceMemory = 6,
            maxTouchPoints = 10, gpuVendor = "ARM",
            gpuRenderer = "Mali-G57 MC2", language = "id-ID",
            screenWidth = 360, screenHeight = 800, pixelRatio = 3.0f
        )
    )

    /** Ambil [count] profil berbeda secara acak (tanpa duplikat). */
    fun pickDistinctProfiles(count: Int): List<Profile> {
        val shuffled = DEVICE_POOL.shuffled(Random(System.nanoTime()))
        return List(count) { shuffled[it % shuffled.size] }
    }

    /** Terapkan spoofing ke WebView. */
    fun apply(webView: WebView, profile: Profile) {
        // 1. Set User-Agent native (paling andal)
        webView.settings.userAgentString = profile.userAgent

        // 2. Inject JavaScript sebelum dokumen dimuat (jika didukung)
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            runCatching {
                WebViewCompat.addDocumentStartJavaScript(
                    webView,
                    buildScript(profile),
                    setOf("*")
                )
            }
        } else {
            // Fallback: evaluate saat onPageStarted
            webView.webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    view?.evaluateJavascript(buildScript(profile), null)
                }
            }
        }
    }

    private fun buildScript(p: Profile): String = """
        (function() {
            'use strict';
            const def = (obj, prop, val) => {
                try {
                    Object.defineProperty(obj, prop, {
                        get: () => val, configurable: true
                    });
                } catch(e) {}
            };

            // --- Navigator ---
            def(navigator, 'platform', '${p.platform}');
            def(navigator, 'hardwareConcurrency', ${p.hardwareConcurrency});
            def(navigator, 'deviceMemory', ${p.deviceMemory});
            def(navigator, 'maxTouchPoints', ${p.maxTouchPoints});
            def(navigator, 'language', '${p.language}');
            def(navigator, 'languages', ['${p.language}', 'en']);

            // --- Screen (viewport tetap asli, hanya metadata yang diubah) ---
            def(screen, 'width', ${p.screenWidth});
            def(screen, 'height', ${p.screenHeight});
            def(screen, 'availWidth', ${p.screenWidth});
            def(screen, 'availHeight', ${p.screenHeight - 48});
            def(window, 'devicePixelRatio', ${p.pixelRatio});

            // --- WebGL vendor/renderer ---
            const patchGL = (proto) => {
                if (!proto || !proto.getParameter) return;
                const orig = proto.getParameter;
                proto.getParameter = function(param) {
                    if (param === 37445) return '${p.gpuVendor}';   // UNMASKED_VENDOR_WEBGL
                    if (param === 37446) return '${p.gpuRenderer}'; // UNMASKED_RENDERER_WEBGL
                    return orig.call(this, param);
                };
            };
            patchGL(window.WebGLRenderingContext && WebGLRenderingContext.prototype);
            patchGL(window.WebGL2RenderingContext && WebGL2RenderingContext.prototype);

            // --- Nonaktifkan WebRTC (cegah kebocoran IP lokal) ---
            def(window, 'RTCPeerConnection', undefined);
            def(window, 'webkitRTCPeerConnection', undefined);
            def(window, 'mozRTCPeerConnection', undefined);
        })();
    """.trimIndent()
}
