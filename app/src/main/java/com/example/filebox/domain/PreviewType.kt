package com.example.filebox.domain

/** Types the app can preview inline. Others fall back to system ACTION_VIEW. */
enum class PreviewType {
    IMAGE, VIDEO, AUDIO, TEXT, NONE;

    companion object {
        private val TEXT_MIMES = setOf(
            "text/plain", "text/markdown", "text/csv", "text/html", "text/xml",
            "application/json", "application/xml"
        )
        private val TEXT_EXTS = setOf(
            "txt", "md", "log", "json", "xml", "csv", "html", "htm", "yml", "yaml",
            "kt", "java", "gradle", "properties", "sh"
        )

        fun of(mime: String?, fileName: String? = null): PreviewType {
            val m = mime?.lowercase()?.trim().orEmpty()
            when {
                m.startsWith("image/") -> return IMAGE
                m.startsWith("video/") -> return VIDEO
                m.startsWith("audio/") -> return AUDIO
                m in TEXT_MIMES || m.startsWith("text/") -> return TEXT
            }
            val ext = fileName?.substringAfterLast('.', "")?.lowercase()?.takeIf { it.isNotEmpty() }
                ?: return NONE
            return when {
                ext in TEXT_EXTS -> TEXT
                Category.fromExtension(ext) == Category.IMAGE -> IMAGE
                Category.fromExtension(ext) == Category.VIDEO -> VIDEO
                Category.fromExtension(ext) == Category.AUDIO -> AUDIO
                else -> NONE
            }
        }
    }
}
