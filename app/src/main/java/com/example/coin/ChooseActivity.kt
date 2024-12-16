package com.example.coin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class ChooseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.choose)

        val backButton: Button = findViewById(R.id.back)
        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        val classicButton: Button = findViewById(R.id.classicButton)
        classicButton.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            intent.putExtra("GAME_MODE", "CLASSIC") // 傳遞經典模式參數
            startActivity(intent)
        }

        val challengeButton: Button = findViewById(R.id.timeButton)
        challengeButton.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            intent.putExtra("GAME_MODE", "CHALLENGE") // 傳遞挑戰模式參數
            startActivity(intent)
        }
        val funButton: Button = findViewById(R.id.funButton)
        funButton.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            intent.putExtra("GAME_MODE", "FUN") // 傳遞挑戰模式參數
            startActivity(intent)
        }
    }
}
