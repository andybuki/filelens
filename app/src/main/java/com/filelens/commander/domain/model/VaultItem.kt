package com.filelens.commander.domain.model

/**
 * An item stored inside the locked vault. The on-disk file lives at
 * [vaultPath] (under app-private storage); [originalPath] is preserved
 * so a "move out" action can put it back where it came from.
 */
data class VaultItem(
    val vaultPath: String,
    val originalPath: String,
    val displayName: String,
    val size: Long,
    val addedAt: Long
)
