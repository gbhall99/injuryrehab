package com.recoverwell.app.ui

import android.net.Uri
import android.provider.DocumentsContract
import android.view.View
import android.widget.FrameLayout
import android.widget.VideoView
import com.recoverwell.app.MainActivity
import com.recoverwell.core.model.ExerciseSpec

/**
 * The user's own demonstration clips: a folder they pick once (Drive, Files,
 * SD card - anywhere the system picker reaches) holding short videos named by
 * exercise id or demo id, e.g. `p3_seated_raise.mp4` or `seated_heel_raise.mp4`
 * - exactly the files docs/exercise-demo-video-prompts.md asks a video
 * producer to deliver. A matching clip replaces the stick-figure animation,
 * plays offline, looped and muted. Uses a persisted SAF tree grant: no storage
 * permission, and nothing leaves the phone.
 */
object OwnClips {

    const val KEY_FOLDER = "own_clips_uri"
    const val KEY_FOLDER_NAME = "own_clips_name"

    private val VIDEO_EXT = listOf(".mp4", ".webm", ".m4v", ".mov", ".3gp")

    private var indexedFor: String? = null
    private var index: Map<String, Uri> = emptyMap()

    fun enabled(a: MainActivity): Boolean = a.store.setting(KEY_FOLDER, "").isNotBlank()

    /** The user's clip for [spec]: an exercise-specific file wins over a shared demo one. */
    fun clipFor(a: MainActivity, spec: ExerciseSpec): Uri? {
        if (!enabled(a)) return null
        ensureIndex(a)
        return index[spec.id.lowercase()] ?: index[spec.demoId.lowercase()]
    }

    /** How many clips the folder holds (after a scan). */
    fun count(a: MainActivity): Int {
        if (!enabled(a)) return 0
        ensureIndex(a)
        return index.size
    }

    /** Forget the cached listing so the next lookup re-reads the folder. */
    fun rescan() {
        indexedFor = null
        index = emptyMap()
    }

    private fun ensureIndex(a: MainActivity) {
        val folder = a.store.setting(KEY_FOLDER, "")
        if (folder == indexedFor) return
        indexedFor = folder
        index = try { scan(a, Uri.parse(folder)) } catch (e: Exception) { emptyMap() }
    }

    private fun scan(a: MainActivity, tree: Uri): Map<String, Uri> {
        val out = HashMap<String, Uri>()
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        a.contentResolver.query(children, arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        ), null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0) ?: continue
                val name = (c.getString(1) ?: continue).trim()
                val mime = c.getString(2) ?: ""
                val lower = name.lowercase()
                if (!mime.startsWith("video/") && VIDEO_EXT.none { lower.endsWith(it) }) continue
                out[lower.substringBeforeLast('.')] = DocumentsContract.buildDocumentUriUsingTree(tree, id)
            }
        }
        return out
    }

    /** The demo for [spec]: the user's own clip when there is one, else the bundled animation. */
    fun demoView(a: MainActivity, spec: ExerciseSpec): View {
        val animation = { ExerciseDemoView(a).apply { demoId = spec.demoId } }
        val clip = clipFor(a, spec) ?: return animation()
        return view(a, clip, animation)
    }

    /** A looping, muted, offline player for [uri]; swaps to [fallback] if the clip can't play
     *  (moved, deleted, or a format the phone can't decode). */
    fun view(a: MainActivity, uri: Uri, fallback: () -> View): View {
        val frame = FrameLayout(a)
        val vv = VideoView(a)
        vv.setVideoURI(uri)
        vv.setOnPreparedListener { mp ->
            mp.isLooping = true
            mp.setVolume(0f, 0f)
            vv.start()
        }
        vv.setOnErrorListener { _, _, _ ->
            frame.removeAllViews()
            frame.addView(fallback(), FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            true
        }
        vv.contentDescription = "Your demonstration clip, playing on a loop"
        frame.addView(vv, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT,
            android.view.Gravity.CENTER))
        return frame
    }
}
