package com.example.filebox

import com.example.filebox.domain.FileExporter
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FileExporterTest {

    @Test fun `unique name returns original when unused`() {
        val used = mutableSetOf<String>()
        assertThat(FileExporter.uniqueName("photo.jpg", used)).isEqualTo("photo.jpg")
        assertThat(used).contains("photo.jpg")
    }

    @Test fun `unique name appends counter before extension on collision`() {
        val used = mutableSetOf("photo.jpg")
        assertThat(FileExporter.uniqueName("photo.jpg", used)).isEqualTo("photo (1).jpg")
        assertThat(FileExporter.uniqueName("photo.jpg", used)).isEqualTo("photo (2).jpg")
    }

    @Test fun `unique name collision is case insensitive`() {
        val used = mutableSetOf("Photo.JPG")
        assertThat(FileExporter.uniqueName("photo.jpg", used)).isEqualTo("photo (1).jpg")
    }

    @Test fun `unique name handles files without extension`() {
        val used = mutableSetOf("README")
        assertThat(FileExporter.uniqueName("README", used)).isEqualTo("README (1)")
    }

    @Test fun `unique name handles dotfiles without treating leading dot as extension`() {
        val used = mutableSetOf(".gitignore")
        assertThat(FileExporter.uniqueName(".gitignore", used)).isEqualTo(".gitignore (1)")
    }

    @Test fun `blank name falls back to file`() {
        val used = mutableSetOf<String>()
        assertThat(FileExporter.uniqueName("", used)).isEqualTo("file")
    }
}
