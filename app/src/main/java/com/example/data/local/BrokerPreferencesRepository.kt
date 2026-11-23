package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.BrokerConfig
import com.example.domain.model.NewsImpact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.brokerDataStore: DataStore<Preferences> by preferencesDataStore(name = "signalpulse_prefs")

class BrokerPreferencesRepository(private val context: Context) {

    private object Keys {
        val PAYOUT_PCT = intPreferencesKey("payout_pct")
        val SPREAD_PIPS = doublePreferencesKey("spread_pips")
        val NEWS_IMPACT = stringPreferencesKey("news_impact")
        val MIN_PAYOUT_THRESHOLD = intPreferencesKey("min_payout_threshold")
        val STRICT_M1_MASTER = booleanPreferencesKey("strict_m1_master")
        val FAVORITE_PAIRS = stringSetPreferencesKey("favorite_pairs")
        val SELECTED_PAIR_ID = stringPreferencesKey("selected_pair_id")
    }

    val brokerConfigFlow: Flow<BrokerConfig> = context.brokerDataStore.data.map { prefs ->
        BrokerConfig(
            payoutPct = prefs[Keys.PAYOUT_PCT] ?: 86,
            spreadPips = prefs[Keys.SPREAD_PIPS] ?: 0.8,
            newsImpact = runCatching {
                NewsImpact.valueOf(prefs[Keys.NEWS_IMPACT] ?: NewsImpact.NONE.name)
            }.getOrDefault(NewsImpact.NONE),
            minPayoutThresholdPct = prefs[Keys.MIN_PAYOUT_THRESHOLD] ?: 75,
            strictM1MasterMode = prefs[Keys.STRICT_M1_MASTER] ?: true
        )
    }

    val favoritePairsFlow: Flow<Set<String>> = context.brokerDataStore.data.map { prefs ->
        prefs[Keys.FAVORITE_PAIRS] ?: setOf("EUR/USD", "GBP/USD", "EUR/AUD", "EUR/USD OTC", "USD/PKR OTC")
    }

    val selectedPairIdFlow: Flow<String> = context.brokerDataStore.data.map { prefs ->
        prefs[Keys.SELECTED_PAIR_ID] ?: "EUR/USD"
    }

    suspend fun updateBrokerConfig(config: BrokerConfig) {
        context.brokerDataStore.edit { prefs ->
            prefs[Keys.PAYOUT_PCT] = config.payoutPct.coerceIn(50, 98)
            prefs[Keys.SPREAD_PIPS] = config.spreadPips.coerceIn(0.0, 15.0)
            prefs[Keys.NEWS_IMPACT] = config.newsImpact.name
            prefs[Keys.MIN_PAYOUT_THRESHOLD] = config.minPayoutThresholdPct.coerceIn(60, 95)
            prefs[Keys.STRICT_M1_MASTER] = config.strictM1MasterMode
        }
    }

    suspend fun toggleFavoritePair(pairId: String) {
        context.brokerDataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITE_PAIRS] ?: setOf("EUR/USD", "GBP/USD", "EUR/AUD", "EUR/USD OTC", "USD/PKR OTC")
            prefs[Keys.FAVORITE_PAIRS] = if (pairId in current) current - pairId else current + pairId
        }
    }

    suspend fun setSelectedPairId(pairId: String) {
        context.brokerDataStore.edit { prefs ->
            prefs[Keys.SELECTED_PAIR_ID] = pairId
        }
    }
}
