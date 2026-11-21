package com.example.domain.model

enum class SupportedPair(
    val lunoPair: String,
    val baseSymbol: String,
    val displayName: String,
    val coinGeckoId: String,
    val binanceSymbol: String
) {
    XBTMYR("XBTMYR", "BTC", "Bitcoin", "bitcoin", "BTCUSDT"),
    ETHMYR("ETHMYR", "ETH", "Ethereum", "ethereum", "ETHUSDT"),
    SOLMYR("SOLMYR", "SOL", "Solana", "solana", "SOLUSDT"),
    AVAXMYR("AVAXMYR", "AVAX", "Avalanche", "avalanche-2", "AVAXUSDT"),
    XRPMYR("XRPMYR", "XRP", "Ripple", "ripple", "XRPUSDT"),
    ADAMYR("ADAMYR", "ADA", "Cardano", "cardano", "ADAUSDT");

    companion object {
        fun fromLunoPair(pair: String): SupportedPair? =
            entries.firstOrNull { it.lunoPair.equals(pair, ignoreCase = true) }
    }
}
