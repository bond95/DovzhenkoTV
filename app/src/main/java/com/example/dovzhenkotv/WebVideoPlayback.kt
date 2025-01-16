package com.example.dovzhenkotv

import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class WebVideoPlayback : AppCompatActivity() {
    private var webView: WebView? = null
    private var mCookies: Map<String, String>? = null
    private var initLoad = false
    private var videoType: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_web_video_playback)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.webvideo)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val (_, title, description, _, _, videoUrl, infoUrl) =
            intent?.getSerializableExtra(DetailsActivity.MOVIE) as Movie
        mCookies = intent.getSerializableExtra(DetailsActivity.COOKIE) as Map<String, String>
        videoType = intent.getStringExtra(DetailsActivity.VIDEO_TYPE).toString()
        Log.d("Video", videoType)
        webView = findViewById(R.id.webvideoview)
//        intent.putExtra("input", "something")
        webView?.settings?.domStorageEnabled = true
        webView?.settings?.javaScriptEnabled = true
        val choosenButton = if (videoType == "trailer") "buttons.length-1" else "0"
//        Log.d("Video", choosenButton)
//        Log.d("Video", "buttons[" + choosenButton + "].click();")
        mCookies!!.map { it.key + "=" + it.value }.forEach { item ->
            CookieManager.getInstance().setCookie("https://online.dovzhenkocentre.org/", item)
        }
        webView?.setWebViewClient(object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                // Check if we've reached the desired page
                url?.let {
                    Log.d("url", it)
//                    if (!initLoad) {
                        val jsFunction = "javascript:(function() { " +
//                                "location.replace('"+ infoUrl + teaserUrl +"');" +
                                "var buttons = jQuery(\".elementor-widget-button[data-widget_type=\\\"button.default\\\"] > div > div > .elementor-button.elementor-button-link.elementor-size-sm\");" +
                                "buttons[" + choosenButton + "].click();" +
                                "setTimeout(() => {document.querySelectorAll('[id^=\"elementor-popup-modal-\"]')[0].querySelector(\".dialog-message.dialog-lightbox-message\").style='width: 100vw;';" +
                                "document.querySelectorAll('[id^=\"elementor-popup-modal-\"]')[0].querySelector(\".dialog-message.dialog-lightbox-message > div > div\").style = 'width: 100vw;';" +
                                "document.querySelectorAll('[id^=\"elementor-popup-modal-\"]')[0].querySelector(\".dialog-message.dialog-lightbox-message > div > div .e-con-inner\").style = 'margin: 0; width: 100vw; max-width: 100vw;';" +
                                "document.querySelectorAll('[id^=\"elementor-popup-modal-\"]')[0].querySelector(\"iframe\").style = 'height: 100vh; width: 100vw;';" +
                                "document.querySelector('[id^=\"elementor-popup-modal-\"] .elementor-shape-square').remove();" +
                                "document.querySelector('[id^=\"elementor-popup-modal-\"] iframe').click();" +
                                "document.querySelector('[id^=\"elementor-popup-modal-\"] iframe').contentWindow.focus();}, 500);" +
                                "})()"
//                        view?.evaluateJavascript(
//                            "javascript:(function() { " +
//                                    "document.getElementById('btn-to-be-clicked').click();" +
//                                    "})()")
                        initLoad = true
                        webView?.loadUrl(jsFunction)
//                    }
                }
            }
        })
//        val that = this
        webView?.loadUrl(infoUrl.toString())
    }
}