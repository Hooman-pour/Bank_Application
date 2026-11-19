package com.example.ui.navigation

import kotlinx.serialization.Serializable

sealed interface ZarBankRoute {
    @Serializable
    data object Dashboard : ZarBankRoute

    @Serializable
    data object Transfers : ZarBankRoute

    @Serializable
    data object Tools : ZarBankRoute

    @Serializable
    data object Security : ZarBankRoute
}
