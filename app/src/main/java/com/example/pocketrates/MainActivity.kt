package com.example.pocketrates

import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.pocketrates.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var fragmentManager : FragmentManager
    private lateinit var binding : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        gotoFrag(dashboardFrag())

        //Hides phone nav bar
        window.insetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        //For the navbar to appear, swipe
        window.insetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnDashboard.setOnClickListener{
            setSelectedButton(binding.btnDashboard)
            gotoFrag(dashboardFrag())
        }
        binding.btnExchange.setOnClickListener{
            setSelectedButton(binding.btnExchange)
            gotoFrag(exchangeFrag())
        }

        binding.btnTransaction.setOnClickListener{
            setSelectedButton(binding.btnTransaction)
            gotoFrag(transacFrag())
        }

        binding.btnSettings.setOnClickListener{
            setSelectedButton(binding.btnSettings)
            gotoFrag(settingFrag())
        }


    }

    private fun setSelectedButton(selectedButton: ImageButton) {
        // Deselect all buttons
        binding.btnDashboard.isSelected = false
        binding.btnExchange.isSelected = false
        binding.btnTransaction.isSelected = false
        binding.btnSettings.isSelected = false


        // Select the clicked one
        selectedButton.isSelected = true
    }


    private fun gotoFrag(fragment: Fragment) {
        fragmentManager = supportFragmentManager
        fragmentManager.beginTransaction().replace(R.id.fragmentContainer, fragment).commit()
    }





}