package com.vegcale.taptaptap.feature.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun GameScreen(
    viewModel: GameViewModel = hiltViewModel()
) {
    val gameState by viewModel.gameState.collectAsState()

    when (gameState) {
        GameScreenState.Loading -> {
            // ローディング画面（必要であれば）
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Loading...")
            }
        }
        GameScreenState.Ready -> {
            StartScreen(onStartGame = { viewModel.startGame() })
        }
        is GameScreenState.Playing -> {
            val playingState = gameState as GameScreenState.Playing
            PlayingScreen(playingState, onTap = { offset, width, height ->
                viewModel.onTap(offset, width, height)
            })
        }
        is GameScreenState.GameOver -> {
            val gameOverState = gameState as GameScreenState.GameOver
            GameOverScreen(gameOverState.finalScore, onRestartGame = { viewModel.resetGame() })
        }
    }
}

@Composable
fun StartScreen(onStartGame: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "TapTapTap",
            fontSize = 48.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "ルール:\n画面に表示される赤い円をタップしてスコアを稼ぎましょう！\n円はランダムな位置に移動し、サイズも変化します。\n30秒間でどれだけスコアを稼げるか挑戦！",
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(64.dp))
        Button(
            onClick = onStartGame,
            shape = CircleShape
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Start Game", modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text(modifier = Modifier.padding(10.dp), text = "ゲームスタート", fontSize = 24.sp)
        }
    }
}

@Composable
fun PlayingScreen(playingState: GameScreenState.Playing, onTap: (Offset, Float, Float) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { // Unitキーで再起動を防止
                detectTapGestures { offset ->
                    onTap(offset, width, height)
                }
            }) {
            // Draw target
            drawCircle(
                color = Color(0xFFFF5722),
                radius = playingState.targetRadius,
                center = Offset(playingState.targetX * width, playingState.targetY * height)
            )
        }

        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Score: ${playingState.score}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50),
                )
                Text(
                    text = "Time: ${playingState.timeLeft.toInt()}s",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3),
                )
            }
        }
    }
}

@Composable
fun GameOverScreen(finalScore: Int, onRestartGame: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ゲームオーバー！",
            fontSize = 40.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFFD32F2F)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "最終スコア: $finalScore",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(64.dp))
        Button(
            onClick = onRestartGame,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Restart Game", modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text(modifier = Modifier.padding(10.dp), text = "もう一度プレイ", fontSize = 24.sp)
        }
    }
}