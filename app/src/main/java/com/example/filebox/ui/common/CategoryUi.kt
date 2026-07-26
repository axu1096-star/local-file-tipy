package com.example.filebox.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.filebox.R
import com.example.filebox.domain.Category

fun Category.icon(): ImageVector = when (this) {
    Category.IMAGE -> Icons.Filled.Image
    Category.VIDEO -> Icons.Filled.Videocam
    Category.AUDIO -> Icons.Filled.AudioFile
    Category.DOCUMENT -> Icons.Filled.Description
    Category.ARCHIVE -> Icons.Filled.Archive
    Category.OTHER -> Icons.Filled.InsertDriveFile
}

fun Category.labelRes(): Int = when (this) {
    Category.IMAGE -> R.string.category_image
    Category.VIDEO -> R.string.category_video
    Category.AUDIO -> R.string.category_audio
    Category.DOCUMENT -> R.string.category_document
    Category.ARCHIVE -> R.string.category_archive
    Category.OTHER -> R.string.category_other
}
