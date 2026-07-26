package com.example.filebox.data.repo

import com.example.filebox.data.db.TagDao
import com.example.filebox.data.entity.Tag
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepository @Inject constructor(private val dao: TagDao) {
    fun observeAll(): Flow<List<Tag>> = dao.observeAll()

    suspend fun createOrGet(name: String, colorArgb: Int? = null): Long {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return -1L
        dao.findByName(trimmed)?.let { return it.id }
        val id = dao.insert(Tag(name = trimmed, colorArgb = colorArgb))
        return if (id > 0) id else dao.findByName(trimmed)?.id ?: -1L
    }

    suspend fun rename(id: Long, name: String) {
        val existing = dao.findById(id) ?: return
        dao.update(existing.copy(name = name.trim()))
    }

    suspend fun setColor(id: Long, colorArgb: Int?) {
        val existing = dao.findById(id) ?: return
        dao.update(existing.copy(colorArgb = colorArgb))
    }

    suspend fun delete(id: Long) = dao.deleteById(id)
}
