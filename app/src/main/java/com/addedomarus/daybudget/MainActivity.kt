package com.addedomarus.daybudget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.addedomarus.daybudget.ui.DayBudgetTheme
import com.addedomarus.daybudget.ui.TodayRoute
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable

@Serializable object Today

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DayBudgetTheme {
                NavHost(rememberNavController(), startDestination = Today) {
                    composable<Today> { TodayRoute() }
                }
            }
        }
    }
}
