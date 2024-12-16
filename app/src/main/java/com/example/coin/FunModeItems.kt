package com.example.coin

import android.media.MediaPlayer
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.animation.doOnEnd

class FunModeItems(
    private val gameArea: FrameLayout,
    private val character: ImageView
) {

    // 播放音效，並確保不會停止其他音效
    private fun playSound(soundResId: Int) {
        // 創建一個新的 MediaPlayer 物件，播放當前音效
        val mediaPlayer = MediaPlayer.create(gameArea.context, soundResId)

        // 當音效播放完畢後釋放資源
        mediaPlayer.setOnCompletionListener {
            it.release()
        }

        // 開始播放音效
        mediaPlayer.start()
    }

    // happycat 道具
    fun createHappyCat() {
        createItem(
            R.drawable.happycat, // 替換為 happycat 圖片資源
            2900L, // 道具掉落時間
            onCollision = { playSound(R.raw.happycat) } // 播放 happycat 音效
        )
    }

    // nuget 道具
    fun createNuget() {
        createItem(
            R.drawable.nuget, // 替換為 nuget 圖片資源
            2900L, // 道具掉落時間
            onCollision = { playSound(R.raw.gedagedgadao) } // 播放 nuget 音效
        )
    }

    // sadcat 道具
    fun createSadCat() {
        createItem(
            R.drawable.sadcat, // 替換為 sadcat 圖片資源
            2900L, // 道具掉落時間
            onCollision = { playSound(R.raw.mewomewo) } // 播放 sadcat 音效
        )
    }

    fun createJiafei() {
        createItem(
            R.drawable.nail, // 替換為 nail 圖片資源
            2900L, // 道具掉落時間
            onCollision = { playSound(R.raw.jiafei) } // 播放 nail 音效
        )
    }

    fun createNeilong() {
        createItem(
            R.drawable.neilong, // 替換為 nail 圖片資源
            2900L, // 道具掉落時間
            onCollision = { playSound(R.raw.neilong) } // 播放 nail 音效
        )
    }


    // 通用的道具生成邏輯
    private fun createItem(
        drawableResId: Int,
        duration: Long,
        onCollision: () -> Unit
    ) {
        val item = ImageView(gameArea.context).apply {
            setImageResource(drawableResId)
            layoutParams = FrameLayout.LayoutParams(100, 100)
        }

        gameArea.addView(item)

        val startX = (0 until gameArea.width - item.layoutParams.width).random()
        item.x = startX.toFloat()
        item.y = 0f

        val animator = android.animation.ObjectAnimator.ofFloat(item, "translationY", gameArea.height.toFloat())
        animator.duration = duration
        animator.start()

        var isRemoved = false
        animator.addUpdateListener {
            if (!isRemoved && checkCollision(item, character)) {
                isRemoved = true
                gameArea.removeView(item)
                animator.cancel()
                onCollision() // 播放音效
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
}
