package com.eyecareguard.app

import android.os.Bundle
import android.os.CountDownTimer
import androidx.appcompat.app.AppCompatActivity
import com.eyecareguard.app.databinding.ActivityRestReminderBinding

class RestReminderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRestReminderBinding
    private var timer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRestReminderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startCountdown()

        binding.btnDone.setOnClickListener {
            timer?.cancel()
            finish()
        }
    }

    private fun startCountdown() {
        binding.textCountdown.text = "20"
        binding.textInstruction.text = "มองไปที่สิ่งที่อยู่ไกล 20 ฟุต\n(ประมาณ 6 เมตร)"

        timer = object : CountDownTimer(20000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt()
                binding.textCountdown.text = secondsLeft.toString()
            }

            override fun onFinish() {
                binding.textCountdown.text = "✅"
                binding.textInstruction.text = "พักสายตาเสร็จแล้ว!\nกลับไปทำงานต่อได้เลย"
                binding.btnDone.text = "กลับ"
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}
