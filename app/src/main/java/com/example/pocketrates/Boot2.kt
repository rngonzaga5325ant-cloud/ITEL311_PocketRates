package com.example.pocketrates

import android.content.Intent
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Boot2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_boot2)

        window.insetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        window.insetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        val navigateButton = findViewById<Button>(R.id.btnContinue)
        val btnSkip = findViewById<Button>(R.id.btnSkip)
// Set the click listener
        navigateButton.setOnClickListener {
            // This code runs when the button is clicked
            val intent_boot3 = Intent(this, Boot3::class.java)
            startActivity(intent_boot3)
            finish()
        }

        btnSkip.setOnClickListener {
            val intentSkip = Intent(this, Boot4::class.java)
            startActivity(intentSkip)
            finish()

        }
    }
}