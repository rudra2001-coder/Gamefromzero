package com.example.gamefromzero

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.VectorDrawable
import androidx.core.content.ContextCompat

class Player(context: Context, screenWidth: Int, screenHeight: Int) {
    
    private val bitmap: Bitmap
    val rect = RectF()
    
    private val playerWidth = 120
    private val playerHeight = 120
    private val playerSpeed = 20f
    
    private var velocityX = 0f
    var lives = 3
    var health = 100
    
    init {
        bitmap = createBitmapFromDrawable(context, R.drawable.player_ship, playerWidth, playerHeight)
        
        val startX = (screenWidth - playerWidth) / 2f
        val startY = screenHeight - playerHeight - 100f
        rect.set(startX, startY, startX + playerWidth, startY + playerHeight)
    }
    
    private fun createBitmapFromDrawable(context: Context, drawableRes: Int, width: Int, height: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, drawableRes)
        drawable?.setBounds(0, 0, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable?.draw(canvas)
        return bitmap
    }
    
    fun update(screenWidth: Int, screenHeight: Int) {
        rect.left += velocityX
        rect.right += velocityX
        
        if (rect.left < 0) {
            rect.left = 0f
            rect.right = playerWidth.toFloat()
        }
        if (rect.right > screenWidth) {
            rect.right = screenWidth.toFloat()
            rect.left = screenWidth - playerWidth.toFloat()
        }
    }
    
    fun setVelocity(tiltValue: Float) {
        velocityX = tiltValue * playerSpeed
    }
    
    fun draw(canvas: Canvas, paint: Paint) {
        canvas.drawBitmap(bitmap, rect.left, rect.top, paint)
    }
    
    fun reset(screenWidth: Int, screenHeight: Int) {
        val startX = (screenWidth - playerWidth) / 2f
        val startY = screenHeight - playerHeight - 100f
        rect.set(startX, startY, startX + playerWidth, startY + playerHeight)
    }
    
    fun hit() {
        health -= 34
        if (health <= 0) {
            lives--
            if (lives > 0) {
                health = 100
            }
        }
    }
    
    fun isDead(): Boolean = lives <= 0
}