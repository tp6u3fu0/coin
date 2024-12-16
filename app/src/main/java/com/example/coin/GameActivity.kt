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

    private lateinit var funModeItems: FunModeItems
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

    private var lives = 5
    private var mode = "CLASSIC" // "CLASSIC" or "CHALLENGE"
    private var highestClassicScore = 0
    private var highestChallengeScore = 0

    private var isMagnetActive = false // 磁鐵是否激活
    private val magnetDuration = 5000L // 磁鐵效果持續時間（5秒）
    private val magnetRange = 1000 // 磁鐵吸引範圍（像素）
    private val magnetDropProbability = 0.6 // 60% 掉落概率
    private val magnetCheckInterval = 10000L // 每分鐘檢查一次（毫秒）

    private var popoScaleFactor = 1f // 大便的當前縮放比例，默認為 1
    private val popoScaleIncrease = 2.5f // 碰到道具後大便變大的比例
    private val popoGrowDuration = 1000L // 大便變大後持續 1 秒



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

        funModeItems = FunModeItems(gameArea, character)

        startItemDrop(::createCoin, dropInterval)
        startItemDrop(::createPopo, popoDropInterval)
        startMagnetDropCheck() // 啟動磁鐵掉落檢測
        startGrowPopoDropCheck() // 啟動大便變大的道具掉落檢查

        if (mode == "CHALLENGE") {
            startCountdownTimer()
        }else if (mode == "FUN"){
            startFunModeItems() // 啟動趣味模式道具
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
            onCollision = { increaseScore() },
            tag = "coin"
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

            },scaleFactor = popoScaleFactor // 使用當前的縮放比例
        )
    }

    private fun createMagnet() {
        createItem(
            R.drawable.imagemagnetic, // 磁鐵的圖片資源
            3000L, // 掉落時間
            onCollision = { activateMagnet() } // 碰撞後激活磁鐵效果
        )
    }


    private fun createItem(
        drawableResId: Int,
        duration: Long = itemDropDuration,
        onCollision: () -> Unit,
        tag: String? = null,
        scaleFactor: Float = 1f // 默認縮放比例
    ) {
        val item = ImageView(this).apply {
            setImageResource(drawableResId)
            layoutParams = FrameLayout.LayoutParams(
                (100 * scaleFactor).toInt(),
                (100 * scaleFactor).toInt()
            )
            this.tag = tag
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



    private fun activateMagnet() {
        if (isMagnetActive) return // 如果已經激活，跳過

        isMagnetActive = true
        // 顯示磁鐵效果（例如改變角色圖片或顯示動畫）
        character.setImageResource(R.drawable.imagemagnetic)

        // 定時檢測並吸引金幣
        Thread {
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < magnetDuration) {
                runOnUiThread { attractCoins() }
                Thread.sleep(200) // 每 200 毫秒檢測一次
            }
            // 磁鐵效果結束
            runOnUiThread {
                isMagnetActive = false
                character.setImageResource(R.drawable.hand) // 還原角色圖片
            }
        }.start()
    }
    private fun attractCoins() {
        for (i in 0 until gameArea.childCount) {
            val view = gameArea.getChildAt(i)
            if (view is ImageView && view.tag == "coin") {
                // 檢測金幣是否在吸引範圍內
                if (isWithinMagnetRange(view)) {
                    // 動畫將金幣移動到角色的位置
                    animateCoinToCharacter(view)
                }
            }
        }
    }
    private fun animateCoinToCharacter(coin: ImageView) {
        // 計算角色的中心位置
        val targetX = character.x + character.width / 2 - coin.width / 2
        val targetY = character.y + character.height / 2 - coin.height / 2

        // X 軸動畫
        val animX = ObjectAnimator.ofFloat(coin, "x", targetX).apply {
            duration = 500 // 動畫時間：500ms
        }

        // Y 軸動畫
        val animY = ObjectAnimator.ofFloat(coin, "y", targetY).apply {
            duration = 500 // 動畫時間：500ms
        }

        // 動畫結束後移除金幣並增加分數
        animY.doOnEnd {
            gameArea.removeView(coin) // 移除金幣
            increaseScore() // 增加分數
        }

        // 同時啟動 X 和 Y 動畫
        animX.start()
        animY.start()
    }


    // 檢測金幣是否在吸引範圍內
    private fun isWithinMagnetRange(coin: View): Boolean {
        val dx = (coin.x + coin.width / 2) - (character.x + character.width / 2)
        val dy = (coin.y + coin.height / 2) - (character.y + character.height / 2)
        return dx * dx + dy * dy <= magnetRange * magnetRange // 圓形範圍檢測
    }
    private fun startMagnetDropCheck() {
        Thread {
            while (isRunning) {
                Thread.sleep(magnetCheckInterval) // 每 1 分鐘檢查一次
                runOnUiThread {
                    // 根據機率生成磁鐵
                    if (Random.nextFloat() <= magnetDropProbability) {
                        createMagnet()
                    }
                }
            }
        }.start()
    }

    private fun createGrowPopoItem() {
        createItem(
            R.drawable.bigpopo, // 替換為道具的圖片資源
            3000L, // 掉落時間
            onCollision = { growAllPopo() } // 碰撞後讓大便變大
        )
    }

    private fun growAllPopo() {
        // 更新全局縮放比例
        popoScaleFactor *= popoScaleIncrease

        // 放大所有大便
        for (i in 0 until gameArea.childCount) {
            val view = gameArea.getChildAt(i)
            if (view is ImageView && view.tag == "popo") { // 檢測是否是大便
                // 放大大便
                view.layoutParams.width = (view.layoutParams.width * popoScaleIncrease).toInt()
                view.layoutParams.height = (view.layoutParams.height * popoScaleIncrease).toInt()
                view.requestLayout() // 更新視圖大小
            }
        }

        // 在10秒後恢復大小
        Thread {
            Thread.sleep(popoGrowDuration) // 等待10秒
            runOnUiThread {
                restorePopoSize() // 恢復大便大小
            }
        }.start()
    }

    private fun restorePopoSize() {
        // 恢復到原始大小
        for (i in 0 until gameArea.childCount) {
            val view = gameArea.getChildAt(i)
            if (view is ImageView && view.tag == "popo") { // 檢測是否是大便
                view.layoutParams.width = (view.layoutParams.width / popoScaleIncrease).toInt()
                view.layoutParams.height = (view.layoutParams.height / popoScaleIncrease).toInt()
                view.requestLayout() // 更新視圖大小
            }
        }
    }

    private fun startGrowPopoDropCheck() {
        Thread {
            while (isRunning) {
                Thread.sleep(20000) // 每 20s 檢查一次
                runOnUiThread {
                    if (Random.nextFloat() <= 0.6) { // 60% 機率掉落
                        createGrowPopoItem()
                    }

                }
            }
        }.start()
    }

    private fun startFunModeItems() {
        // 每隔 5 秒檢查一次，隨機生成趣味道具
        Thread {
            while (isRunning) {
                Thread.sleep(2000) // 每 2 秒檢查一次
                runOnUiThread {
                    when (Random.nextInt(5)) {
                        0 -> funModeItems.createHappyCat()
                        1 -> funModeItems.createNuget()
                        2 -> funModeItems.createSadCat()
                        3 -> funModeItems.createJiafei()
                        4 -> funModeItems.createNeilong()
                    }
                }
            }
        }.start()
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
        val highestScore = if (mode == "CLASSIC") highestClassicScore else highestChallengeScore

        if (score > highestScore) {
            if (mode == "CLASSIC") highestClassicScore = score else highestChallengeScore = score
        }

        // 跳轉到結算頁面並傳遞相關數據
        val intent = Intent(this, EndGameActivity::class.java)
        intent.putExtra("FINAL_SCORE", score)
        intent.putExtra("HIGHEST_SCORE", highestScore)
        intent.putExtra("GAME_MODE", mode)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
    }
}
