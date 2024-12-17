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
    private lateinit var pauseButton: ImageView

    private var score = 0
    private var dropInterval = 1000L
    private var popoDropInterval = 4000L // 初始 popo 的生成間隔
    private var itemDropDuration = 3000L // 掉落動畫初始時間
    private var isRunning = true
    private val maxItemsOnScreen = 5 // 限制屏幕上的金幣和道具數量
    private var isPaused = false // 控制暫停狀態
    private var itemAnimators = mutableListOf<ObjectAnimator>() // 儲存所有物件的動畫
    private var countdownTimer: CountDownTimer? = null // 倒數計時器
    private var remainingTime = 0L // 倒數計時剩餘時間

    private var lives = 5
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
        pauseButton = findViewById(R.id.imagePause)

        // 接收從上一頁傳遞的模式參數
        mode = intent.getStringExtra("GAME_MODE") ?: "CLASSIC"

        setupGameUI()
        setupCharacterControl()

        pauseButton.setOnClickListener {
            togglePause()
        }

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
        remainingTime = 60000 // 初始時間 60 秒

        countdownTimer = object : CountDownTimer(remainingTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTime = millisUntilFinished // 記錄剩餘時間
                timerTextView.text = "剩餘時間: ${millisUntilFinished / 1000}秒"
            }

            override fun onFinish() {
                isRunning = false
                endGame()
            }
        }.start()
    }

    private fun pauseCountdownTimer() {
        countdownTimer?.cancel()
    }

    private fun resumeCountdownTimer() {
        if (remainingTime > 0) { // 確保有剩餘時間時才恢復
            countdownTimer?.cancel()
            countdownTimer = object : CountDownTimer(remainingTime, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    remainingTime = millisUntilFinished
                    timerTextView.text = "剩餘時間: ${millisUntilFinished / 1000}秒"
                }

                override fun onFinish() {
                    if (isRunning) { // 確保遊戲仍在執行
                        isRunning = false
                        endGame()
                    }
                }
            }.start()
        }
    }



    private fun togglePause() {
        if (!isPaused) {
            isPaused = true
            pauseItemAnimations()
            disableCharacterControl()
            pauseCountdownTimer()
            pauseButton.setImageResource(R.drawable.play_icon)
        } else {
            // 確保遊戲尚未結束時才恢復
            if (isRunning) {
                isPaused = false
                resumeItemAnimations()
                enableCharacterControl()
                resumeCountdownTimer()
                pauseButton.setImageResource(R.drawable.pause_icon)
            }
        }
    }



    private fun setupCharacterControl() {
        var initialX = 0f
        var characterInitialX = 0f

        character.setOnTouchListener { _, event ->
            if (isPaused) return@setOnTouchListener true // 暫停時禁止移動

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

    // 禁用玩家控制（暫停用）
    private fun disableCharacterControl() {
        character.setOnTouchListener { _, _ -> true }
    }

    // 啟用玩家控制（恢復用）
    private fun enableCharacterControl() {
        setupCharacterControl()
    }


    private fun startItemDrop(createItem: () -> Unit, initialInterval: Long) {
        Thread {
            var interval = initialInterval
            while (isRunning) {
                if (!isPaused) { // 遊戲未暫停時執行
                    if (gameArea.childCount < maxItemsOnScreen) {
                        runOnUiThread { createItem() }
                    }
                    Thread.sleep(interval)
                } else {
                    Thread.sleep(100) // 遊戲暫停時減少 CPU 資源消耗
                }

                interval = if (createItem == ::createPopo) popoDropInterval else dropInterval
            }
        }.start()
    }


    private fun removeItemImmediately(item: ImageView, animator: ObjectAnimator, onRemove: () -> Unit) {
        runOnUiThread {
            gameArea.removeView(item)
            animator.cancel()
            itemAnimators.remove(animator) // 移除失效的動畫
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

        itemAnimators.add(animator)

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


    private fun pauseItemAnimations() {
        itemAnimators.forEach { animator ->
            if (animator.isRunning) {
                animator.pause()
            }
        }
    }

    private fun resumeItemAnimations() {
        itemAnimators.forEach { animator ->
            if (animator.isPaused) {
                animator.resume()
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
        if (!isRunning) return // 避免重複執行

        isRunning = false // 結束遊戲狀態
        pauseCountdownTimer()
        pauseItemAnimations()

        val highestScore = if (mode == "CLASSIC") highestClassicScore else highestChallengeScore

        if (score > highestScore) {
            if (mode == "CLASSIC") highestClassicScore = score else highestChallengeScore = score
        }

        val intent = Intent(this, EndGameActivity::class.java)
        intent.putExtra("FINAL_SCORE", score)
        intent.putExtra("HIGHEST_SCORE", highestScore)
        intent.putExtra("GAME_MODE", mode)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRunning) {
            isRunning = false
            countdownTimer?.cancel()
        }
    }

}
