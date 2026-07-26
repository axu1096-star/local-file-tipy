package com.example.filebox.domain

enum class Category {
    IMAGE, VIDEO, AUDIO, DOCUMENT, ARCHIVE, OTHER;

    companion object {
        fun fromMime(mime: String?, fileName: String? = null): Category {
            val m = mime?.lowercase()?.trim().orEmpty()
            when {
                m.startsWith("image/") -> return IMAGE
                m.startsWith("video/") -> return VIDEO
                m.startsWith("audio/") -> return AUDIO
                m.startsWith("text/") -> return DOCUMENT
                m == "application/pdf" -> return DOCUMENT
                m == "application/rtf" -> return DOCUMENT
                m == "application/msword" ||
                        m == "application/vnd.ms-excel" ||
                        m == "application/vnd.ms-powerpoint" -> return DOCUMENT
                m.startsWith("application/vnd.openxmlformats-officedocument") -> return DOCUMENT
                m.startsWith("application/vnd.oasis.opendocument") -> return DOCUMENT
                m == "application/zip" ||
                        m == "application/x-tar" ||
                        m == "application/x-7z-compressed" ||
                        m == "application/x-rar-compressed" ||
                        m == "application/vnd.rar" ||
                        m == "application/gzip" ||
                        m == "application/x-bzip2" -> return ARCHIVE
            }
            val ext = fileName?.substringAfterLast('.', missingDelimiterValue = "")
                ?.lowercase()
                ?.takeIf { it.isNotEmpty() }
                ?: return OTHER
            return fromExtension(ext)
        }

        fun fromExtension(ext: String): Category = when (ext.lowercase()) {
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif", "svg" -> IMAGE
            "mp4", "mkv", "mov", "avi", "webm", "3gp", "flv", "wmv" -> VIDEO
            "mp3", "wav", "flac", "aac", "ogg", "m4a", "opus", "amr" -> AUDIO
            "pdf", "txt", "md", "rtf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "odt", "ods", "odp", "csv", "log", "json", "xml", "html", "htm" -> DOCUMENT
            "zip", "tar", "gz", "tgz", "bz2", "7z", "rar", "xz" -> ARCHIVE
            else -> OTHER
        }
    }
}
