package com.example.filebox.domain

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.filebox.data.entity.ManagedFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Exports managed files back out to a user-chosen SAF tree (folder). This is the
 * only path that writes outside filesDir; it is always user-initiated via
 * OpenDocumentTree and adds no permissions / no network. See
 * aiTask/rules/privacy-invariants.md.
 */
@Singleton
class FileExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val importer: FileImporter
) {
    data class Progress(val done: Int, val total: Int, val currentName: String)
    data class Result(val success: Int, val failed: Int)

    suspend fun exportTo(
        files: List<ManagedFile>,
        treeUri: Uri,
        onProgress: (Progress) -> Unit = {}
    ): Result = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val dir = DocumentFile.fromTreeUri(context, treeUri)
            ?: return@withContext Result(0, files.size)

        val usedNames = HashSet<String>()
        var success = 0
        var failed = 0

        files.forEachIndexed { index, file ->
            onProgress(Progress(index, files.size, file.displayName))
            val ok = runCatching {
                val source = importer.resolveFile(file.storedPath)
                if (!source.exists()) return@runCatching false
                val targetName = uniqueName(file.displayName, usedNames)
                val mime = file.mimeType.ifBlank { "application/octet-stream" }
                val doc = dir.createFile(mime, targetName)
                    ?: return@runCatching false
                resolver.openOutputStream(doc.uri)?.use { out ->
                    source.inputStream().use { input -> input.copyTo(out) }
                } ?: return@runCatching false
                true
            }.getOrDefault(false)
            if (ok) success++ else failed++
        }
        onProgress(Progress(files.size, files.size, ""))
        Result(success, failed)
    }

    companion object {
        /**
         * Returns a name not present in [used] (case-insensitive), appending
         * " (n)" before the extension on collision, then records it. Pure JVM.
         */
        fun uniqueName(displayName: String, used: MutableSet<String>): String {
            val name = displayName.ifBlank { "file" }
            fun taken(candidate: String) = used.any { it.equals(candidate, ignoreCase = true) }
            if (!taken(name)) {
                used.add(name)
                return name
            }
            val dot = name.lastIndexOf('.')
            val base = if (dot > 0) name.substring(0, dot) else name
            val ext = if (dot > 0) name.substring(dot) else ""
            var n = 1
            while (true) {
                val candidate = "$base ($n)$ext"
                if (!taken(candidate)) {
                    used.add(candidate)
                    return candidate
                }
                n++
            }
        }
    }
}
