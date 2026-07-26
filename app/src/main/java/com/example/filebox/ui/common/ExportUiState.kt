package com.example.filebox.ui.common

import com.example.filebox.domain.FileExporter

data class ExportUiState(
    val active: Boolean = false,
    val progress: FileExporter.Progress = FileExporter.Progress(0, 0, ""),
    val result: FileExporter.Result? = null
)