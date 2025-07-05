package com.vegcale.taptaptap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.vegcale.taptaptap.feature.game.GameScreen
import com.vegcale.taptaptap.feature.settings.SettingsScreen
import com.vegcale.taptaptap.ui.theme.taptaptapTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            taptaptapTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val scope = rememberCoroutineScope()
                    LaunchedEffect(Unit) {
                        scope.launch {
                            MobileAds.initialize(this@MainActivity) {}
                        }
                    }

                    Column {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val context = LocalContext.current
                            val adView = remember { AdView(context) }
                            adView.adUnitId = BuildConfig.adUnitId
                            val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(LocalContext.current, 360)
                            adView.setAdSize(adSize)

                            val adRequest = AdRequest.Builder().build()
                            adView.loadAd(adRequest)

                            BannerAd(adView, Modifier)
                        }
                        TapTapTapApp()
                    }
                }
            }
        }
    }
}

@Composable
fun BannerAd(adView: AdView, modifier: Modifier = Modifier) {
    // Ad load does not work in preview mode because it requires a network connection.
    if (LocalInspectionMode.current) {
        Box { Text(text = "Google Mobile Ads preview banner.", modifier.align(Alignment.Center)) }
        return
    }

    AndroidView(modifier = modifier.fillMaxWidth(), factory = { adView })
}

@Composable
fun TapTapTapApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "game") {
        composable("game") {
            GameScreen()
        }
        composable("settings") {
            SettingsScreen()
        }
    }
}