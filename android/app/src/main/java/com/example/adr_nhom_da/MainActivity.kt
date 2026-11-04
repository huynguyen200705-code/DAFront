package com.example.adr_nhom_da

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.adr_nhom_da.data.model.User
import com.example.adr_nhom_da.data.remote.WebApiSyncEngine
import com.example.adr_nhom_da.data.repository.RestaurantRepository
import com.example.adr_nhom_da.ui.screens.auth.LoginScreen
import com.example.adr_nhom_da.ui.screens.auth.RegisterScreen
import com.example.adr_nhom_da.ui.screens.home.MainContainerScreen
import com.example.adr_nhom_da.ui.theme.AdrNhomDaTheme

enum class ScreenState {
    LOGIN,
    REGISTER,
    MAIN_APP
}

class MainActivity : ComponentActivity() {

    private lateinit var repository: RestaurantRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Bảo vệ toàn cục: ghi nhận mọi ngoại lệ không mong muốn, tránh sập app đột ngột
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("APP_CRASH", "Bắt ngoại lệ trên luồng ${thread.name}: ${throwable.message}", throwable)
        }

        try {
            repository = RestaurantRepository(this)
            
            // Khởi động Engine đồng bộ 2 chiều liên tục với Web API
            WebApiSyncEngine.start(applicationContext, repository)

            val savedUser = repository.getSavedLoggedInUser()

            setContent {
                AdrNhomDaTheme {
                    var currentScreen by remember { mutableStateOf(if (savedUser != null) ScreenState.MAIN_APP else ScreenState.LOGIN) }
                    var currentUser by remember { mutableStateOf(savedUser) }

                    when (currentScreen) {
                        ScreenState.LOGIN -> {
                            LoginScreen(
                                repository = repository,
                                onLoginSuccess = { user ->
                                    repository.saveLoggedInUser(user)
                                    currentUser = user
                                    currentScreen = ScreenState.MAIN_APP
                                },
                                onNavigateToRegister = {
                                    currentScreen = ScreenState.REGISTER
                                }
                            )
                        }

                        ScreenState.REGISTER -> {
                            RegisterScreen(
                                repository = repository,
                                onRegisterSuccess = {
                                    currentScreen = ScreenState.LOGIN
                                },
                                onNavigateBackToLogin = {
                                    currentScreen = ScreenState.LOGIN
                                }
                            )
                        }

                        ScreenState.MAIN_APP -> {
                            currentUser?.let { user ->
                                MainContainerScreen(
                                    currentUser = user,
                                    repository = repository,
                                    onLogout = {
                                        repository.clearLoggedInUser()
                                        currentUser = null
                                        currentScreen = ScreenState.LOGIN
                                    }
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Lỗi khởi tạo Activity: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        WebApiSyncEngine.stop()
    }
}
