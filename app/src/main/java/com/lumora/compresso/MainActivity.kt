package com.lumora.compresso

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lumora.compresso.ui.CompressoApp
import com.lumora.compresso.ui.theme.CompressoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CompressoTheme {
                CompressoApp()
            }
        }
    }
}