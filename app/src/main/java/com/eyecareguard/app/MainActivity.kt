package com.eyecareguard.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.eyecareguard.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isOverlayOn = false
    private var intensity = 45
    private var selectedColor = "#FFB300"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.textHello.text = getString(R.string.hello_world)

        binding.seekIntensity.progress = intensity
        updatePercentText()

        binding.btnToggle.setOnClickListener {
            if (!isOverlayOn) {
                requestOverlayPermissionThenStart()
            } else {
                stopOverlay()
            }
        }

        binding.seekIntensity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                intensity = progress
                updatePercentText()
                if (isOverlayOn) sendUpdateToService()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.btnToneWarm.setOnClickListener { selectTone("#FFE0B2") }
        binding.btnToneOrange.setOnClickListener { selectTone("#FFB300") }
        binding.btnToneRed.setOnClickListener { selectTone("#E64A19") }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                100
            )
        }
    }

    private fun selectTone(colorHex: String) {
        selectedColor = colorHex
        if (isOverlayOn) sendUpdateToService()
    }

    private fun updatePercentText() {
        binding.textPercent.text = "กรองแสงสีฟ้าได้ ${intensity}%"
    }

    private fun sendUpdateToService() {
        val intent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_UPDATE
            putExtra(OverlayService.EXTRA_INTENSITY, intensity)
            putExtra(OverlayService.EXTRA_COLOR, selectedColor)
        }
        startService(intent)
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
        val intent = Intent(this, OverlayService::class.java).apply {
            putExtra(OverlayService.EXTRA_INTENSITY, intensity)
            putExtra(OverlayService.EXTRA_COLOR, selectedColor)
        }
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
