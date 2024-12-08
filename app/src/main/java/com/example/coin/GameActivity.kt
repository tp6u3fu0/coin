package com.example.coin
import android.os.Bundle
import android.view.MotionEvent
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
class GameActivity : AppCompatActivity(){
    private lateinit var character: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        // 取得角色的視圖
        character = findViewById(R.id.character)

        // 設置觸控事件
        setupTouchListener()
}