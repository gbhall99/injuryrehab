package com.recoverwell.app.screens

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import com.recoverwell.app.MainActivity
import com.recoverwell.app.ui.Ui
import com.recoverwell.core.model.ExerciseOverride
import com.recoverwell.core.model.ExerciseSpec
import com.recoverwell.core.protocol.ExerciseVideo
import com.recoverwell.core.protocol.ProtocolRegistry
import com.recoverwell.core.protocol.VideoResolution
import com.recoverwell.core.protocol.VideoTier

/**
 * In-app exercise video: plays the best available demonstration (the user's
 * pinned video, a physio-checked one, a pre-screened suggestion, or a tuned
 * YouTube search) inline, and says honestly which it is. A safety strip keeps
 * "your plan comes first" in view, because a video made for someone else can
 * show a stretch or a load this user isn't cleared for. This is the ONLY part
 * of the app that touches the network.
 */
object VideoScreen {

    /** Setting key: the user has seen the one-time "videos use data" notice. */
    private const val NOTICE_ACK = "video_notice_ack"

    /** Which pre-screened suggestion is showing, per exercise ("Next" cycles it). */
    private val suggestionIndex = HashMap<String, Int>()

    /**
     * Opens the in-app player, but the first time only, shows a one-time notice
     * that video demonstrations stream from YouTube and use data (the rest of the
     * app is offline). Keeps the offline expectation honest before any network use.
     */
    fun open(a: MainActivity, spec: ExerciseSpec) {
        val go = { a.pushOverlay(spec.name) { build(a, spec) } }
        if (a.store.setting(NOTICE_ACK, "") == "1") {
            go()
            return
        }
        AlertDialog.Builder(a)
            .setTitle("Videos play from YouTube")
            .setMessage("Exercise demonstrations stream from YouTube and use your internet or mobile data. " +
                "Everything else in the app works fully offline.")
            .setPositiveButton("Continue") { _, _ ->
                a.store.saveSetting(NOTICE_ACK, "1")
                go()
            }
            .setNegativeButton("Not now", null)
            .show()
    }

    /** The link handed to the YouTube app in "Open YouTube" mode. */
    fun externalUrl(a: MainActivity, spec: ExerciseSpec): String {
        val r = resolution(a, spec)
        val pick = r.pick ?: return searchUrl(a, spec)
        return "https://www.youtube.com/watch?v=${pick.videoId}" +
            if (pick.startSeconds > 0) "&t=${pick.startSeconds}s" else ""
    }

    fun resolution(a: MainActivity, spec: ExerciseSpec): VideoResolution =
        ExerciseVideo.resolve(spec.id, a.store.exerciseOverrides()[spec.id]?.videoId, suggestionIndex[spec.id] ?: 0)

