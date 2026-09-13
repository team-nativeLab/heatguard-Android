package com.nativelap.heartguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nativelap.heartguard.navigation.HeartGuardNavHost
import com.nativelap.heartguard.ui.theme.HeartGuardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HeartGuardTheme {
                HeartGuardNavHost()
            }
        }
    }
}
