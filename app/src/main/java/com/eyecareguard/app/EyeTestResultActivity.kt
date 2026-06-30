package com.eyecareguard.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.eyecareguard.app.databinding.ActivityEyeTestResultBinding
import kotlin.math.roundToInt

class EyeTestResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEyeTestResultBinding
    private var recommendedIntensity = 30
    private var recommendedColor = "#FFB300"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEyeTestResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val score = intent.getIntExtra("SCORE", 0)
        calculateRecommendation(score)
        showResult(score)

        binding.btnApplyFilter.setOnClickListener {
            applyFilterAndOpen()
        }

        binding.btnBackToMain.setOnClickListener {
            finish()
        }
    }

    private fun calculateRecommendation(score: Int) {
        // คะแนนเต็ม 15, แปลงเป็น % กรองแสงแบบต่อเนื่อง 15% - 90%
        val minIntensity = 15
        val maxIntensity = 90
        val maxScore = 15

        recommendedIntensity = minIntensity + ((maxIntensity - minIntensity) * score / maxScore.toFloat()).roundToInt()

        // เลือกโทนสีตามความรุนแรง
        recommendedColor = when {
            score <= 5 -> "#FFE0B2"  // อุ่น
            score <= 10 -> "#FFB300" // ส้ม
            else -> "#E64A19"        // แดง (กรองแสงสีฟ้าได้ดีที่สุด)
        }

        binding.textRecommendedFilter.text = "แนะนำกรองแสง $recommendedIntensity%"
    }

    private fun showResult(score: Int) {
        when {
            score <= 5 -> {
                binding.textResultEmoji.text = "😊"
                binding.textResultLevel.text = "สายตาปกติ"
                binding.textResultAdvice.text =
                    "ดวงตาของคุณยังไม่มีอาการล้ามากนัก แต่ควรพักสายตาตามกฎ 20-20-20 อย่างสม่ำเสมอเพื่อรักษาสุขภาพตาให้ดีต่อไป"
            }
            score <= 10 -> {
                binding.textResultEmoji.text = "😐"
                binding.textResultLevel.text = "เริ่มมีอาการล้าตา"
                binding.textResultAdvice.text =
                    "คุณเริ่มมีสัญญาณความล้าของดวงตา ควรเปิดโหมดถนอมสายตาและพักสายตาทุก 20 นาทีอย่างจริงจัง รวมถึงปรับแสงหน้าจอให้เหมาะสม"
            }
            else -> {
                binding.textResultEmoji.text = "😣"
                binding.textResultLevel.text = "ล้าตามาก ควรพักผ่อน"
                binding.textResultAdvice.text =
                    "ดวงตาของคุณล้ามากแล้ว ควรพักสายตาทันที ลดเวลาหน้าจอ และหากอาการไม่ดีขึ้นควรปรึกษาจักษุแพทย์"
            }
        }
    }

    private fun applyFilterAndOpen() {
        val prefs = getSharedPreferences(EyeRestService.PREFS_NAME, MODE_PRIVATE)
        prefs.edit()
            .putInt("saved_intensity", recommendedIntensity)
            .putString("saved_color", recommendedColor)
            .apply()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } else {
            val intent = Intent(this, OverlayService::class.java).apply {
                putExtra(OverlayService.EXTRA_INTENSITY, recommendedIntensity)
                putExtra(OverlayService.EXTRA_COLOR, recommendedColor)
            }
            startForegroundService(intent)
        }

        finish()
    }
}