    private fun searchUrl(a: MainActivity, spec: ExerciseSpec): String =
        ExerciseVideo.youtubeSearchUrl(spec, ProtocolRegistry.forProfile(a.store.profile()).videoContext)

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
    fun build(a: MainActivity, spec: ExerciseSpec): View {
        val r = resolution(a, spec)
        val search = searchUrl(a, spec)
        val root = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Ui.BG)
        }
        val head = Ui.column(a).apply { setPadding(paddingLeft, paddingTop, paddingRight, 0) }
        head.addView(Ui.backRow(a, spec.name) { a.popOverlay() })
        head.addView(sourceBanner(a, spec, r))
        root.addView(head)

        val web = PlayerView(a).apply {
            setBackgroundColor(0xFF000000.toInt())
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            // allow a chosen video to start playing on its own
            settings.mediaPlaybackRequiresUserGesture = false
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
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
        // no connection: say so plainly instead of a browser error page
        val offline = Ui.column(a).apply {
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(Ui.BG)
            visibility = View.GONE
            addView(Ui.text(a, "You're offline", 17f, Ui.TEXT, bold = true).apply {
                gravity = android.view.Gravity.CENTER })
            addView(Ui.text(a, "Videos need a connection. The animation on the exercise works without one, " +
                "and the steps and cues are all there.", 14f, Ui.TEXT_DIM).apply {
                gravity = android.view.Gravity.CENTER })
        }
        web.webViewClient = object : WebViewClient() {
            override fun onReceivedError(view: WebView, request: android.webkit.WebResourceRequest,
                                         error: android.webkit.WebResourceError) {
                if (request.isForMainFrame) offline.visibility = View.VISIBLE
            }
        }
        val stage = android.widget.FrameLayout(a)
        stage.addView(web, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        stage.addView(offline, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        val pick = r.pick
        if (!isOnline(a)) {
            offline.visibility = View.VISIBLE
        } else if (pick != null) {
            // embed the exact video; if it can't play/embed, self-heal to the search
            web.loadDataWithBaseURL("https://www.youtube.com",
                html(pick.videoId, pick.startSeconds, search), "text/html", "utf-8", null)
        } else {
            // no specific video: browse the tuned search results inside the app
            web.loadUrl(search)
        }
        root.addView(stage, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val footer = Ui.column(a)
        // a video made for someone else can show a stretch or load this user isn't cleared for
        footer.addView(Ui.pillBadge(a, "Your plan comes first: boot on where it says, and no calf stretching or " +
            "pulling the foot up past neutral unless your physio said so", Ui.WARN, Ui.WARN_BG).apply {
            maxLines = 3
        })
        val row = Ui.row(a)
        row.addView(Ui.weight(Ui.tonalButton(a, "Open in YouTube") { a.openUrl(externalUrl(a, spec)) }, 1f))
        val browse = Ui.tonalButton(a, "Search results") {
            if (isOnline(a)) {
                offline.visibility = View.GONE
                web.loadUrl(search)
            } else offline.visibility = View.VISIBLE
        }
        val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        lp.setMargins(Ui.dp(a, 8), Ui.dp(a, 10), 0, 0)
        browse.layoutParams = lp
        row.addView(browse)
        footer.addView(row)
        root.addView(footer)
        return root
    }

    /** Says plainly where this video came from and what the user can do about it. */
    private fun sourceBanner(a: MainActivity, spec: ExerciseSpec, r: VideoResolution): View {
        val card = Ui.card(a, if (r.tier == VideoTier.SUGGESTED) Ui.INFO_BG else Ui.CARD)
        when (r.tier) {
            VideoTier.PINNED -> pinnedBanner(a, card)
            VideoTier.CURATED -> {
                card.addView(Ui.text(a, "Physio-checked demonstration", 14.5f, Ui.TEXT, bold = true))
                r.pick?.let { card.addView(Ui.caption(a, "${it.title} · ${it.source}")) }
            }
            VideoTier.SUGGESTED -> {
                val pick = r.pick!!
                val total = ExerciseVideo.suggested[spec.id].orEmpty().size
                card.addView(Ui.text(a, "Suggested: ${pick.title}", 14.5f, Ui.ON_INFO_BG, bold = true))
                card.addView(Ui.text(a, "${pick.source} · picked by its title and source, not yet checked by a " +
                    "physio. Does it match the exercise and your plan?", 13f, Ui.ON_INFO_BG))
                val use = Ui.tonalButton(a, "Use this video") {
                    val base = a.store.exerciseOverrides()[spec.id]
                        ?: ExerciseOverride(spec.id, null, null, null, null, true)
                    a.store.saveExerciseOverride(base.copy(videoId = pick.videoId))
                    // update the banner in place: rebuilding would restart the video
                    card.removeAllViews()
                    card.background = Ui.rounded(Ui.CARD)
                    pinnedBanner(a, card)
                }
                val next = Ui.textButton(a, if (total > 1) "Next suggestion" else "Search instead") {
                    if (total > 1) {
                        suggestionIndex[spec.id] = (suggestionIndex[spec.id] ?: 0) + 1
                        a.refresh()
                    } else a.openUrl(searchUrl(a, spec))
                }
                card.addView(Ui.buttonPair(a, use, next, marginTopDp = 6))
            }
            VideoTier.SEARCH -> {
                card.addView(Ui.text(a, "YouTube results - tap one to play", 14.5f, Ui.TEXT, bold = true))
                card.addView(Ui.caption(a, "Searching “${ExerciseVideo.query(spec,
                    ProtocolRegistry.forProfile(a.store.profile()).videoContext)}”. Found a good one? " +
                    "Pin it under Adjust dose or video."))
            }
        }
        return card
    }

    private fun pinnedBanner(a: MainActivity, card: LinearLayout) {
        card.addView(Ui.text(a, "Your chosen video", 14.5f, Ui.TEXT, bold = true))
        card.addView(Ui.caption(a, "It plays every time for this exercise. Change it under Adjust dose or video."))
    }

    /** False only when the phone clearly has no network; anything uncertain lets the page try. */
    private fun isOnline(a: MainActivity): Boolean = try {
        val cm = a.getSystemService(android.net.ConnectivityManager::class.java)
        val net = cm?.activeNetwork
        when {
            cm == null -> true
            net == null -> false
            else -> cm.getNetworkCapabilities(net)
                ?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: true
        }
    } catch (e: Exception) {
        true
    }

    /** Inline IFrame player for [videoId]; on any error it navigates to [searchUrl]. */
    private fun html(videoId: String, startSeconds: Int, searchUrl: String): String {
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
                playerVars:{playsinline:1,rel:0,modestbranding:1,autoplay:1,fs:1,
                  cc_load_policy:1,cc_lang_pref:'en',start:${startSeconds.coerceAtLeast(0)}},
                events:{onError:function(e){ window.location.href=$search; }}});
            }
            // if the API itself fails to load, fall back to the search too
            setTimeout(function(){ if(!window.YT||!window.YT.Player){ window.location.href=$search; } }, 12000);
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
