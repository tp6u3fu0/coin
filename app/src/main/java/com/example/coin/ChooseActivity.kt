package com.example.coin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
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

        val classicButton: ImageView = findViewById(R.id.classicButton)
        classicButton.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            intent.putExtra("GAME_MODE", "CLASSIC") // 傳遞經典模式參數
            startActivity(intent)
        }

        val challengeButton: ImageView = findViewById(R.id.timeButton)
        challengeButton.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            intent.putExtra("GAME_MODE", "CHALLENGE") // 傳遞挑戰模式參數
            startActivity(intent)
        }
        val funButton: ImageView = findViewById(R.id.funButton)
        funButton.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            intent.putExtra("GAME_MODE", "FUN") // 傳遞挑戰模式參數
            startActivity(intent)
        }
    }
}
