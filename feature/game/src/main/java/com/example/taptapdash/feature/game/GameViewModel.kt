package com.example.taptapdash.feature.game

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

sealed interface GameScreenState {
    object Loading : GameScreenState
    object Ready : GameScreenState
    data class Playing(
        val score: Int,
        val timeLeft: Float,
        val targetX: Float,
        val targetY: Float,
        val targetRadius: Float
    ) : GameScreenState

    data class GameOver(val finalScore: Int) : GameScreenState
}

@HiltViewModel
class GameViewModel @Inject constructor() : ViewModel() {

    private val _gameState = MutableStateFlow<GameScreenState>(GameScreenState.Ready)
    val gameState: StateFlow<GameScreenState> = _gameState.asStateFlow()

    private var gameJob: kotlinx.coroutines.Job? = null

    fun startGame() {
        _gameState.value = GameScreenState.Playing(score = 0, timeLeft = 30f, targetX = 0.5f, targetY = 0.5f, targetRadius = 50f)
        gameJob?.cancel()

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

        if (distance <= currentState.targetRadius) {
            _gameState.value = currentState.copy(score = currentState.score + 1)
            updateTargetPosition() // Update target immediately after a successful tap
        }
    }

    fun resetGame() {
        _gameState.value = GameScreenState.Ready
    }
}