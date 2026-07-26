package com.example.filebox.data.repo

import com.example.filebox.data.db.TagDao
import com.example.filebox.data.entity.Tag
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepository @Inject constructor(private val dao: TagDao) {
    fun observeAll(): Flow<List<Tag>> = dao.observeAll()

    suspend fun findById(id: Long): Tag? = dao.findById(id)

    suspend fun createOrGet(name: String, parentId: Long? = null, colorArgb: Int? = null): Long {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return -1L
        dao.findByNameInParent(trimmed, parentId)?.let { return it.id }
        val id = dao.insert(Tag(name = trimmed, colorArgb = colorArgb, parentId = parentId))
        return if (id > 0) id else dao.findByNameInParent(trimmed, parentId)?.id ?: -1L
    }

    suspend fun rename(id: Long, name: String) {
        val existing = dao.findById(id) ?: return
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        dao.update(existing.copy(name = trimmed))
    }

    suspend fun setColor(id: Long, colorArgb: Int?) {
        val existing = dao.findById(id) ?: return
        dao.update(existing.copy(colorArgb = colorArgb))
    }

    /**
     * Move [id] under [newParentId] (or to root when null).
     * Prevents cycles: refuses to set the parent to itself or to any of its descendants.
     * Returns true when the change was applied.
     */
    suspend fun reparent(id: Long, newParentId: Long?): Boolean {
        if (newParentId == id) return false
        if (newParentId != null && isDescendant(newParentId, id)) return false
        dao.updateParent(id, newParentId)
        return true
    }

    private suspend fun isDescendant(candidate: Long, ancestor: Long): Boolean {
        var current: Long? = candidate
        val guard = mutableSetOf<Long>()
        while (current != null) {
            if (!guard.add(current)) return false
            if (current == ancestor) return true
            current = dao.findById(current)?.parentId
        }
        return false
    }

    suspend fun delete(id: Long) = dao.deleteById(id)
}
