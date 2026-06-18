package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.ui.ShopAppUI
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ShopViewModel
import com.example.viewmodel.ShopViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-edge support for beautiful, immersive status/navigation bar styling
        enableEdgeToEdge()
        
        // Retrieve the centralized state manager via Android ViewModelProvider
        val viewModel = ViewModelProvider(
            this,
            ShopViewModelFactory(application)
        )[ShopViewModel::class.java]

        setContent {
            MyApplicationTheme {
                ShopAppUI(viewModel)
            }
        }
    }
}
