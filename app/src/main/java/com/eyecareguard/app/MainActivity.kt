package com.eyecareguard.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.eyecareguard.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isOverlayOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.textHello.text = getString(R.string.hello_world)

        binding.btnToggle.setOnClickListener {
            if (!isOverlayOn) {
                requestOverlayPermissionThenStart()
            } else {
                stopOverlay()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                100
            )
        }
    }

    private fun requestOverlayPermissionThenStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } else {
            startOverlay()
        }
    }

    override fun onResume() {
        super.onResume()
        updateButtonText()
    }

    private fun startOverlay() {
        val intent = Intent(this, OverlayService::class.java)
        startForegroundService(intent)
        isOverlayOn = true
        updateButtonText()
    }

    private fun stopOverlay() {
        val intent = Intent(this, OverlayService::class.java)
        intent.action = OverlayService.ACTION_STOP
        startService(intent)
        isOverlayOn = false
        updateButtonText()
    }

    private fun updateButtonText() {
        binding.btnToggle.text = if (isOverlayOn) {
            "ปิดโหมดถนอมสายตา"
        } else {
            "เปิดโหมดถนอมสายตา"
        }
    }
}
