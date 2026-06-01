package com.aipoweredgita.app.coin

/** Single coin transaction entry for history display. */
data class CoinEntry(
    val amount: Int,
    val description: String,
    val timestamp: Long,
    val type: CoinTxType
)

enum class CoinTxType { EARN, SPEND }
