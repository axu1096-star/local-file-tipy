package com.example.filebox.domain

import java.io.File
import java.io.InputStream
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Pure-JVM file copy helper. Copies an input stream into the managed root under
 * `<yyyyMM>/<uuid>[.ext]` and returns the resulting file plus bytes written.
 *
 * Injecting the root directory and clock keeps this testable without Android SDK.
 */
class FileCopier(
    private val managedRoot: File,
    private val clock: () -> LocalDate = { LocalDate.now(ZoneId.systemDefault()) },
    private val idGen: () -> String = { UUID.randomUUID().toString() }
) {
    data class Result(val file: File, val bytes: Long, val relativePath: String)

    fun copy(source: InputStream, originalName: String?): Result {
        val ext = originalName?.substringAfterLast('.', "")
            ?.lowercase()
            ?.takeIf { it.isNotEmpty() && it.length <= 10 }
        val subdir = clock().format(MONTH_FMT)
        val dir = File(managedRoot, subdir).apply { mkdirs() }
        val name = buildString {
            append(idGen())
            if (ext != null) {
                append('.')
                append(ext)
            }
        }
        val target = File(dir, name)
        val bytes = source.use { input ->
            target.outputStream().use { out -> input.copyTo(out) }
        }
        val relative = "$subdir/$name"
        return Result(target, bytes, relative)
    }

    companion object {
        private val MONTH_FMT = DateTimeFormatter.ofPattern("yyyyMM")
        const val ROOT_DIR = "managed"
    }
}
