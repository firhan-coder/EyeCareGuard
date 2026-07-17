package com.eyecareguard.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
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
    private lateinit var prefs: SharedPreferences
    private lateinit var billingManager: BillingManager
    private var isOverlayOn = false
    private var isRestOn = false
    private var intensity = 45
    private var selectedColor = "#FFB300"

    private val tickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val millisLeft = intent?.getLongExtra(EyeRestService.EXTRA_MILLIS_LEFT, 0) ?: 0
            updateCountdownText(millisLeft)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences(EyeRestService.PREFS_NAME, MODE_PRIVATE)

        binding.textHello.text = getString(R.string.hello_world)

        intensity = prefs.getInt("saved_intensity", 45)
        selectedColor = prefs.getString("saved_color", "#FFB300") ?: "#FFB300"

        binding.seekIntensity.progress = intensity
        updatePercentText()

        setupOverlayButton()
        setupSeekBar()
        setupToneButtons()
        setupRestButton()
        setupNotifyOptions()
        setupEyeTestButton()
        setupBilling()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                100
            )
        }
    }

    private fun setupBilling() {
        billingManager = BillingManager(this) { isPremium ->
            runOnUiThread { updatePremiumUi(isPremium) }
        }
        billingManager.startConnection()

        binding.btnGoPremium.setOnClickListener {
            if (billingManager.isPremium()) {
                // ซื้อแล้ว ไม่ต้องทำอะไร (ปุ่มจะถกซ่อนอยู่แล้ว)
            } else {
                billingManager.launchPurchaseFlow(this)
            }
        }

        updatePremiumUi(billingManager.isPremium())
    }

    private fun updatePremiumUi(isPremium: Boolean) {
        if (isPremium) {
            binding.cardPremium.visibility = android.view.View.GONE
            binding.seekIntensity.max = 100
        } else {
            binding.cardPremium.visibility = android.view.View.VISIBLE
            binding.seekIntensity.max = 90
            if (intensity > 90) {
                intensity = 90
                binding.seekIntensity.progress = intensity
                updatePercentText()
            }
        }
    }

    private fun setupOverlayButton() {
        binding.btnToggle.setOnClickListener {
            if (!isOverlayOn) requestOverlayPermissionThenStart()
            else stopOverlay()
        }
    }

    private fun setupSeekBar() {
        binding.seekIntensity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                intensity = progress
                updatePercentText()
                if (isOverlayOn) sendUpdateToService()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupToneButtons() {
        binding.btnToneWarm.setOnClickListener { selectTone("#FFE0B2") }
        binding.btnToneOrange.setOnClickListener { selectTone("#FFB300") }
        binding.btnToneRed.setOnClickListener { selectTone("#E64A19") }
    }

    private fun setupRestButton() {
        binding.btnToggleRest.setOnClickListener {
            if (!isRestOn) {
                val intent = Intent(this, EyeRestService::class.java)
                startForegroundService(intent)
                isRestOn = true
            } else {
                val intent = Intent(this, EyeRestService::class.java)
                intent.action = EyeRestService.ACTION_STOP
                startService(intent)
                isRestOn = false
                binding.textCountdown.text = getString(R.string.countdown_default)
            }
            updateRestButtonText()
        }
    }

    private fun setupNotifyOptions() {
        binding.switchSound.isChecked = prefs.getBoolean(EyeRestService.KEY_SOUND, true)
        binding.switchVibrate.isChecked = prefs.getBoolean(EyeRestService.KEY_VIBRATE, true)

        binding.switchSound.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(EyeRestService.KEY_SOUND, isChecked).apply()
        }
        binding.switchVibrate.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(EyeRestService.KEY_VIBRATE, isChecked).apply()
        }
    }

    private fun setupEyeTestButton() {
        binding.btnEyeTest.setOnClickListener {
            startActivity(Intent(this, EyeTestActivity::class.java))
        }
    }

    private fun selectTone(colorHex: String) {
        selectedColor = colorHex
        if (isOverlayOn) sendUpdateToService()
    }

    private fun updatePercentText() {
        binding.textPercent.text = getString(R.string.label_blue_light, intensity)
    }

    private fun updateCountdownText(millisLeft: Long) {
        val totalSeconds = millisLeft / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        binding.textCountdown.text = if (isRestOn) {
            String.format("%02d:%02d", minutes, seconds)
        } else {
            getString(R.string.countdown_default)
        }
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

        intensity = prefs.getInt("saved_intensity", intensity)
        selectedColor = prefs.getString("saved_color", selectedColor) ?: selectedColor
        binding.seekIntensity.progress = intensity
        updatePercentText()

        updateButtonText()
        updateRestButtonText()
        val filter = IntentFilter(EyeRestService.ACTION_TICK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(tickReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(tickReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(tickReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager.endConnection()
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
        binding.btnToggle.text = if (isOverlayOn)
            getString(R.string.btn_toggle_off)
        else
            getString(R.string.btn_toggle_on)
    }

    private fun updateRestButtonText() {
        binding.btnToggleRest.text = if (isRestOn)
            getString(R.string.btn_rest_off)
        else
            getString(R.string.btn_rest_on)
    }
}
