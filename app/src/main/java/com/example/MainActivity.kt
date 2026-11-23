package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CandlestickChart
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.TerminalViewModel
import com.example.ui.components.PairPickerBottomSheet
import com.example.ui.screens.BrokerSettingsBottomSheet
import com.example.ui.screens.JournalScreen
import com.example.ui.screens.MatrixScreen
import com.example.ui.screens.TerminalScreen
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.QuantCyan
import com.example.ui.theme.SignalPulseTheme
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

private enum class AppTab(
    val routeId: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    TERMINAL("terminal", R.string.nav_terminal, Icons.Filled.CandlestickChart, Icons.Outlined.CandlestickChart),
    MATRIX("matrix", R.string.nav_matrix, Icons.Filled.Analytics, Icons.Outlined.Analytics),
    JOURNAL("journal", R.string.nav_journal, Icons.Filled.History, Icons.Outlined.History)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent before any UI or ViewModel creation.
        configureAppCheck(this, intent)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )

        setContent {
            SignalPulseTheme {
                SignalPulseApp()
            }
        }
    }
}

@Composable
fun SignalPulseApp(
    viewModel: TerminalViewModel = viewModel()
) {
    var activeTab by rememberSaveable { mutableStateOf(AppTab.TERMINAL) }
    var showPairPicker by remember { mutableStateOf(false) }
    var showBrokerSettings by remember { mutableStateOf(false) }

    val selectedPair by viewModel.selectedPair.collectAsStateWithLifecycle()
    val selectedTimeframe by viewModel.selectedTimeframe.collectAsStateWithLifecycle()
    val selectedExpiry by viewModel.selectedExpiry.collectAsStateWithLifecycle()
    val brokerConfig by viewModel.brokerConfig.collectAsStateWithLifecycle()
    val favoritePairIds by viewModel.favoritePairIds.collectAsStateWithLifecycle()
    val marketFeedState by viewModel.marketFeedState.collectAsStateWithLifecycle()
    val signalState by viewModel.signalState.collectAsStateWithLifecycle()
    val remainingExpirySec by viewModel.remainingExpirySec.collectAsStateWithLifecycle()
    val scannerState by viewModel.scannerState.collectAsStateWithLifecycle()
    val signalHistory by viewModel.signalHistory.collectAsStateWithLifecycle()

    if (activeTab != AppTab.TERMINAL) {
        BackHandler {
            activeTab = AppTab.TERMINAL
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = ObsidianBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        containerColor = SlateSurface,
                        contentColor = TextPrimary,
                        tonalElevation = 0.dp
                    ) {
                        AppTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { activeTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = QuantCyan,
                                    selectedTextColor = QuantCyan,
                                    indicatorColor = QuantCyan.copy(alpha = 0.16f),
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_${tab.routeId}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = SlateSurface,
                        contentColor = TextPrimary
                    ) {
                        AppTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { activeTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = QuantCyan,
                                    selectedTextColor = QuantCyan,
                                    indicatorColor = QuantCyan.copy(alpha = 0.16f),
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("rail_${tab.routeId}")
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .background(BorderSlate)
                    )
                    Spacer(modifier = Modifier.width(0.dp))
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (activeTab) {
                        AppTab.TERMINAL -> TerminalScreen(
                            selectedPair = selectedPair,
                            selectedTimeframe = selectedTimeframe,
                            selectedExpiry = selectedExpiry,
                            brokerConfig = brokerConfig,
                            favoritePairIds = favoritePairIds,
                            marketFeedState = marketFeedState,
                            signalState = signalState,
                            remainingExpirySec = remainingExpirySec,
                            onOpenPairPicker = { showPairPicker = true },
                            onOpenBrokerSettings = { showBrokerSettings = true },
                            onSelectPair = viewModel::selectPair,
                            onSelectTimeframe = viewModel::selectTimeframe,
                            onSelectExpiry = viewModel::selectExpiry,
                            onRefreshFeed = viewModel::refreshMarketFeed,
                            onGenerateSignal = viewModel::generateAiSignal,
                            onMarkOutcome = viewModel::markSignalOutcome
                        )

                        AppTab.MATRIX -> MatrixScreen(
                            marketFeedState = marketFeedState,
                            scannerState = scannerState,
                            onRunScanner = viewModel::runMultiPairScanner,
                            onJumpToPair = { pair ->
                                viewModel.selectPair(pair)
                                activeTab = AppTab.TERMINAL
                            }
                        )

                        AppTab.JOURNAL -> JournalScreen(
                            history = signalHistory,
                            onMarkOutcome = viewModel::markSignalOutcome,
                            onDeleteItem = viewModel::deleteHistoryItem,
                            onClearAll = viewModel::clearHistory
                        )
                    }
                }
            }
        }
    }

    if (showPairPicker) {
        PairPickerBottomSheet(
            selectedPair = selectedPair,
            favoritePairIds = favoritePairIds,
            onSelectPair = viewModel::selectPair,
            onToggleFavorite = viewModel::toggleFavoritePair,
            onDismiss = { showPairPicker = false }
        )
    }

    if (showBrokerSettings) {
        BrokerSettingsBottomSheet(
            currentConfig = brokerConfig,
            onSave = viewModel::updateBrokerConfig,
            onDismiss = { showBrokerSettings = false }
        )
    }
}
