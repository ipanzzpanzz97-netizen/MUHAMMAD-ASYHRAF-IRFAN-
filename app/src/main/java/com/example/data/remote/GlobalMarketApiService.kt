package com.example.data.remote

import com.example.domain.model.CandlePoint
import com.example.domain.model.SupportedPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class FearGreedSnapshot(
    val value: Int,
    val label: String,
    val labelBm: String
)

data class CoinGeckoGlobalPrice(
    val myrPrice: Double,
    val usdPrice: Double,
    val change24hPct: Double
)

class GlobalMarketApiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
) {

    suspend fun fetchFearAndGreed(): FearGreedSnapshot = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.alternative.me/fng/?limit=1")
            .header("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Fear & Greed HTTP ${response.code}")
            }
            val root = JSONObject(response.body?.string().orEmpty())
            val data = root.optJSONArray("data")?.optJSONObject(0)
                ?: throw IllegalStateException("Tiada data Fear & Greed")
            val value = data.optString("value", "50").toIntOrNull() ?: 50
            val label = data.optString("value_classification", "Neutral")
            val labelBm = when {
                value <= 24 -> "Ketakutan Ekstrem"
                value <= 44 -> "Ketakutan"
                value <= 55 -> "Neutral"
                value <= 74 -> "Tamak"
                else -> "Tamak Ekstrem"
            }
            FearGreedSnapshot(value = value, label = label, labelBm = labelBm)
        }
    }

    suspend fun fetchCoinGeckoGlobalPrices(): Map<String, CoinGeckoGlobalPrice> =
        withContext(Dispatchers.IO) {
            val ids = SupportedPair.entries.joinToString(",") { it.coinGeckoId }
            val url =
                "https://api.coingecko.com/api/v3/simple/price?ids=$ids&vs_currencies=myr,usd&include_24hr_change=true"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "LunoIntelAIScanner/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("CoinGecko HTTP ${response.code}")
                }
                val root = JSONObject(response.body?.string().orEmpty())
                val result = mutableMapOf<String, CoinGeckoGlobalPrice>()
                for (pair in SupportedPair.entries) {
                    val obj = root.optJSONObject(pair.coinGeckoId) ?: continue
                    result[pair.lunoPair] = CoinGeckoGlobalPrice(
                        myrPrice = obj.optDouble("myr", 0.0),
                        usdPrice = obj.optDouble("usd", 0.0),
                        change24hPct = obj.optDouble("myr_24h_change", 0.0)
                    )
                }
                result
            }
        }

    /**
     * Public Binance OHLCV fallback scaled to MYR using live Luno/USD conversion factor
     * when Luno /api/exchange/1/candles requires API auth.
     */
    suspend fun fetchBinanceCandlesScaledToMyr(
        binanceSymbol: String,
        interval: String,
        myrPerUsdFactor: Double,
        limit: Int = 250
    ): List<CandlePoint> = withContext(Dispatchers.IO) {
        val url = "https://api.binance.com/api/v3/klines?symbol=$binanceSymbol&interval=$interval&limit=$limit"
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Binance Klines $binanceSymbol HTTP ${response.code}")
            }
            val arr = JSONArray(response.body?.string().orEmpty())
            val scale = if (myrPerUsdFactor > 0.0) myrPerUsdFactor else 4.45
            val list = ArrayList<CandlePoint>(arr.length())
            for (i in 0 until arr.length()) {
                val k = arr.optJSONArray(i) ?: continue
                list.add(
                    CandlePoint(
                        timestampMs = k.optLong(0, 0L),
                        open = (k.optString(1, "0").toDoubleOrNull() ?: 0.0) * scale,
                        high = (k.optString(2, "0").toDoubleOrNull() ?: 0.0) * scale,
                        low = (k.optString(3, "0").toDoubleOrNull() ?: 0.0) * scale,
                        close = (k.optString(4, "0").toDoubleOrNull() ?: 0.0) * scale,
                        volume = k.optString(5, "0").toDoubleOrNull() ?: 0.0
                    )
                )
            }
            list
        }
    }
}
