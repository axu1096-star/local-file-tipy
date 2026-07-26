package com.example.filebox.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.filebox.R

@Composable
fun ExportProgressDialog(state: ExportUiState) {
    if (!state.active) return
    val total = state.progress.total.coerceAtLeast(1)
    val done = state.progress.done
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        title = { Text(stringResource(R.string.batch_save)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.batch_save_progress, done, total),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { done.toFloat() / total.toFloat() },
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.progress.currentName.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = state.progress.currentName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    )
}
