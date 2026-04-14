package com.example.gamefromzero

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

class GameView(private val context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {
    
    private var gameThread: Thread? = null
    private var playing = false
    
    private lateinit var player: Player
    private lateinit var bullet: Bullet
    private val enemies = mutableListOf<Enemy>()
    private lateinit var sensorController: SensorController
    
    private var score = 0
    private var gameOver = false
    private var gameStarted = false
    
    private var lastShotTime = 0L
    private var lastEnemySpawnTime = 0L
    
    private val shootInterval = 400L
    private val enemySpawnInterval = 1500L
    
    private val paint = Paint()
    
    private var screenWidth = 1080
    private var screenHeight = 1920
    private var isInitialized = false
    
    private lateinit var soundPool: SoundPool
    private var shootSoundId = 0
    private var explosionSoundId = 0
    private var isSoundLoaded = false
    
    private var moveLeft = false
    private var moveRight = false
    
    private val leftArrowRect = RectF()
    private val rightArrowRect = RectF()
    private val arrowSize = 150f
    private val arrowMargin = 50f
    
    private fun initSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        
        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()
        
        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) isSoundLoaded = true
        }
    }
    
    init {
        holder.addCallback(this)
        isFocusable = true
        isFocusableInTouchMode = true
        initSoundPool()
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        leftArrowRect.set(arrowMargin, h / 2f - arrowSize / 2, arrowMargin + arrowSize, h / 2f + arrowSize / 2)
        rightArrowRect.set(w - arrowMargin - arrowSize, h / 2f - arrowSize / 2, w - arrowMargin, h / 2f + arrowSize / 2)
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (leftArrowRect.contains(event.x, event.y)) {
                    moveLeft = true
                    return true
                }
                if (rightArrowRect.contains(event.x, event.y)) {
                    moveRight = true
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                moveLeft = false
                moveRight = false
            }
        }
        return super.onTouchEvent(event)
    }
    
    private fun handleTouchInput() {
        if (moveLeft && !moveRight) {
            player.setVelocity(-1f)
        } else if (moveRight && !moveLeft) {
            player.setVelocity(1f)
        } else if (!sensorController.hasAccelerometer()) {
            player.setVelocity(0f)
        }
    }
    
    override fun surfaceCreated(holder: SurfaceHolder) {
        screenWidth = width
        screenHeight = height
        
        if (screenWidth == 0 || screenHeight == 0) {
            screenWidth = 1080
            screenHeight = 1920
        }
        
        leftArrowRect.set(arrowMargin, screenHeight / 2f - arrowSize / 2, arrowMargin + arrowSize, screenHeight / 2f + arrowSize / 2)
        rightArrowRect.set(screenWidth - arrowMargin - arrowSize, screenHeight / 2f - arrowSize / 2, screenWidth - arrowMargin, screenHeight / 2f + arrowSize / 2)
        
        initGame()
        isInitialized = true
        startGame()
    }
    
    private fun initGame() {
        player = Player(context, screenWidth, screenHeight)
        bullet = Bullet(context, screenWidth, screenHeight)
        enemies.clear()
        
        for (i in 0 until 3) {
            val enemy = Enemy(context, screenWidth, screenHeight)
            enemy.rect.offset(0f, -i * 200f - 200f)
            enemies.add(enemy)
        }
        
        sensorController = SensorController(context)
        sensorController.register()
        
        lastShotTime = System.currentTimeMillis()
        lastEnemySpawnTime = System.currentTimeMillis()
        
        score = 0
        gameOver = false
        gameStarted = true
    }
    
    private fun startGame() {
        playing = true
        gameThread = Thread(this)
        gameThread?.start()
    }
    
    override fun run() {
        val targetFPS = 60
        val targetFrameTime = 1000L / targetFPS
        
        while (playing) {
            if (!isInitialized || !gameStarted) {
                try { Thread.sleep(100) } catch (e: Exception) {}
                continue
            }
            
            val startTime = System.currentTimeMillis()
            
            if (!gameOver) {
                update()
            }
            draw()
            
            val endTime = System.currentTimeMillis()
            val frameTime = endTime - startTime
            
            if (frameTime < targetFrameTime) {
                try {
                    Thread.sleep(targetFrameTime - frameTime)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    private fun update() {
        val currentTime = System.currentTimeMillis()
        
        handleTouchInput()
        player.update(screenWidth, screenHeight)
        
        if (currentTime - lastShotTime >= shootInterval) {
            if (!bullet.isActive) {
                val bulletX = player.rect.centerX()
                val bulletY = player.rect.top
                bullet.fire(bulletX, bulletY)
                lastShotTime = currentTime
                playShootSound()
            }
        }
        
        bullet.update()
        
        if (bullet.isOffScreen()) {
            bullet.reset()
        }
        
        for (enemy in enemies) {
            enemy.update(enemy.currentSpeed)
            
            if (bullet.hasHitEnemy(enemy.rect)) {
                score += 10
                bullet.reset()
                enemy.reset(screenWidth, screenHeight)
                enemy.increaseDifficulty()
                playExplosionSound()
                vibrateOnHit()
            }
            
            if (enemy.hasReachedPlayer(player.rect)) {
                player.hit()
                enemy.reset(screenWidth, screenHeight)
                vibrateOnHit()
                
                if (player.isDead()) {
                    gameOver = true
                }
            }
            
            if (enemy.isOffScreen(screenHeight)) {
                enemy.reset(screenWidth, screenHeight)
            }
        }
        
        if (currentTime - lastEnemySpawnTime >= enemySpawnInterval) {
            spawnEnemy()
            lastEnemySpawnTime = currentTime
        }
    }
    
    private fun spawnEnemy() {
        if (enemies.size < 8) {
            val enemy = Enemy(context, screenWidth, screenHeight)
            enemies.add(enemy)
        }
    }
    
    private fun playShootSound() {
        if (isSoundLoaded) {
            soundPool.play(shootSoundId, 0.3f, 0.3f, 1, 0, 1f)
        }
    }
    
    private fun playExplosionSound() {
        if (isSoundLoaded) {
            soundPool.play(explosionSoundId, 0.5f, 0.5f, 1, 0, 1f)
        }
    }
    
    private fun vibrateOnHit() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
    
    private fun draw() {
        if (!holder.surface.isValid) return
        
        var canvas: Canvas? = null
        try {
            canvas = holder.lockCanvas()
            if (canvas == null) return
            
            canvas.drawColor(Color.BLACK)
            
            for (enemy in enemies) {
                enemy.draw(canvas, paint)
            }
            
            bullet.draw(canvas, paint)
            player.draw(canvas, paint)
            
            drawArrows(canvas)
            
            val scorePaint = Paint().apply {
                color = Color.GREEN
                textSize = 50f
            }
            canvas.drawText("Score: $score", 30f, 80f, scorePaint)
            
            val livesPaint = Paint().apply {
                color = Color.RED
                textSize = 50f
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("Lives: ${player.lives}", screenWidth - 30f, 80f, livesPaint)
            
            val healthPaint = Paint().apply {
                color = Color.YELLOW
                textSize = 40f
            }
            canvas.drawText("Health: ${player.health}%", 30f, 140f, healthPaint)
            
            if (gameOver) {
                val gameOverPaint = Paint().apply {
                    color = Color.RED
                    textSize = 100f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("GAME OVER", screenWidth / 2f, screenHeight / 2f - 50f, gameOverPaint)
                
                val finalScorePaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 60f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("Final Score: $score", screenWidth / 2f, screenHeight / 2f + 50f, finalScorePaint)
                
                val restartPaint = Paint().apply {
                    color = Color.CYAN
                    textSize = 40f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("Press BACK to restart", screenWidth / 2f, screenHeight / 2f + 150f, restartPaint)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            canvas?.let {
                try {
                    holder.unlockCanvasAndPost(it)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    private fun drawArrows(canvas: Canvas) {
        val arrowPaint = Paint().apply {
            color = Color.argb(128, 100, 100, 100)
            style = Paint.Style.FILL
        }
        
        val strokePaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
        
        canvas.drawRoundRect(leftArrowRect, 20f, 20f, arrowPaint)
        canvas.drawRoundRect(leftArrowRect, 20f, 20f, strokePaint)
        canvas.drawText("<", leftArrowRect.centerX() - 15f, leftArrowRect.centerY() + 15f, Paint().apply {
            color = Color.WHITE
            textSize = 60f
            textAlign = Paint.Align.CENTER
        })
        
        canvas.drawRoundRect(rightArrowRect, 20f, 20f, arrowPaint)
        canvas.drawRoundRect(rightArrowRect, 20f, 20f, strokePaint)
        canvas.drawText(">", rightArrowRect.centerX() - 15f, rightArrowRect.centerY() + 15f, Paint().apply {
            color = Color.WHITE
            textSize = 60f
            textAlign = Paint.Align.CENTER
        })
    }
    
    fun restartGame() {
        if (this::player.isInitialized) {
            player.lives = 3
            player.health = 100
        }
        
        if (this::bullet.isInitialized) {
            bullet.reset()
        }
        
        enemies.clear()
        
        score = 0
        gameOver = false
        
        initGame()
    }
    
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        playing = false
        if (::sensorController.isInitialized) {
            sensorController.unregister()
        }
        try {
            gameThread?.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
}