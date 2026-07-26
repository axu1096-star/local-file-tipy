package com.example.filebox.domain

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.example.filebox.data.db.ManagedFileDao
import com.example.filebox.data.entity.ManagedFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Imports a content URI selected via SAF into the app-private managed directory,
 * then persists a ManagedFile row. Strategy A: copy, do not persist URI permission.
 */
@Singleton
class FileImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: ManagedFileDao
) {

    data class Metadata(val displayName: String, val size: Long, val mimeType: String)

    suspend fun import(uri: Uri): Long = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val meta = queryMetadata(resolver, uri)
        val root = File(context.filesDir, FileCopier.ROOT_DIR).apply { mkdirs() }
        val copier = FileCopier(root)
        val result = resolver.openInputStream(uri)?.use { input ->
            copier.copy(input, meta.displayName)
        } ?: error("Cannot open input stream for $uri")

        val row = ManagedFile(
            displayName = meta.displayName,
            storedPath = result.relativePath,
            mimeType = meta.mimeType,
            sizeBytes = if (meta.size > 0) meta.size else result.bytes,
            category = Category.fromMime(meta.mimeType, meta.displayName),
            sourceUri = uri.toString(),
            note = null,
            addedAt = System.currentTimeMillis()
        )
        dao.insert(row)
    }

    private fun queryMetadata(resolver: ContentResolver, uri: Uri): Metadata {
        var name: String? = null
        var size: Long = -1
        resolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) name = c.getString(nameIdx)
                if (sizeIdx >= 0 && !c.isNull(sizeIdx)) size = c.getLong(sizeIdx)
            }
        }
        val fallbackName = uri.lastPathSegment?.substringAfterLast('/') ?: "file"
        val displayName = name ?: fallbackName
        val mime = resolver.getType(uri)
            ?: displayName.substringAfterLast('.', "")
                .takeIf { it.isNotEmpty() }
                ?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it.lowercase()) }
            ?: "application/octet-stream"
        return Metadata(displayName, size, mime)
    }

    fun managedRoot(): File = File(context.filesDir, FileCopier.ROOT_DIR)

    fun resolveFile(storedPath: String): File = File(managedRoot(), storedPath)
}
