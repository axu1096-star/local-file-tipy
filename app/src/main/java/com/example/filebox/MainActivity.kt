package com.example.filebox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.filebox.ui.FileboxNavHost
import com.example.filebox.ui.theme.FileboxTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FileboxTheme {
                FileboxNavHost()
            }
        }
    }
}
