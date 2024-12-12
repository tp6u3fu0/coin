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
    private var isRunning = true
    private val maxItemsOnScreen = 5 // 限制屏幕上的金幣和道具數量

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gaming)

        character = findViewById(R.id.imageCharacter)
        gameArea = findViewById(R.id.gameArea)
        scoreTextView = findViewById(R.id.scoreTextView)

        setupCharacterControl()
        startItemDrop(::createCoin, dropInterval)
        startItemDrop(::createPopo, 4000L)
    }

    // 設置角色的移動控制
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

    // 通用的掉落生成邏輯
    private fun startItemDrop(createItem: () -> Unit, interval: Long) {
        Thread {
            while (isRunning) {
                if (gameArea.childCount < maxItemsOnScreen) { // 限制最大數量
                    runOnUiThread { createItem() }
                }
                Thread.sleep(interval)
            }
        }.start()
    }

    // 通用的移除行為
    private fun removeItemImmediately(item: ImageView, animator: ObjectAnimator, onRemove: () -> Unit) {
        runOnUiThread {
            gameArea.removeView(item)
            animator.cancel()
            onRemove()
        }
    }

    // 創建金幣
    private fun createCoin() {
        createItem(
            R.drawable.coin,
            3000L,
            onCollision = { increaseScore() }
        )
    }

    // 創建道具 popo
    private fun createPopo() {
        createItem(
            R.drawable.imagepopo,
            3000L,
            onCollision = { decreaseScore(10) }
        )
    }

    // 通用的生成邏輯
    private fun createItem(
        drawableResId: Int,
        duration: Long,
        onCollision: () -> Unit
    ) {
        val item = ImageView(this).apply {
            setImageResource(drawableResId)
            layoutParams = FrameLayout.LayoutParams(100, 100)
        }

        gameArea.addView(item)

        // 設置初始位置
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

    // 碰撞檢測
    private fun checkCollision(view1: View, view2: View): Boolean {
        val rect1 = android.graphics.Rect()
        val rect2 = android.graphics.Rect()
        view1.getGlobalVisibleRect(rect1)
        view2.getGlobalVisibleRect(rect2)
        return android.graphics.Rect.intersects(rect1, rect2)
    }

    // 增加分數
    private fun increaseScore() {
        score += 10
        updateScore()
        if (score % 50 == 0 && dropInterval > 400) {
            dropInterval -= 100 // 增加遊戲難度
        }
    }

    // 扣分
    private fun decreaseScore(amount: Int) {
        score = (score - amount).coerceAtLeast(0) // 保證分數不低於 0
        updateScore()
    }

    // 更新分數顯示
    private fun updateScore() {
        scoreTextView.text = "當前分數: $score"
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false // 停止生成
    }
}