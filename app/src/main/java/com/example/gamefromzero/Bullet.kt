package com.example.gamefromzero

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.ContextCompat
import android.graphics.drawable.VectorDrawable

class Bullet(context: Context, screenWidth: Int, screenHeight: Int) {
    
    private val bitmap: Bitmap
    val rect = RectF()
    
    private val bulletWidth = 20
    private val bulletHeight = 40
    private val bulletSpeed = 18f
    
    var isActive = false
    
    init {
        bitmap = createBitmapFromDrawable(context, R.drawable.bullet, bulletWidth, bulletHeight)
        rect.set(0f, 0f, 0f, 0f)
    }
    
    private fun createBitmapFromDrawable(context: Context, drawableRes: Int, width: Int, height: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, drawableRes)
        drawable?.setBounds(0, 0, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable?.draw(canvas)
        return bitmap
    }
    
    fun fire(startX: Float, startY: Float) {
        rect.set(
            startX - bulletWidth / 2f,
            startY,
            startX + bulletWidth / 2f,
            startY + bulletHeight
        )
        isActive = true
    }
    
    fun update() {
        if (isActive) {
            rect.top -= bulletSpeed
            rect.bottom -= bulletSpeed
        }
    }
    
    fun reset() {
        isActive = false
        rect.set(0f, 0f, 0f, 0f)
    }
    
    fun draw(canvas: Canvas, paint: Paint) {
        if (isActive) {
            canvas.drawBitmap(bitmap, rect.left, rect.top, paint)
        }
    }
    
    fun hasHitEnemy(enemyRect: RectF): Boolean {
        if (!isActive) return false
        return RectF.intersects(rect, enemyRect)
    }
    
    fun isOffScreen(): Boolean {
        return isActive && rect.bottom < 0
    }
}