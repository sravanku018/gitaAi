package com.aipoweredgita.app.coin

/** Single coin transaction entry for history display. */
data class CoinEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val eventKey: String? = null,
    val amount: Int,
    val description: String,
    val timestamp: Long,
    val type: CoinTxType,
    val source: String? = null
)

enum class CoinTxType { EARN, SPEND }
