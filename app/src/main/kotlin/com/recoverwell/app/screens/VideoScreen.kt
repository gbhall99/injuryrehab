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

    /**
     * @param videoId  a resolved YouTube id to embed inline (pinned/curated), or
     *                 null to browse the search results in-app.
     * @param searchUrl the always-valid YouTube search the player falls back to.
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun build(a: MainActivity, title: String, videoId: String?, searchUrl: String): View {
        val root = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Ui.BG)
        }
        root.addView(Ui.backRow(a, title) { a.popOverlay() })

        val web = PlayerView(a).apply {
            setBackgroundColor(0xFF000000.toInt())
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            // allow a pinned video to start playing on its own
            settings.mediaPlaybackRequiresUserGesture = false
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
        if (videoId != null) {
            // embed the exact video; if it can't play/embed, self-heal to the search
            web.loadDataWithBaseURL("https://www.youtube.com", html(videoId, searchUrl), "text/html", "utf-8", null)
        } else {
            // no specific video: browse the live search results inside the app
            web.loadUrl(searchUrl)
        }
        root.addView(web, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val footer = Ui.column(a)
        footer.addView(Ui.caption(a, if (videoId != null)
            "Playing inside the app. If it won't load, use the buttons below."
            else "Live YouTube results inside the app - tap one to play. Pin a favourite on the exercise screen."))
        val row = Ui.row(a)
        row.addView(Ui.weight(Ui.tonalButton(a, "Open in YouTube") { a.openUrl(
            if (videoId != null) "https://www.youtube.com/watch?v=$videoId" else searchUrl) }, 1f))
        val browse = Ui.tonalButton(a, "Browse results") { web.loadUrl(searchUrl) }
        val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        lp.setMargins(Ui.dp(a, 8), Ui.dp(a, 10), 0, 0)
        browse.layoutParams = lp
        row.addView(browse)
        footer.addView(row)
        root.addView(footer)
        return root
    }

    /** Inline IFrame player for [videoId]; on any error it navigates to [searchUrl]. */
    private fun html(videoId: String, searchUrl: String): String {
        val vid = jsString(videoId)
        val search = jsString(searchUrl)
        return """
            <!DOCTYPE html><html><head>
            <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
            <style>html,body{margin:0;padding:0;background:#000;height:100%}
            #player{position:absolute;top:0;left:0;width:100%;height:100%}</style>
            </head><body><div id="player"></div>
            <script src="https://www.youtube.com/iframe_api"></script>
            <script>
            function onYouTubeIframeAPIReady(){
              new YT.Player('player',{width:'100%',height:'100%',videoId:$vid,
                playerVars:{playsinline:1,rel:0,modestbranding:1,autoplay:1,fs:1},
                events:{onError:function(e){ window.location.href=$search; }}});
            }
            // if the API itself fails to load, fall back to the search too
            setTimeout(function(){ if(!window.YT||!window.YT.Player){ window.location.href=$search; } }, 6000);
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
