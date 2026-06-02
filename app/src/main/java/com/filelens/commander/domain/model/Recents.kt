package com.filelens.commander.domain.model

data class RecentFile(
    val path: String,
    val displayName: String,
    val openedAt: Long
)

data class FrequentFolder(
    val path: String,
    val displayName: String,
    val lastVisitedAt: Long,
    val visitCount: Int
)
