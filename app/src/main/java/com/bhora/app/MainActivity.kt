package com.bhora.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bhora.app.ui.matchday.MatchdayScreen
import com.bhora.app.ui.matchday.MatchdayViewModel
import com.bhora.app.ui.theme.BhoraTheme
import java.time.ZoneId

class MainActivity : ComponentActivity() {

    private val viewModel: MatchdayViewModel by viewModels {
        MatchdayViewModel.factory((application as BhoraApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        setContent {
            BhoraTheme {
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val tzReceiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context?, intent: Intent?) {
                            viewModel.onTimeZoneChanged(ZoneId.systemDefault())
                        }
                    }
                    val tzFilter = IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)
                    ContextCompat.registerReceiver(
                        this@MainActivity,
                        tzReceiver,
                        tzFilter,
                        ContextCompat.RECEIVER_NOT_EXPORTED
                    )

                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_START -> {
                                viewModel.onTimeZoneChanged(ZoneId.systemDefault())
                                viewModel.startForegroundLivePolling()
                            }
                            Lifecycle.Event.ON_RESUME -> {
                                viewModel.onTimeZoneChanged(ZoneId.systemDefault())
                            }
                            Lifecycle.Event.ON_STOP -> viewModel.stopForegroundLivePolling()
                            else -> Unit
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                        unregisterReceiver(tzReceiver)
                        viewModel.stopForegroundLivePolling()
                    }
                }

                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                MatchdayScreen(
                    uiState = uiState,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    onFilterSelected = viewModel::onFilterSelected,
                    onServiceFilterSelected = viewModel::onServiceFilterSelected,
                    onToggleFixtureExpanded = viewModel::onToggleFixtureExpanded,
                    onPullToRefresh = viewModel::onPullToRefresh,
                    onRetry = viewModel::onRetry
                )
            }
        }
    }
}
