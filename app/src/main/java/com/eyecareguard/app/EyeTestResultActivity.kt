package com.eyecareguard.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.eyecareguard.app.databinding.ActivityEyeTestResultBinding

class EyeTestResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEyeTestResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEyeTestResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val score = intent.getIntExtra("SCORE", 0)
        showResult(score)

        binding.btnBackToMain.setOnClickListener {
            finish()
        }
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
}
