package com.example.pocketrates

import android.content.Intent
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Boot1 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_boot1)

        window.insetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())

        // Optionally, set immersive behavior: bars reappear on swipe, then hide again
        window.insetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        val navigateButton = findViewById<ImageButton>(R.id.btnNext)

// Set the click listener
        navigateButton.setOnClickListener {
            // This code runs when the button is clicked
            val intent_boot2 = Intent(this, Boot2   ::class.java)
            startActivity(intent_boot2)
            finish()
        }


    }
}