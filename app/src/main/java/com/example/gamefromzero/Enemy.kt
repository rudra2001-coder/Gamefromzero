package com.example.gamefromzero

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.ContextCompat
import android.graphics.drawable.VectorDrawable

class Enemy(context: Context, screenWidth: Int, screenHeight: Int) {
    
    private val bitmap: Bitmap
    val rect = RectF()
    
    private val enemyWidth = 100
    private val enemyHeight = 100
    
    private var baseSpeed = 3f
    var currentSpeed = 3f
    
    init {
        bitmap = createBitmapFromDrawable(context, R.drawable.enemy_ship, enemyWidth, enemyHeight)
        reset(screenWidth, screenHeight)
    }
    
    private fun createBitmapFromDrawable(context: Context, drawableRes: Int, width: Int, height: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, drawableRes)
        drawable?.setBounds(0, 0, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable?.draw(canvas)
        return bitmap
    }
    
    fun reset(screenWidth: Int, screenHeight: Int) {
        val randomX = (Math.random() * (screenWidth - enemyWidth)).toFloat()
        rect.set(randomX, -enemyHeight.toFloat(), randomX + enemyWidth, 0f)
        currentSpeed = baseSpeed
    }
    
    fun moveDown() {
        rect.top += currentSpeed
        rect.bottom += currentSpeed
    }
    
    fun update(speed: Float) {
        rect.top += speed
        rect.bottom += speed
    }
    
    fun increaseDifficulty() {
        currentSpeed += 0.3f
    }
    
    fun draw(canvas: Canvas, paint: Paint) {
        canvas.drawBitmap(bitmap, rect.left, rect.top, paint)
    }
    
    fun hasReachedPlayer(playerRect: RectF): Boolean {
        return RectF.intersects(rect, playerRect)
    }
    
    fun isOffScreen(screenHeight: Int): Boolean {
        return rect.top > screenHeight
    }
}