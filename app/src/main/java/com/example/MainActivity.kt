package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.ui.TripSplitApp
import com.example.ui.TripViewModel
import com.example.ui.TripViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val factory = TripViewModelFactory(application)
    val viewModel = ViewModelProvider(this, factory)[TripViewModel::class.java]

    setContent {
      MyApplicationTheme {
        TripSplitApp(viewModel = viewModel)
      }
    }
  }
}

