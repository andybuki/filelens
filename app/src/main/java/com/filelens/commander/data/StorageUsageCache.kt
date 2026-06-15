package com.filelens.commander.data

import com.filelens.commander.domain.usecase.FolderAnalysisUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-scoped cache for the latest Storage Usage analysis. ViewModels are
 * tied to the navigation back stack — every visit creates a new instance — so
 * we keep the last result here to make the screen open instantly on revisit.
 * A background refresh still runs to surface any changes since the previous
 * scan.
 */
@Singleton
class StorageUsageCache @Inject constructor() {
    data class Snapshot(
        val summary: FolderAnalysisUseCase.FolderSummary,
        val totalBytes: Long,
        val freeBytes: Long,
        val capturedAt: Long
    )

    @Volatile
    private var snapshot: Snapshot? = null

    fun get(): Snapshot? = snapshot

    fun put(snapshot: Snapshot) {
        this.snapshot = snapshot
    }
}
