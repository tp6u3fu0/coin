package com.example.coin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class EndGameActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.finish)

        // 從 Intent 中取得傳遞過來的分數
        val finalScore = intent.getIntExtra("FINAL_SCORE", 0)
        val highestScore = intent.getIntExtra("HIGHEST_SCORE", 0)

        // 找到 TextView 和 Button 元件
        val scoreTextView = findViewById<TextView>(R.id.textView3)
        val highestScoreTextView = findViewById<TextView>(R.id.textView4)
        val returnButton = findViewById<Button>(R.id.button4)

        // 更新 TextView 的文字顯示分數
        scoreTextView.text = "獲得: $finalScore 分"
        highestScoreTextView.text ="最高獲得: $highestScore 分"
        // 設定 Button 的點擊事件，返回 choose.xml
        returnButton.setOnClickListener {
            val intent = Intent(this, ChooseActivity::class.java)
            startActivity(intent)
            finish() // 避免返回鍵返回到結算頁面
        }
    }
}