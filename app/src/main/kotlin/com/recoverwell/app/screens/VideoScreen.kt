package com.recoverwell.app.screens

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import com.recoverwell.app.MainActivity
import com.recoverwell.app.ui.Ui

/**
 * In-app exercise video: a WebView that plays the YouTube search result for the
 * movement inline, so the user never leaves the app. This is the ONLY part of
 * the app that touches the network; everything else stays fully offline. An
 * "Open in YouTube" fallback covers devices where the embed is blocked.
 */
object VideoScreen {

    /** Stops playback/audio the moment the player leaves the screen. */
    private class PlayerView(context: android.content.Context) : WebView(context) {
        override fun onDetachedFromWindow() {
            try {
                stopLoading()
                loadUrl("about:blank")
                onPause()
            } catch (_: Exception) {
            }
            super.onDetachedFromWindow()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun build(a: MainActivity, query: String, title: String, fallbackUrl: String): View {
        val root = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Ui.BG)
        }
        root.addView(Ui.backRow(a, title) { a.popOverlay() })

        val web = PlayerView(a).apply {
            setBackgroundColor(0xFF000000.toInt())
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            webViewClient = WebViewClient()
            webChromeClient = object : WebChromeClient() {
                private var custom: View? = null
                override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                    custom = view
                    (a.window.decorView as ViewGroup).addView(
                        view, ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    )
                }
                override fun onHideCustomView() {
                    custom?.let { (a.window.decorView as ViewGroup).removeView(it) }
                    custom = null
                }
            }
        }
        web.loadDataWithBaseURL("https://www.youtube.com", html(query), "text/html", "utf-8", null)
        root.addView(web, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val footer = Ui.column(a)
        footer.addView(Ui.caption(a, "Playing inside the app from YouTube. The animation on the " +
            "exercise screen works fully offline."))
        footer.addView(Ui.fullWidth(Ui.tonalButton(a, "Open in YouTube app instead") {
            a.openUrl(fallbackUrl)
        }, a, 6))
        root.addView(footer)
        return root
    }

    /** Inline IFrame player cued to the search results for [query]. */
    private fun html(query: String): String {
        val js = jsString(query)
        return """
            <!DOCTYPE html><html><head>
            <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
            <style>html,body{margin:0;padding:0;background:#000;height:100%}
            #player{position:absolute;top:0;left:0;width:100%;height:100%}</style>
            </head><body><div id="player"></div>
            <script src="https://www.youtube.com/iframe_api"></script>
            <script>
            function onYouTubeIframeAPIReady(){
              new YT.Player('player',{width:'100%',height:'100%',
                playerVars:{listType:'search',list:$js,rel:0,modestbranding:1,playsinline:1,fs:1}});
            }
            </script></body></html>
        """.trimIndent()
    }

    private fun jsString(s: String): String {
        val b = StringBuilder("\"")
        for (c in s) when (c) {
            '\\' -> b.append("\\\\")
            '"' -> b.append("\\\"")
            '\n', '\r' -> b.append(' ')
            '<' -> b.append("\\u003c")
            '>' -> b.append("\\u003e")
            else -> b.append(c)
        }
        return b.append("\"").toString()
    }
}
