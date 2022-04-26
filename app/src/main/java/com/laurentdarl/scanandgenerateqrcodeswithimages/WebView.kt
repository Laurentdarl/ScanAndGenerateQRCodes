package com.laurentdarl.scanandgenerateqrcodeswithimages

import android.os.Build
import android.os.Bundle
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.laurentdarl.scanandgenerateqrcodeswithimages.databinding.ActivityWebViewBinding

class WebView : AppCompatActivity() {

    private lateinit var binding: ActivityWebViewBinding

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val intent = intent!!
        val url = intent.extras!!.getString("Message")!!

        binding.webView.settings.apply {
            // this will enable the javascript settings
            javaScriptEnabled = true
            // if you want to enable zoom feature
            setSupportZoom(true)
            // Warns you from visiting malicious sites
            safeBrowsingEnabled = true
        }
        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                Toast.makeText(
                    applicationContext,
                    "No internet connection. Check your internet connection!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.webView.loadUrl(url)

    }

    // if you press Back button this code will work
    override fun onBackPressed() {
        // if the webView can go back, it will go back
        if (binding.webView.canGoBack())
            binding.webView.goBack()
        else
            super.onBackPressed()
    }
}