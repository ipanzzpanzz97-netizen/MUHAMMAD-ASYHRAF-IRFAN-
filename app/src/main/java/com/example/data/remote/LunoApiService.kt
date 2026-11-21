package com.example.data.remote

import android.util.Base64
import com.example.domain.model.CandlePoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class LunoTickerSnapshot(
    val pair: String,
    val ask: Double,
    val bid: Double,
    val lastTrade: Double,
    val volume24h: Double,
    val status: String,
    val timestampMs: Long
)

data class LunoOrderBookTopSnapshot(
    val bestBid: Double,
    val bestAsk: Double,
    val bidDepthVolume: Double,
    val askDepthVolume: Double
)

class LunoApiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
) {

    suspend fun fetchAllTickers(): Map<String, LunoTickerSnapshot> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.luno.com/api/1/tickers")
            .header("Accept", "application/json")
            .header("User-Agent", "LunoIntelAIScanner/1.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Luno Tickers HTTP ${response.code}")
            }
            val body = response.body?.string().orEmpty()
            val root = JSONObject(body)
            val tickersArray = root.optJSONArray("tickers") ?: return@withContext emptyMap()
            val result = mutableMapOf<String, LunoTickerSnapshot>()
            for (i in 0 until tickersArray.length()) {
                val obj = tickersArray.optJSONObject(i) ?: continue
                val pair = obj.optString("pair", "")
                if (pair.isNotBlank()) {
                    result[pair] = LunoTickerSnapshot(
                        pair = pair,
                        ask = obj.optString("ask", "0").toDoubleOrNull() ?: 0.0,
                        bid = obj.optString("bid", "0").toDoubleOrNull() ?: 0.0,
                        lastTrade = obj.optString("last_trade", "0").toDoubleOrNull() ?: 0.0,
                        volume24h = obj.optString("rolling_24_hour_volume", "0").toDoubleOrNull() ?: 0.0,
                        status = obj.optString("status", "UNKNOWN"),
                        timestampMs = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                }
            }
            result
        }
    }

    suspend fun fetchSingleTicker(pair: String): LunoTickerSnapshot = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.luno.com/api/1/ticker?pair=$pair")
            .header("Accept", "application/json")
            .header("User-Agent", "LunoIntelAIScanner/1.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Luno Ticker $pair HTTP ${response.code}")
            }
            val obj = JSONObject(response.body?.string().orEmpty())
            LunoTickerSnapshot(
                pair = obj.optString("pair", pair),
                ask = obj.optString("ask", "0").toDoubleOrNull() ?: 0.0,
                bid = obj.optString("bid", "0").toDoubleOrNull() ?: 0.0,
                lastTrade = obj.optString("last_trade", "0").toDoubleOrNull() ?: 0.0,
                volume24h = obj.optString("rolling_24_hour_volume", "0").toDoubleOrNull() ?: 0.0,
                status = obj.optString("status", "UNKNOWN"),
                timestampMs = obj.optLong("timestamp", System.currentTimeMillis())
            )
        }
    }

    suspend fun fetchOrderBookTop(pair: String): LunoOrderBookTopSnapshot? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("https://api.luno.com/api/1/orderbook_top?pair=$pair")
                .header("Accept", "application/json")
                .header("User-Agent", "LunoIntelAIScanner/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching null
                val root = JSONObject(response.body?.string().orEmpty())
                val bids = root.optJSONArray("bids")
                val asks = root.optJSONArray("asks")
                val bestBid = bids?.optJSONObject(0)?.optString("price")?.toDoubleOrNull() ?: 0.0
                val bestAsk = asks?.optJSONObject(0)?.optString("price")?.toDoubleOrNull() ?: 0.0
                var bidVol = 0.0
                if (bids != null) {
                    for (i in 0 until minOf(10, bids.length())) {
                        bidVol += bids.optJSONObject(i)?.optString("volume")?.toDoubleOrNull() ?: 0.0
                    }
                }
                var askVol = 0.0
                if (asks != null) {
                    for (i in 0 until minOf(10, asks.length())) {
                        askVol += asks.optJSONObject(i)?.optString("volume")?.toDoubleOrNull() ?: 0.0
                    }
                }
                LunoOrderBookTopSnapshot(
                    bestBid = bestBid,
                    bestAsk = bestAsk,
                    bidDepthVolume = bidVol,
                    askDepthVolume = askVol
                )
            }
        }.getOrNull()
    }

    /**
     * Fetches Luno Exchange candles (/api/exchange/1/candles).
     * Uses optional Read-Only Basic Auth if supplied; throws if HTTP != 2xx so repository can fallback.
     */
    suspend fun fetchLunoCandles(
        pair: String,
        sinceMs: Long,
        durationSeconds: Int,
        lunoKeyId: String = "",
        lunoKeySecret: String = ""
    ): List<CandlePoint> = withContext(Dispatchers.IO) {
        val url = "https://api.luno.com/api/exchange/1/candles?pair=$pair&since=$sinceMs&duration=$durationSeconds"
        val builder = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "LunoIntelAIScanner/1.0")

        if (lunoKeyId.isNotBlank() && lunoKeySecret.isNotBlank()) {
            val creds = "$lunoKeyId:$lunoKeySecret"
            val encoded = Base64.encodeToString(creds.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            builder.header("Authorization", "Basic $encoded")
        }

        client.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Luno Candles HTTP ${response.code}")
            }
            val root = JSONObject(response.body?.string().orEmpty())
            val arr = root.optJSONArray("candles") ?: return@withContext emptyList()
            val list = ArrayList<CandlePoint>(arr.length())
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                list.add(
                    CandlePoint(
                        timestampMs = item.optLong("timestamp", 0L),
                        open = item.optString("open", "0").toDoubleOrNull() ?: 0.0,
                        high = item.optString("high", "0").toDoubleOrNull() ?: 0.0,
                        low = item.optString("low", "0").toDoubleOrNull() ?: 0.0,
                        close = item.optString("close", "0").toDoubleOrNull() ?: 0.0,
                        volume = item.optString("volume", "0").toDoubleOrNull() ?: 0.0
                    )
                )
            }
            list
        }
    }
}
