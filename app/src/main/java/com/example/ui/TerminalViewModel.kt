package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.TradingSignalAiRepository
import com.example.data.local.BrokerPreferencesRepository
import com.example.data.local.SignalDatabase
import com.example.data.local.SignalHistoryEntity
import com.example.data.remote.MarketRepository
import com.example.domain.model.AiSignalResult
import com.example.domain.model.BrokerConfig
import com.example.domain.model.CandleTimeframe
import com.example.domain.model.MarketFeedSnapshot
import com.example.domain.model.MarketPair
import com.example.domain.model.SignalDirection
import com.example.domain.model.SignalOutcome
import com.example.domain.model.TradeExpiry
import com.example.domain.model.TradingPairsCatalog
import com.example.domain.model.TrendClassification
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "TerminalViewModel"

sealed interface MarketFeedUiState {
    data object Loading : MarketFeedUiState
    data class Ready(val snapshot: MarketFeedSnapshot) : MarketFeedUiState
    data class Error(val message: String) : MarketFeedUiState
}

sealed interface SignalUiState {
    data object Idle : SignalUiState
    data object Analyzing : SignalUiState
    data class Success(val signal: AiSignalResult) : SignalUiState
    data class Error(val message: String) : SignalUiState
}

data class ScannerResultItem(
    val pair: MarketPair,
    val price: Double,
    val trend: TrendClassification,
    val callScore: Int,
    val putScore: Int,
    val rsi14: Double,
    val adx14: Double,
    val feedLabel: String
)

sealed interface ScannerUiState {
    data object Idle : ScannerUiState
    data object Scanning : ScannerUiState
    data class Ready(val items: List<ScannerResultItem>) : ScannerUiState
    data class Error(val message: String) : ScannerUiState
}

class TerminalViewModel(application: Application) : AndroidViewModel(application) {

    private val marketRepository = MarketRepository()
    private val aiRepository = TradingSignalAiRepository()
    private val prefsRepo = BrokerPreferencesRepository(application)
    private val signalDao = SignalDatabase.getInstance(application).signalHistoryDao()

    private val _selectedPair = MutableStateFlow(TradingPairsCatalog.allPairs.first())
    val selectedPair: StateFlow<MarketPair> = _selectedPair.asStateFlow()

    private val _selectedTimeframe = MutableStateFlow(CandleTimeframe.M1)
    val selectedTimeframe: StateFlow<CandleTimeframe> = _selectedTimeframe.asStateFlow()

    private val _selectedExpiry = MutableStateFlow(TradeExpiry.EXP_1M)
    val selectedExpiry: StateFlow<TradeExpiry> = _selectedExpiry.asStateFlow()

