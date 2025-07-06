package com.vegcale.taptaptap.feature.game

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
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.random.Random

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
        val lastTapTime: Long
    ) : GameScreenState

    data class GameOver(val finalScore: Int) : GameScreenState
}

data class ChickenState(
    val x: Float,
    val y: Float,
    val size: Float,
    val isRaging: Boolean,
    val rageEndTime: Long,
    val isVisible: Boolean,
    val speedX: Float,
    val speedY: Float,
    val rotation: Float
)

@HiltViewModel
class GameViewModel @Inject constructor() : ViewModel() {

    private val _gameState = MutableStateFlow<GameScreenState>(GameScreenState.Ready)
    val gameState: StateFlow<GameScreenState> = _gameState.asStateFlow()

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
            rotation = 0f
        )
    )
    val chickenState: StateFlow<ChickenState> = _chickenState.asStateFlow()

    private var gameJob: kotlinx.coroutines.Job? = null
    private var chickenJob: kotlinx.coroutines.Job? = null

    private val COMBO_TIMEOUT_MS = 1500L // 1.5 seconds

    fun startGame() {
        _gameState.value = GameScreenState.Playing(
            score = 0,
            timeLeft = 30f,
            targetX = 0.5f,
            targetY = 0.5f,
            targetRadius = 50f,
            comboCount = 0,
            lastTapTime = System.currentTimeMillis()
        )
        gameJob?.cancel()
        chickenJob?.cancel()

        gameJob = viewModelScope.launch {
            var timeLeft = 30f
            while (timeLeft > 0.1f) {
                delay(100) // Update every 0.1 seconds
                timeLeft -= 0.1f
                (_gameState.value as? GameScreenState.Playing)?.let { currentState ->
                    _gameState.value = currentState.copy(timeLeft = timeLeft)
                }
            }
            _gameState.value = GameScreenState.GameOver((_gameState.value as GameScreenState.Playing).score)
        }
        updateTargetPosition() // Set initial target position
        startChickenMovement()
    }

    private fun startChickenMovement() {
        chickenJob = viewModelScope.launch {
            while (true) {
                delay(16) // Update chicken position every 16ms (around 60fps)
                _chickenState.value = _chickenState.value.let { currentChicken ->
                    val currentSpeedX = if (currentChicken.isRaging) currentChicken.speedX * 2 else currentChicken.speedX
                    val currentSpeedY = if (currentChicken.isRaging) currentChicken.speedY * 2 else currentChicken.speedY

                    var newX = currentChicken.x + currentSpeedX
                    var newY = currentChicken.y + currentSpeedY
                    var newSpeedX = currentChicken.speedX
                    var newSpeedY = currentChicken.speedY

                    if (newX < 0f || newX > 1f) {
                        newSpeedX = -newSpeedX * (Random.nextFloat() * 0.5f + 0.75f) // Add some randomness to the bounce
                    }
                    if (newY < 0f || newY > 1f) {
                        newSpeedY = -newSpeedY * (Random.nextFloat() * 0.5f + 0.75f) // Add some randomness to the bounce
                    }

                    val rotation = atan2(newSpeedY, newSpeedX) * (180f / Math.PI.toFloat())

                    val nextChickenState = currentChicken.copy(
                        x = newX.coerceIn(0f, 1f),
                        y = newY.coerceIn(0f, 1f),
                        speedX = newSpeedX,
                        speedY = newSpeedY,
                        rotation = rotation
                    )

                    if (currentChicken.isRaging && System.currentTimeMillis() > currentChicken.rageEndTime) {
                        nextChickenState.copy(isRaging = false)
                    } else {
                        nextChickenState
                    }
                }
            }
        }
    }

    private fun updateTargetPosition() {
        (_gameState.value as? GameScreenState.Playing)?.let { currentState ->
            _gameState.value = currentState.copy(
                targetX = Random.nextFloat(),
                targetY = Random.nextFloat(),
                targetRadius = 30 + Random.nextFloat() * (70 - 30)
            )
        }
    }

    fun onTap(offset: Offset, screenWidth: Float, screenHeight: Float) {
        if (_gameState.value !is GameScreenState.Playing) return

        val currentState = _gameState.value as GameScreenState.Playing

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
            val scoreIncrease = 1 + (newComboCount / 5) // Example: +1 score for every 5 combo
            _gameState.value = currentState.copy(
                score = currentState.score + scoreIncrease,
                comboCount = newComboCount,
                lastTapTime = currentTime
            )
            updateTargetPosition() // Update target immediately after a successful tap
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
                    rageEndTime = System.currentTimeMillis() + 5000L // 5 seconds rage
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
        _gameState.value = GameScreenState.Ready
    }
}
