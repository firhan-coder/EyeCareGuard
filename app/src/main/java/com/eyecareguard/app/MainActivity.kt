package com.eyecareguard.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.eyecareguard.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.textHello.text = getString(R.string.hello_world)
    }
}
