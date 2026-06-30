package com.eyecareguard.app

import android.content.Intent
import android.os.Bundle
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import com.eyecareguard.app.databinding.ActivityEyeTestBinding

class EyeTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEyeTestBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEyeTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSubmitTest.setOnClickListener {
            calculateResult()
        }
    }

    private fun getScoreFromGroup(group: RadioGroup): Int {
        return when (group.checkedRadioButtonId) {
            group.getChildAt(0).id -> 0
            group.getChildAt(1).id -> 1
            group.getChildAt(2).id -> 2
            group.getChildAt(3).id -> 3
            else -> -1
        }
    }

    private fun calculateResult() {
        val scores = listOf(
            getScoreFromGroup(binding.radioGroup1),
            getScoreFromGroup(binding.radioGroup2),
            getScoreFromGroup(binding.radioGroup3),
            getScoreFromGroup(binding.radioGroup4),
            getScoreFromGroup(binding.radioGroup5)
        )

        if (scores.any { it == -1 }) {
            android.widget.Toast.makeText(
                this, "กรุณาตอบให้ครบทุกข้อ", android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }

        val total = scores.sum()

        val intent = Intent(this, EyeTestResultActivity::class.java)
        intent.putExtra("SCORE", total)
        startActivity(intent)
        finish()
    }
}
