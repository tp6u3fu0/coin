package com.example.coin

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import kotlin.random.Random

class GameActivity : AppCompatActivity() {

    private lateinit var character: ImageView
    private lateinit var gameArea: FrameLayout
    private lateinit var scoreTextView: TextView
    private var score = 0
    private var dropInterval = 1000L
    private val maxCoins = 5
    private var isRunning = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gaming)

        character = findViewById(R.id.imageCharacter)
        gameArea = findViewById(R.id.gameArea)
        scoreTextView = findViewById(R.id.scoreTextView)

        setupCharacterControl()
        startCoinDrop()
    }

    private fun setupCharacterControl() {
        var initialX = 0f
        var characterInitialX = 0f

        character.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = event.rawX
                    characterInitialX = character.x
                    character.performClick()
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialX
                    val newCharacterX = characterInitialX + deltaX
                    character.x = newCharacterX.coerceIn(
                        0f,
                        resources.displayMetrics.widthPixels - character.width.toFloat()
                    )
                }
            }
            true
        }
    }

    private fun startCoinDrop() {
        Thread {
            while (isRunning) {
                runOnUiThread {
                    createCoin()
                }
                Thread.sleep(dropInterval)
            }
        }.start()
    }

    private fun createCoin() {
        val coin = ImageView(this).apply {
            setImageResource(R.drawable.coin)
            layoutParams = FrameLayout.LayoutParams(100, 100)
        }

        gameArea.addView(coin)

        val startX = Random.nextInt(0, gameArea.width - coin.layoutParams.width)
        coin.x = startX.toFloat()
        coin.y = 0f

        val animator = ObjectAnimator.ofFloat(coin, "translationY", gameArea.height.toFloat())
        animator.duration = 3000
        animator.start()

        animator.addUpdateListener {
            if (checkCollision(coin, character)) {
                runOnUiThread {
                    increaseScore()
                    gameArea.removeView(coin)
                }
                animator.cancel()
            }
        }

        animator.doOnEnd {
            gameArea.removeView(coin)
        }
    }

    private fun checkCollision(view1: View, view2: View): Boolean {
        val rect1 = android.graphics.Rect(
            view1.x.toInt(),
            view1.y.toInt(),
            (view1.x + view1.width).toInt(),
            (view1.y + view1.height).toInt()
        )
        val rect2 = android.graphics.Rect(
            view2.x.toInt(),
            view2.y.toInt(),
            (view2.x + view2.width).toInt(),
            (view2.y + view2.height).toInt()
        )
        return android.graphics.Rect.intersects(rect1, rect2)
    }

    private fun increaseScore() {
        runOnUiThread {
            score += 10
            scoreTextView.text = "當前分數: $score"
            if (score % 50 == 0 && dropInterval > 400) {
                dropInterval -= 100
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
    }
}