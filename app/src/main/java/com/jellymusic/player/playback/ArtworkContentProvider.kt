package com.jellymusic.player.playback

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.jellymusic.player.JellyMusicApp
import java.io.File
import java.io.FileNotFoundException

class ArtworkContentProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.jellymusic.player.artwork"

        fun getArtworkUri(itemId: String): Uri {
            return Uri.parse("content://$AUTHORITY/$itemId.jpg")
        }
    }

    override fun onCreate(): Boolean = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val itemId = uri.lastPathSegment?.removeSuffix(".jpg") ?: throw FileNotFoundException("Invalid URI")
        val ctx = context ?: throw FileNotFoundException("No context")
        val cacheDir = File(ctx.cacheDir, "artwork_cache")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        val file = File(cacheDir, "$itemId.jpg")
        if (!file.exists() || file.length() == 0L) {
            val success = JellyMusicApp.instance.jellyfinClient.downloadArtworkToFileSync(itemId, file)
            if (!success || !file.exists() || file.length() == 0L) {
                throw FileNotFoundException("Artwork could not be downloaded for $itemId")
            }
        }

        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String = "image/jpeg"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
