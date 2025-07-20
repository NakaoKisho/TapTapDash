package com.vegcale.taptaptap.feature.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

@Composable
fun GameScreen(
    onNavigateToShop: () -> Unit,
    viewModel: GameViewModel = hiltViewModel()
) {
    val gameState by viewModel.gameState.collectAsState()
    val chickenState by viewModel.chickenState.collectAsState()
    val powerUps by viewModel.powerUps.collectAsState()

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
            StartScreen(
                setSkinId = { viewModel.setSkinId() },
                onStartGame = { viewModel.startGame() },
                onNavigateToShop = onNavigateToShop,
            )
        }
        is GameScreenState.Playing -> {
            val playingState = gameState as GameScreenState.Playing
            PlayingScreen(
                playingState,
                chickenState,
                powerUps,
                onTap = { offset, screenWidth, screenHeight ->
                    viewModel.onTap(offset, screenWidth, screenHeight)
                },
                onChickenTap = { offset, screenWidth, screenHeight, density ->
                    viewModel.onChickenTap(offset, screenWidth, screenHeight, density)
                }
            )
        }
        is GameScreenState.GameOver -> {
            val gameOverState = gameState as GameScreenState.GameOver
            GameOverScreen(gameOverState.finalScore, gameOverState.highScore, onRestartGame = { viewModel.resetGame() }, onNavigateToShop = onNavigateToShop, viewModel = viewModel)
        }
    }
}

@Composable
fun StartScreen(
    setSkinId: () -> Unit,
    onStartGame: () -> Unit,
    onNavigateToShop: () -> Unit,
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        setSkinId()
    }

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
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onNavigateToShop,
            shape = CircleShape
        ) {
            Icon(Icons.Default.ShoppingBag, contentDescription = "Go to Shop", modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text(modifier = Modifier.padding(10.dp), text = "スキンを変更する", fontSize = 24.sp)
        }
    }
}

@Composable
fun PlayingScreen(
    playingState: GameScreenState.Playing,
    chickenState: ChickenState,
    powerUps: List<PowerUp>,
    onTap: (Offset, Float, Float) -> Unit,
    onChickenTap: (Offset, Float, Float, Float) -> Boolean
) {
    val density = LocalDensity.current.density
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures {
                        if (!onChickenTap(it, width, height, density)) {
                            onTap(it, width, height)
                        }
                    }
                }
        ) {
            // Draw target
            val targetColor = Color(0xFFFF5722)
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = targetColor,
                    radius = playingState.targetRadius,
                    center = Offset(playingState.targetX * width, playingState.targetY * height)
                )
            }

            // Draw chicken
            if (chickenState.isVisible) {
                val chickenDrawable = when (chickenState.skinId) {
                    "turtle" -> if (chickenState.isRaging) R.mipmap.angry_turtle else R.mipmap.turtle
                    "ninja" -> if (chickenState.isRaging) R.mipmap.angry_ninja else R.mipmap.ninja
                    else -> if (chickenState.isRaging) R.mipmap.angry_chicken else R.mipmap.chicken
                }
                Image(
                    painter = painterResource(id = chickenDrawable),
                    contentDescription = "Chicken",
                    modifier = Modifier
                        .size(chickenState.size.dp)
                        .offset { 
                            IntOffset(
                                (chickenState.x * width - (chickenState.size * density) / 2f).toInt(),
                                (chickenState.y * height - (chickenState.size * density) / 2f).toInt()
                            )
                        }
//                        .rotate(chickenState.rotation)
                )
            }

            // Draw power-ups
            powerUps.forEach { powerUp ->
                val powerUpColor = when (powerUp.type) {
                    PowerUpType.TimeFreeze -> Color(0xFF9C27B0) // Purple
                    PowerUpType.ScoreMultiplier -> Color(0xFFFFC107) // Amber
                    PowerUpType.ChickenStop -> Color(0xFF00BCD4) // Cyan
                    PowerUpType.TargetRefresh -> Color(0xFF4CAF50) // Green
                }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = powerUpColor,
                        radius = powerUp.radius,
                        center = Offset(powerUp.x * width, powerUp.y * height)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
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
                    text = "Combo: ${playingState.comboCount}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFA000),
                )
                Text(
                    text = "Time: ${playingState.timeLeft.toInt()}s",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3),
                )
            }
            // Power-up active indicators
            if (playingState.isScoreMultiplierActive) {
                Text("Score x2!", color = Color.Yellow, fontSize = 20.sp)
            }
            if (playingState.isTimeFreezeActive) {
                Text("Time Freeze!", color = Color.Magenta, fontSize = 20.sp)
            }
            if (playingState.isChickenStopActive) {
                Text("Chicken Stop!", color = Color.Cyan, fontSize = 20.sp)
            }
        }
    }
}

@Composable
fun GameOverScreen(
    finalScore: Int,
    highScore: Int,
    onRestartGame: () -> Unit,
    onNavigateToShop: () -> Unit,
    viewModel: GameViewModel = hiltViewModel()
) {
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
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "ハイスコア: $highScore",
            fontSize = 28.sp,
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
        Spacer(modifier = Modifier.size(16.dp))
        Button(
            onClick = onNavigateToShop,
            shape = CircleShape
        ) {
            Icon(Icons.Default.ShoppingBag, contentDescription = "Go to Shop", modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text(modifier = Modifier.padding(10.dp), text = "スキンを変更する", fontSize = 24.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))

        val activity = LocalContext.current.findActivity()
        Button(
            onClick = {
                if (activity == null) return@Button
                viewModel.showRewardedAd(activity) {  }
            },
            shape = CircleShape
        ) {
            Text(text = "広告を見て+10コイン", fontSize = 24.sp)
        }
    }
}
