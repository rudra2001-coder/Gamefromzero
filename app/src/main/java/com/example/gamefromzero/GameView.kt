package com.example.gamefromzero

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
        initSoundPool()
    }
    
    override fun surfaceCreated(holder: SurfaceHolder) {
        screenWidth = width
        screenHeight = height
        
        if (screenWidth == 0 || screenHeight == 0) {
            screenWidth = 1080
            screenHeight = 1920
        }
        
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
        
        player.setVelocity(sensorController.getTiltX())
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