package com.example.dovzhenkotv

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


class LoginActivity : AppCompatActivity() {
    private var webView: WebView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_layout)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        webView = findViewById(R.id.weblogin)
        webView?.settings?.domStorageEnabled = true
        webView?.settings?.javaScriptEnabled = true
        val that = this
        webView?.setWebViewClient(object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                // Check if we've reached the desired page
                url?.let {
                    if (it.contains("https://online.dovzhenkocentre.org/my-account")) {
                        // Handle cookies here after the page is fully loaded
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true);

                        // Retrieve all cookies as a single string
                        val cookies = cookieManager.getCookie(it)
                        that.intent.putExtra("cookies", cookies)
                        setResult(Activity.RESULT_OK, intent)
                        finish()
                    }
                }
            }
        })
        webView?.loadUrl("https://online.dovzhenkocentre.org/login-3/")
    }
}