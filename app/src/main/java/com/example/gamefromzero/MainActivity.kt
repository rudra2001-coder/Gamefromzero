package com.example.gamefromzero

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    private var gameView: GameView? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        
        setContentView(R.layout.activity_main)
        
        val playButton = findViewById<Button>(R.id.playButton)
        playButton.setOnClickListener {
            playGame()
        }
    }
    
    private fun playGame() {
        val mainLayout = findViewById<View>(R.id.main)
        mainLayout.visibility = View.GONE
        
        gameView = GameView(this)
        setContentView(gameView)
    }
    
    private fun showMenu() {
        gameView = null
        setContentView(R.layout.activity_main)
        
        val playButton = findViewById<Button>(R.id.playButton)
        playButton.setOnClickListener {
            playGame()
        }
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (gameView != null) {
            gameView?.restartGame()
            setContentView(R.layout.activity_main)
            val playButton = findViewById<Button>(R.id.playButton)
            playButton.setOnClickListener { playGame() }
        } else {
            super.onBackPressed()
        }
    }
}
