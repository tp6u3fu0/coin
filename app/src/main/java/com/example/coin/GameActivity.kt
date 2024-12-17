package com.example.coin

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
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
    private lateinit var livesTextView: TextView
    private lateinit var timerTextView: TextView

    private var score = 0
    private var dropInterval = 1000L
    private var popoDropInterval = 4000L // 初始 popo 的生成間隔
    private var itemDropDuration = 3000L // 掉落動畫初始時間
    private var isRunning = true
    private val maxItemsOnScreen = 5 // 限制屏幕上的金幣和道具數量

    private var lives = 1
    private var mode = "CLASSIC" // "CLASSIC" or "CHALLENGE"
    private var highestClassicScore = 0
    private var highestChallengeScore = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gaming)

        character = findViewById(R.id.imageCharacter)
        gameArea = findViewById(R.id.gameArea)
        scoreTextView = findViewById(R.id.scoreTextView)
        livesTextView = findViewById(R.id.livesTextView)
        timerTextView = findViewById(R.id.timerTextView)

        // 接收從上一頁傳遞的模式參數
        mode = intent.getStringExtra("GAME_MODE") ?: "CLASSIC"

        setupGameUI()
        setupCharacterControl()

        startItemDrop(::createCoin, dropInterval)
        startItemDrop(::createPopo, popoDropInterval)

        if (mode == "CHALLENGE") {
            startCountdownTimer()
        }
    }

    private fun setupGameUI() {
        livesTextView.visibility = if (mode == "CLASSIC") View.VISIBLE else View.GONE
        timerTextView.visibility = if (mode == "CHALLENGE") View.VISIBLE else View.GONE
        livesTextView.text = "生命值: $lives"
    }

    private fun startCountdownTimer() {
        object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timerTextView.text = "剩餘時間: ${millisUntilFinished / 1000}秒"
            }

            override fun onFinish() {
                isRunning = false
                endGame()
            }
        }.start()
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

    private fun startItemDrop(createItem: () -> Unit, initialInterval: Long) {
        Thread {
            var interval = initialInterval
            while (isRunning) {
                if (gameArea.childCount < maxItemsOnScreen) {
                    runOnUiThread { createItem() }
                }
                Thread.sleep(interval)

                interval = if (createItem == ::createPopo) popoDropInterval else dropInterval
            }
        }.start()
    }

    private fun removeItemImmediately(item: ImageView, animator: ObjectAnimator, onRemove: () -> Unit) {
        runOnUiThread {
            gameArea.removeView(item)
            animator.cancel()
            onRemove()
        }
    }

    private fun createCoin() {
        createItem(
            R.drawable.coin,
            3000L,
            onCollision = { increaseScore() }
        )
    }

    private fun createPopo() {
        createItem(
            R.drawable.imagepopo,
            3000L,
            onCollision = {
                if (mode == "CLASSIC") {
                    decreaseLives()
                } else {
                    decreaseScore(10)
                }
            }
        )
    }

    private fun createItem(
        drawableResId: Int,
        duration: Long = itemDropDuration, // 使用動態掉落時間
        onCollision: () -> Unit
    ) {
        val item = ImageView(this).apply {
            setImageResource(drawableResId)
            layoutParams = FrameLayout.LayoutParams(100, 100)
        }

        gameArea.addView(item)

        val startX = Random.nextInt(0, gameArea.width - item.layoutParams.width)
        item.x = startX.toFloat()
        item.y = 0f

        val animator = ObjectAnimator.ofFloat(item, "translationY", gameArea.height.toFloat())
        animator.duration = duration
        animator.start()

        var isRemoved = false
        animator.addUpdateListener {
            if (!isRemoved && checkCollision(item, character)) {
                isRemoved = true
                removeItemImmediately(item, animator, onCollision)
            }
        }

        animator.doOnEnd {
            if (!isRemoved) {
                gameArea.removeView(item)
            }
        }
    }

    private fun checkCollision(view1: View, view2: View): Boolean {
        val rect1 = android.graphics.Rect()
        val rect2 = android.graphics.Rect()
        view1.getGlobalVisibleRect(rect1)
        view2.getGlobalVisibleRect(rect2)
        return android.graphics.Rect.intersects(rect1, rect2)
    }

    private fun increaseScore() {
        score += 10
        updateScore()

        if (score % 50 == 0) {
            if (itemDropDuration > 1000) {
                itemDropDuration -= 200
            }

            if (popoDropInterval > 1000) {
                popoDropInterval -= 200
            }

            if (dropInterval > 400) {
                dropInterval -= 100
            }
        }
    }

    private fun decreaseLives() {
        lives--
        livesTextView.text = "生命值: $lives"
        if (lives <= 0) {
            isRunning = false
            endGame()
        }
    }

    private fun decreaseScore(amount: Int) {
        score = (score - amount).coerceAtLeast(0) // 保證分數不低於 0
        updateScore()
    }

    private fun updateScore() {
        scoreTextView.text = "當前分數: $score"
    }

    private fun endGame() {
        // 判斷並更新最高分
        if (mode == "CLASSIC") {
            if (score > highestClassicScore) {
                highestClassicScore = score
            }
        } else {
            if (score > highestChallengeScore) {
                highestChallengeScore = score
            }
        }

        // 獲取最新的最高分
        val updatedHighestScore = if (mode == "CLASSIC") highestClassicScore else highestChallengeScore

        // 跳轉到結算頁面並傳遞相關數據
        val intent = Intent(this, EndGameActivity::class.java).apply {
            putExtra("FINAL_SCORE", score) // 當前分數
            putExtra("HIGHEST_SCORE", updatedHighestScore) // 最新最高分數
        }
        startActivity(intent)
        finish()
    }


    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
    }
}
