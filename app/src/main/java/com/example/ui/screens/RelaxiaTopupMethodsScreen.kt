package com.example.ui.screens

import androidx.compose.runtime.Composable
import com.example.data.api.LayaliApi

@Composable
internal fun RelaxiaTopupMethodsScreen(api: LayaliApi, back: () -> Unit) {
    CoinShopScreen(api, back)
}
