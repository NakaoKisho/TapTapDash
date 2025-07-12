package com.vegcale.taptaptap.feature.game

import android.content.SharedPreferences
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.pow
import kotlin.random.Random
import kotlin.math.atan2

sealed interface GameScreenState {
    object Loading : GameScreenState
    object Ready : GameScreenState
    data class Playing(
        val score: Int,
        val timeLeft: Float,
        val targetX: Float,
        val targetY: Float,
        val targetRadius: Float,
        val comboCount: Int,
        val lastTapTime: Long,
        val isScoreMultiplierActive: Boolean,
        val isTimeFreezeActive: Boolean,
        val isChickenStopActive: Boolean,
        val scoreMultiplierEndTime: Long,
        val timeFreezeEndTime: Long,
        val chickenStopEndTime: Long
    ) : GameScreenState

    data class GameOver(val finalScore: Int, val highScore: Int) : GameScreenState
}

sealed class PowerUpType {
    object TimeFreeze : PowerUpType()
    object ScoreMultiplier : PowerUpType()
    object ChickenStop : PowerUpType()
    object TargetRefresh : PowerUpType()

    companion object {
        fun values(): Array<PowerUpType> {
            return arrayOf(TimeFreeze, ScoreMultiplier, ChickenStop, TargetRefresh)
        }
    }
}

data class PowerUp(
    val id: Long,
    val x: Float,
    val y: Float,
    val radius: Float,
    val type: PowerUpType,
    val spawnTime: Long
)

data class ChickenState(
    val x: Float,
    val y: Float,
    val size: Float,
    val isRaging: Boolean,
    val rageEndTime: Long,
    val isVisible: Boolean,
    val speedX: Float,
    val speedY: Float,
    val rotation: Float,
    val skinId: String
)

