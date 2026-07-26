package com.example.filebox

import com.example.filebox.domain.Category
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CategoryTest {

    @Test fun `image mime maps to IMAGE`() {
        assertThat(Category.fromMime("image/jpeg")).isEqualTo(Category.IMAGE)
        assertThat(Category.fromMime("image/png")).isEqualTo(Category.IMAGE)
        assertThat(Category.fromMime("IMAGE/HEIC")).isEqualTo(Category.IMAGE)
    }

    @Test fun `video and audio mimes map correctly`() {
        assertThat(Category.fromMime("video/mp4")).isEqualTo(Category.VIDEO)
        assertThat(Category.fromMime("audio/mpeg")).isEqualTo(Category.AUDIO)
    }

    @Test fun `document mimes cover pdf office and text`() {
        assertThat(Category.fromMime("application/pdf")).isEqualTo(Category.DOCUMENT)
        assertThat(Category.fromMime("text/plain")).isEqualTo(Category.DOCUMENT)
        assertThat(Category.fromMime("application/msword")).isEqualTo(Category.DOCUMENT)
        assertThat(
            Category.fromMime("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        ).isEqualTo(Category.DOCUMENT)
    }

    @Test fun `archive mimes map to ARCHIVE`() {
        assertThat(Category.fromMime("application/zip")).isEqualTo(Category.ARCHIVE)
        assertThat(Category.fromMime("application/x-7z-compressed")).isEqualTo(Category.ARCHIVE)
        assertThat(Category.fromMime("application/vnd.rar")).isEqualTo(Category.ARCHIVE)
    }

    @Test fun `octet-stream falls back to extension`() {
        assertThat(Category.fromMime("application/octet-stream", "photo.HEIC"))
            .isEqualTo(Category.IMAGE)
        assertThat(Category.fromMime("application/octet-stream", "movie.mkv"))
            .isEqualTo(Category.VIDEO)
        assertThat(Category.fromMime("application/octet-stream", "song.flac"))
            .isEqualTo(Category.AUDIO)
        assertThat(Category.fromMime("application/octet-stream", "notes.md"))
            .isEqualTo(Category.DOCUMENT)
        assertThat(Category.fromMime("application/octet-stream", "bundle.tgz"))
            .isEqualTo(Category.ARCHIVE)
    }

    @Test fun `unknown yields OTHER`() {
        assertThat(Category.fromMime(null)).isEqualTo(Category.OTHER)
        assertThat(Category.fromMime("application/x-weird")).isEqualTo(Category.OTHER)
        assertThat(Category.fromMime("application/x-weird", "no_extension")).isEqualTo(Category.OTHER)
    }
}