    val brokerConfig: StateFlow<BrokerConfig> = prefsRepo.brokerConfigFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, BrokerConfig())

    val favoritePairIds: StateFlow<Set<String>> = prefsRepo.favoritePairsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val signalHistory: StateFlow<List<AiSignalResult>> = signalDao.observeAll()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _marketFeedState = MutableStateFlow<MarketFeedUiState>(MarketFeedUiState.Loading)
    val marketFeedState: StateFlow<MarketFeedUiState> = _marketFeedState.asStateFlow()

    private val _signalState = MutableStateFlow<SignalUiState>(SignalUiState.Idle)
    val signalState: StateFlow<SignalUiState> = _signalState.asStateFlow()

    private val _remainingExpirySec = MutableStateFlow(0)
    val remainingExpirySec: StateFlow<Int> = _remainingExpirySec.asStateFlow()

    private val _scannerState = MutableStateFlow<ScannerUiState>(ScannerUiState.Idle)
    val scannerState: StateFlow<ScannerUiState> = _scannerState.asStateFlow()

    private var countdownJob: Job? = null
    private var feedJob: Job? = null

    init {
        viewModelScope.launch {
            val savedId = prefsRepo.selectedPairIdFlow.first()
            val pair = TradingPairsCatalog.findById(savedId)
            _selectedPair.value = pair
            refreshMarketFeed()
        }
        // Keep brokerConfig changes reflected cleanly when strict M1 mode changes
        viewModelScope.launch {
            combine(brokerConfig, _selectedExpiry) { cfg, exp -> cfg to exp }.collect { (cfg, exp) ->
                if (cfg.strictM1MasterMode && exp != TradeExpiry.EXP_1M) {
                    _selectedExpiry.value = TradeExpiry.EXP_1M
                }
            }
        }
    }

    fun selectPair(pair: MarketPair) {
        if (_selectedPair.value.id == pair.id) return
        _selectedPair.value = pair
        _signalState.value = SignalUiState.Idle
        countdownJob?.cancel()
        _remainingExpirySec.value = 0
        viewModelScope.launch {
            prefsRepo.setSelectedPairId(pair.id)
        }
        refreshMarketFeed()
    }

    fun selectTimeframe(timeframe: CandleTimeframe) {
        if (_selectedTimeframe.value == timeframe) return
        _selectedTimeframe.value = timeframe
        refreshMarketFeed()
    }

    fun selectExpiry(expiry: TradeExpiry) {
        if (brokerConfig.value.strictM1MasterMode && expiry != TradeExpiry.EXP_1M) {
            // Automatically disable strict M1 lock when user explicitly selects sub-minute expiry
            viewModelScope.launch {
                prefsRepo.updateBrokerConfig(brokerConfig.value.copy(strictM1MasterMode = false))
            }
        }
        _selectedExpiry.value = expiry
    }

    fun toggleFavoritePair(pairId: String) {
        viewModelScope.launch {
            prefsRepo.toggleFavoritePair(pairId)
        }
    }

    fun updateBrokerConfig(config: BrokerConfig) {
        viewModelScope.launch {
            prefsRepo.updateBrokerConfig(config)
            if (config.strictM1MasterMode) {
                _selectedExpiry.value = TradeExpiry.EXP_1M
                _selectedTimeframe.value = CandleTimeframe.M1
            }
            refreshMarketFeed()
        }
    }

    fun refreshMarketFeed() {
        feedJob?.cancel()
        feedJob = viewModelScope.launch {
            _marketFeedState.value = MarketFeedUiState.Loading
            val pair = _selectedPair.value
            val tf = _selectedTimeframe.value
            val cfg = brokerConfig.value
            val result = marketRepository.fetchMarketSnapshot(pair, tf, cfg)
            result.onSuccess { snapshot ->
                _marketFeedState.value = MarketFeedUiState.Ready(snapshot)
            }.onFailure { err ->
                if (err is CancellationException) throw err
                Log.e(TAG, "Market feed refresh failed", err)
                _marketFeedState.value = MarketFeedUiState.Error(
                    err.localizedMessage ?: "Failed to load market feed."
                )
            }
        }
    }

    fun generateAiSignal() {
        viewModelScope.launch {
            _signalState.value = SignalUiState.Analyzing
            countdownJob?.cancel()
            _remainingExpirySec.value = 0

            val currentSnapshot = when (val feed = _marketFeedState.value) {
                is MarketFeedUiState.Ready -> feed.snapshot
                else -> {
                    val fresh = marketRepository.fetchMarketSnapshot(
                        _selectedPair.value,
                        _selectedTimeframe.value,
                        brokerConfig.value
                    )
                    fresh.getOrElse { err ->
                        if (err is CancellationException) throw err
                        _signalState.value = SignalUiState.Error(
                            err.localizedMessage ?: "Cannot analyze without live market data."
                        )
                        return@launch
                    }.also { _marketFeedState.value = MarketFeedUiState.Ready(it) }
                }
            }

            val aiResult = aiRepository.analyzeAndGenerateSignal(
                snapshot = currentSnapshot,
                selectedExpiry = _selectedExpiry.value
            )

            aiResult.onSuccess { signal ->
                val insertedId = signalDao.insert(SignalHistoryEntity.fromDomain(signal))
                val savedSignal = signal.copy(id = insertedId)
                _signalState.value = SignalUiState.Success(savedSignal)
                if (savedSignal.direction != SignalDirection.NO_TRADE) {
                    startExpiryCountdown(savedSignal.expirySeconds)
                }
            }.onFailure { err ->
                if (err is CancellationException) throw err
                _signalState.value = SignalUiState.Error(
                    err.localizedMessage ?: "AI signal generation failed."
                )
            }
        }
    }

    private fun startExpiryCountdown(totalSeconds: Int) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var remaining = totalSeconds
            while (remaining >= 0) {
                _remainingExpirySec.value = remaining
                delay(1_000L)
                remaining--
            }
        }
    }

    fun runMultiPairScanner() {
        viewModelScope.launch {
            _scannerState.value = ScannerUiState.Scanning
            try {
                val cfg = brokerConfig.value
                val favs = favoritePairIds.value
                val scanPairs = TradingPairsCatalog.allPairs
                    .filter { it.id in favs }
                    .ifEmpty { TradingPairsCatalog.allPairs.take(6) }
                    .take(8)

                val items = coroutineScope {
                    scanPairs.map { pair ->
                        async {
                            marketRepository.fetchMarketSnapshot(pair, CandleTimeframe.M1, cfg)
                                .getOrNull()
                                ?.let { snap ->
                                    ScannerResultItem(
                                        pair = pair,
                                        price = snap.currentPrice,
                                        trend = snap.indicators.trendClassification,
                                        callScore = snap.indicators.callPassedCount,
                                        putScore = snap.indicators.putPassedCount,
                                        rsi14 = snap.indicators.rsi14,
                                        adx14 = snap.indicators.adx14,
                                        feedLabel = snap.feedSourceLabel
                                    )
                                }
                        }
                    }.awaitAll().filterNotNull()
                        .sortedByDescending { maxOf(it.callScore, it.putScore) }
                }

                if (items.isEmpty()) {
                    _scannerState.value = ScannerUiState.Error(
                        "Unable to fetch market snapshots. Check network connectivity."
                    )
                } else {
                    _scannerState.value = ScannerUiState.Ready(items)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Multi-pair scan failed", e)
                _scannerState.value = ScannerUiState.Error(
                    e.localizedMessage ?: "Market scan failed."
                )
            }
        }
    }

    fun markSignalOutcome(signalId: Long, outcome: SignalOutcome) {
        viewModelScope.launch {
            signalDao.updateOutcome(signalId, outcome.name)
            val current = _signalState.value
            if (current is SignalUiState.Success && current.signal.id == signalId) {
                _signalState.value = SignalUiState.Success(current.signal.copy(outcome = outcome))
            }
        }
    }

    fun deleteHistoryItem(signalId: Long) {
        viewModelScope.launch {
            signalDao.deleteById(signalId)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            signalDao.clearAll()
        }
    }
}