@HiltViewModel
class GameViewModel @Inject constructor(
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    private val HIGH_SCORE_KEY = "high_score"
    private val COIN_KEY = "coin"

    private val _gameState = MutableStateFlow<GameScreenState>(GameScreenState.Ready)
    val gameState: StateFlow<GameScreenState> = _gameState.asStateFlow()

    private val SELECTED_SKIN_KEY = "selected_skin"

    private val _chickenState = MutableStateFlow(
        ChickenState(
            x = 0.5f,
            y = 0.5f,
            size = 60f,
            isRaging = false,
            rageEndTime = 0L,
            isVisible = true,
            speedX = 0.005f,
            speedY = 0.005f,
            rotation = 0f,
            skinId = sharedPreferences.getString(SELECTED_SKIN_KEY, "default") ?: "default"
        )
    )
    val chickenState: StateFlow<ChickenState> = _chickenState.asStateFlow()

    private val _powerUps = MutableStateFlow<List<PowerUp>>(emptyList())
    val powerUps: StateFlow<List<PowerUp>> = _powerUps.asStateFlow()

    private var gameJob: kotlinx.coroutines.Job? = null
    private var chickenJob: kotlinx.coroutines.Job? = null
    private var powerUpJob: kotlinx.coroutines.Job? = null

    private val COMBO_TIMEOUT_MS = 1500L // 1.5 seconds
    private val POWER_UP_DURATION_MS = 5000L // 5 seconds
    private val POWER_UP_SPAWN_INTERVAL_MS = 7000L // 7 seconds

    fun startGame() {
        val initialHighScore = sharedPreferences.getInt(HIGH_SCORE_KEY, 0)

        _gameState.value = GameScreenState.Playing(
            score = 0,
            timeLeft = 30f,
            targetX = Random.nextFloat(),
            targetY = Random.nextFloat(),
            targetRadius = 30 + Random.nextFloat() * (70 - 30),
            comboCount = 0,
            lastTapTime = System.currentTimeMillis(),
            isScoreMultiplierActive = false,
            isTimeFreezeActive = false,
            isChickenStopActive = false,
            scoreMultiplierEndTime = 0L,
            timeFreezeEndTime = 0L,
            chickenStopEndTime = 0L
        )
        _powerUps.value = emptyList() // Clear power-ups on new game
        gameJob?.cancel()
        chickenJob?.cancel()
        powerUpJob?.cancel()

        gameJob = viewModelScope.launch {
            var timeLeft = (_gameState.value as GameScreenState.Playing).timeLeft
            while (timeLeft > 0.1f) {
                delay(100) // Update every 0.1 seconds
                val currentState = _gameState.value
                if (currentState is GameScreenState.Playing) {
                    if (!currentState.isTimeFreezeActive) {
                        timeLeft -= 0.1f
                    }
                    _gameState.value = currentState.copy(timeLeft = timeLeft)
                } else {
                    break // Game is no longer playing
                }
            }
            val finalScore = (_gameState.value as? GameScreenState.Playing)?.score ?: 0
            val currentHighScore = sharedPreferences.getInt(HIGH_SCORE_KEY, 0)
            if (finalScore > currentHighScore) {
                sharedPreferences.edit().putInt(HIGH_SCORE_KEY, finalScore).apply()
            }
            val coins = sharedPreferences.getInt(COIN_KEY, 0)
            sharedPreferences.edit().putInt(COIN_KEY, coins + finalScore / 10).apply()
            _gameState.value = GameScreenState.GameOver(finalScore, sharedPreferences.getInt(HIGH_SCORE_KEY, 0))
        }
        startChickenMovement()
        startPowerUpGenerationAndManagement()
    }

    private fun startChickenMovement() {
        chickenJob = viewModelScope.launch {
            while (true) {
                delay(16) // Update chicken position every 16ms (around 60fps)
                val currentState = _gameState.value
                if (currentState is GameScreenState.Playing && currentState.isChickenStopActive) {
                    continue
                }

                _chickenState.value = _chickenState.value.let { currentChicken ->
                    val speedMultiplier = if (currentChicken.isRaging) 1.5f else 1f
                    var newX = currentChicken.x + currentChicken.speedX * speedMultiplier
                    var newY = currentChicken.y + currentChicken.speedY * speedMultiplier
                    var newSpeedX = currentChicken.speedX
                    var newSpeedY = currentChicken.speedY

                    if (newX < 0f || newX > 1f) {
                        newSpeedX = -newSpeedX * (Random.nextFloat() * 0.5f + 0.75f) // Add some randomness to the bounce
                    }
                    if (newY < 0f || newY > 1f) {
                        newSpeedY = -newSpeedY * (Random.nextFloat() * 0.5f + 0.75f) // Add some randomness to the bounce
                    }

                    val rotation = kotlin.math.atan2(newSpeedY, newSpeedX) * (180f / Math.PI.toFloat())

                    currentChicken.copy(
                        x = newX.coerceIn(0f, 1f),
                        y = newY.coerceIn(0f, 1f),
                        speedX = newSpeedX,
                        speedY = newSpeedY,
                        rotation = rotation
                    )
                }
            }
        }
    }

    private fun startPowerUpGenerationAndManagement() {
        powerUpJob = viewModelScope.launch {
            val powerUpTypes = PowerUpType.values().toList()
            var nextPowerUpIndex = 0

            while (true) {
                delay(POWER_UP_SPAWN_INTERVAL_MS) // Spawn power-up periodically
                if (_powerUps.value.isEmpty()) { // Only one power-up at a time
                    if (nextPowerUpIndex >= powerUpTypes.size) {
                        nextPowerUpIndex = 0
                        powerUpTypes.shuffled()
                    }
                    val newPowerUpType = powerUpTypes[nextPowerUpIndex]
                    nextPowerUpIndex++

                    _powerUps.value = listOf(PowerUp(
                        id = System.nanoTime(),
                        x = Random.nextFloat(),
                        y = Random.nextFloat(),
                        radius = 40f, // Power-up size
                        type = newPowerUpType,
                        spawnTime = System.currentTimeMillis()
                    ))
                }

                // Deactivate power-up effects if expired
                (_gameState.value as? GameScreenState.Playing)?.let { currentState ->
                    val currentTime = System.currentTimeMillis()
                    var updatedState = currentState

                    if (currentState.isScoreMultiplierActive && currentTime > currentState.scoreMultiplierEndTime) {
                        updatedState = updatedState.copy(isScoreMultiplierActive = false)
                    }
                    if (currentState.isTimeFreezeActive && currentTime > currentState.timeFreezeEndTime) {
                        updatedState = updatedState.copy(isTimeFreezeActive = false)
                    }
                    if (currentState.isChickenStopActive && currentTime > currentState.chickenStopEndTime) {
                        updatedState = updatedState.copy(isChickenStopActive = false)
                    }
                    _gameState.value = updatedState
                }

                // Remove expired power-ups from screen (if any)
                _powerUps.value = _powerUps.value.filter { powerUp ->
                    System.currentTimeMillis() - powerUp.spawnTime <= POWER_UP_DURATION_MS
                }
            }
        }
    }

    fun onTap(offset: Offset, screenWidth: Float, screenHeight: Float) {
        if (_gameState.value !is GameScreenState.Playing) return

        val currentState = _gameState.value as GameScreenState.Playing

        // Check for power-up tap
        val tappedPowerUp = _powerUps.value.firstOrNull { powerUp ->
            val powerUpCenterX = powerUp.x * screenWidth
            val powerUpCenterY = powerUp.y * screenHeight
            val distance = kotlin.math.sqrt(
                (offset.x - powerUpCenterX).toDouble().pow(2) +
                        (offset.y - powerUpCenterY).toDouble().pow(2)
            )
            distance <= powerUp.radius + 15f // Power-up hit area
        }

        if (tappedPowerUp != null) {
            _powerUps.value = emptyList() // Remove power-up after tap

            when (tappedPowerUp.type) {
                PowerUpType.TimeFreeze -> {
                    _gameState.value = currentState.copy(
                        isTimeFreezeActive = true,
                        timeFreezeEndTime = System.currentTimeMillis() + 5000L // 5 seconds
                    )
                }
                PowerUpType.ScoreMultiplier -> {
                    _gameState.value = currentState.copy(
                        isScoreMultiplierActive = true,
                        scoreMultiplierEndTime = System.currentTimeMillis() + 5000L // 5 seconds
                    )
                }
                PowerUpType.ChickenStop -> {
                    _gameState.value = currentState.copy(
                        isChickenStopActive = true,
                        chickenStopEndTime = System.currentTimeMillis() + 3000L // 3 seconds
                    )
                }
                PowerUpType.TargetRefresh -> {
                    _gameState.value = currentState.copy(
                        timeLeft = currentState.timeLeft + 3f
                    )
                }
            }

            return // Power-up tapped, don't check for target tap
        }

        // Original target tap logic
        val targetCenterX = currentState.targetX * screenWidth
        val targetCenterY = currentState.targetY * screenHeight
        val distance = kotlin.math.sqrt(
            (offset.x - targetCenterX).toDouble().pow(2) +
                    (offset.y - targetCenterY).toDouble().pow(2)
        )

        val HIT_AREA_BUFFER_PX = 15f // 赤い点の当たり判定を広げるためのバッファ値 (ピクセル)

        if (distance <= currentState.targetRadius + HIT_AREA_BUFFER_PX) {
            val currentTime = System.currentTimeMillis()
            val newComboCount = if (currentTime - currentState.lastTapTime <= COMBO_TIMEOUT_MS) {
                currentState.comboCount + 1
            } else {
                1
            }
            val scoreMultiplier = if (currentState.isScoreMultiplierActive) 2 else 1
            val scoreIncrease = (1 + (newComboCount / 5)) * scoreMultiplier
            _gameState.value = currentState.copy(
                score = currentState.score + scoreIncrease,
                targetX = Random.nextFloat(),
                targetY = Random.nextFloat(),
                targetRadius = 30 + Random.nextFloat() * (70 - 30),
                comboCount = newComboCount,
                lastTapTime = currentTime
            )
        } else {
            // Missed tap, reset combo
            _gameState.value = currentState.copy(comboCount = 0)
        }
    }

    fun onChickenTap(offset: Offset, screenWidth: Float, screenHeight: Float, density: Float): Boolean {
        val currentChicken = _chickenState.value
        if (!currentChicken.isVisible) return false

        val chickenCenterX = currentChicken.x * screenWidth
        val chickenCenterY = currentChicken.y * screenHeight
        val chickenSizeInPixels = currentChicken.size * density

        val distance = kotlin.math.sqrt(
            (offset.x - chickenCenterX).toDouble().pow(2) +
                    (offset.y - chickenCenterY).toDouble().pow(2)
        )

        if (distance <= chickenSizeInPixels / 2) { // Assuming size is diameter
            if (!currentChicken.isRaging) {
                _chickenState.value = currentChicken.copy(
                    isRaging = true,
                    rageEndTime = System.currentTimeMillis() + 5000L, // 5 seconds rage
                    speedX = currentChicken.speedX * 1.5f,
                    speedY = currentChicken.speedY * 1.5f
                )
            }
            // Tapping chicken resets combo
            (_gameState.value as? GameScreenState.Playing)?.let { currentState ->
                _gameState.value = currentState.copy(comboCount = 0)
            }
            return true
        }
        return false
    }

    fun resetGame() {
        gameJob?.cancel()
        chickenJob?.cancel()
        powerUpJob?.cancel()
        _powerUps.value = emptyList()
        _gameState.value = GameScreenState.Ready
    }
}