package com.example.filebox.data.db

import androidx.room.TypeConverter
import com.example.filebox.domain.Category

class Converters {
    @TypeConverter fun categoryToString(c: Category): String = c.name
    @TypeConverter fun stringToCategory(s: String): Category =
        runCatching { Category.valueOf(s) }.getOrDefault(Category.OTHER)
}
