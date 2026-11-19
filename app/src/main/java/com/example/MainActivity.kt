package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.core.security.OAuthConsentCoordinator
import com.example.ui.navigation.ZarBankApp
import com.example.ui.theme.ZarBankTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var oAuthConsentCoordinator: OAuthConsentCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        oAuthConsentCoordinator.handleRedirectUri(intent?.data)
        setContent {
            ZarBankTheme {
                ZarBankApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        oAuthConsentCoordinator.handleRedirectUri(intent.data)
    }
}
