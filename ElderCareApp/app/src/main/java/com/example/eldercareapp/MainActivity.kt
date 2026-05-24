package com.example.eldercareapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.eldercareapp.ui.screen.ElderCareAppRoot
import com.example.eldercareapp.ui.theme.ElderCareAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ElderCareAppTheme(dynamicColor = false) {
                ElderCareAppRoot(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
