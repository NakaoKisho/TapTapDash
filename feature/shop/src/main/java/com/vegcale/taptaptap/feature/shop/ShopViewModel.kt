package com.vegcale.taptaptap.feature.shop

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class Skin(
    val id: String,
    val name: String,
    val price: Int,
    val isOwned: Boolean,
    val isSelected: Boolean
)

data class ShopUiState(
    val coins: Int,
    val skins: List<Skin>
)

@HiltViewModel
class ShopViewModel @Inject constructor(
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShopUiState(0, emptyList()))
    val uiState: StateFlow<ShopUiState> = _uiState.asStateFlow()

    private val COIN_KEY = "coin"
    private val OWNED_SKINS_KEY = "owned_skins"
    private val SELECTED_SKIN_KEY = "selected_skin"

    init {
        loadShopState()
    }

    private fun loadShopState() {
        viewModelScope.launch {
            val coins = sharedPreferences.getInt(COIN_KEY, 0)
            val ownedSkins = sharedPreferences.getStringSet(OWNED_SKINS_KEY, setOf("default")) ?: setOf("default")
            val selectedSkin = sharedPreferences.getString(SELECTED_SKIN_KEY, "default")

            val skins = listOf(
                Skin("default", "Default", 0, ownedSkins.contains("default"), selectedSkin == "default"),
                Skin("gold", "Gold", 100, ownedSkins.contains("gold"), selectedSkin == "gold"),
                Skin("ninja", "Ninja", 200, ownedSkins.contains("ninja"), selectedSkin == "ninja")
            )

            _uiState.value = ShopUiState(coins, skins)
        }
    }

    fun onBuyClick(skin: Skin) {
        viewModelScope.launch {
            val coins = _uiState.value.coins
            if (coins >= skin.price) {
                val newCoins = coins - skin.price
                val ownedSkins = sharedPreferences.getStringSet(OWNED_SKINS_KEY, setOf("default"))?.toMutableSet() ?: mutableSetOf("default")
                ownedSkins.add(skin.id)
                sharedPreferences.edit()
                    .putInt(COIN_KEY, newCoins)
                    .putStringSet(OWNED_SKINS_KEY, ownedSkins)
                    .apply()
                loadShopState()
            }
        }
    }

    fun onSelectClick(skin: Skin) {
        viewModelScope.launch {
            sharedPreferences.edit().putString(SELECTED_SKIN_KEY, skin.id).apply()
            loadShopState()
        }
    }
}
