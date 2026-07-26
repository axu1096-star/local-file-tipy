package com.example.filebox

import com.example.filebox.domain.FileCopier
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDate

class FileCopierTest {

    @get:Rule val temp = TemporaryFolder()

    @Test fun `copy writes file into yyyyMM subdir with original extension`() {
        val root = temp.newFolder("managed")
        val copier = FileCopier(
            managedRoot = root,
            clock = { LocalDate.of(2026, 7, 26) },
            idGen = { "id-123" }
        )
        val bytes = "hello world".toByteArray()

        val result = copier.copy(bytes.inputStream(), "photo.JPG")

        assertThat(result.file.exists()).isTrue()
        assertThat(result.bytes).isEqualTo(bytes.size.toLong())
        assertThat(result.relativePath).isEqualTo("202607/id-123.jpg")
        assertThat(result.file.readBytes()).isEqualTo(bytes)
    }

    @Test fun `copy without extension omits dot`() {
        val root = temp.newFolder("managed")
        val copier = FileCopier(
            managedRoot = root,
            clock = { LocalDate.of(2026, 1, 1) },
            idGen = { "abc" }
        )
        val result = copier.copy(byteArrayOf(1, 2, 3).inputStream(), "noext")
        assertThat(result.relativePath).isEqualTo("202601/abc")
    }

    @Test fun `unreasonably long extension is dropped`() {
        val root = temp.newFolder("managed")
        val copier = FileCopier(
            managedRoot = root,
            clock = { LocalDate.of(2026, 1, 1) },
            idGen = { "abc" }
        )
        val result = copier.copy(byteArrayOf(0).inputStream(), "weird.thisistoolongforanext")
        assertThat(result.relativePath).isEqualTo("202601/abc")
    }
}
