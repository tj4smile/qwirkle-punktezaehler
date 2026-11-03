package com.thomas.qwirkle.data.model

/** Spaltennamen entsprechen der Query in GameHistoryDao.getStatistics(). */
data class StatisticsSummary(
    val totalGames: Int,
    val totalPoints: Int,
    val totalQwirkles: Int,
    val averagePoints: Double,
    val averageQwirkles: Double,
    val maxPoints: Int,
    val maxQwirkles: Int
)
