package com.example.filebox.ui.detail.preview

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import com.example.filebox.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val MAX_BYTES = 200 * 1024

@Composable
fun TextPreview(file: File, modifier: Modifier = Modifier) {
    var content by remember { mutableStateOf<String?>(null) }
    var truncated by remember { mutableStateOf(false) }
    LaunchedEffect(file.absolutePath) {
        withContext(Dispatchers.IO) {
            val buf = ByteArray(MAX_BYTES)
            file.inputStream().use { input ->
                val n = input.read(buf)
                truncated = file.length() > MAX_BYTES
                content = if (n <= 0) "" else String(buf, 0, n)
            }
        }
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        if (truncated) {
            Text(
                text = stringResource(R.string.detail_text_truncated),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = content ?: "",
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
