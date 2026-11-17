package com.teczo.khatabill

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.teczo.khatabill.ui.navigation.AppNavHost
import com.teczo.khatabill.ui.splash.VyroSplash
import com.teczo.khatabill.ui.theme.KhataBillTheme
import dagger.hilt.android.AndroidEntryPoint

/** Single activity. Extends AppCompatActivity so the app language can change without a restart. */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Edge-to-edge with automatic light/dark status and navigation bar icons (readable on both themes).
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            KhataBillTheme {
                // The app loads underneath while the opening animation plays on top of it.
                var splashDone by rememberSaveable { mutableStateOf(false) }
                Box {
                    AppNavHost()
                    if (!splashDone) VyroSplash(onFinished = { splashDone = true })
                }
            }
        }
    }
}
