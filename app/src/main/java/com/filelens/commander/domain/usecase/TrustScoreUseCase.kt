package com.filelens.commander.domain.usecase

import com.filelens.commander.detector.DetectionResult
import com.filelens.commander.domain.model.InspectorResult
import com.filelens.commander.domain.model.RiskItem
import com.filelens.commander.domain.model.RiskSeverity
import com.filelens.commander.domain.model.TrustLevel
import com.filelens.commander.domain.model.TrustScore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrustScoreUseCase @Inject constructor() {

    fun compute(result: InspectorResult, detection: DetectionResult?): TrustScore {
        var score = 100
        for (risk in result.risks) {
            score -= weightFor(risk)
        }
        if (detection != null && detection.confidence < 0.3f && detection.detectedType == "Unknown") {
            score -= 5
        }
        score = score.coerceIn(0, 100)

        val level = when {
            score >= 90 -> TrustLevel.TRUSTED
            score >= 70 -> TrustLevel.LOW_RISK
            score >= 40 -> TrustLevel.MEDIUM_RISK
            else -> TrustLevel.HIGH_RISK
        }

        val reasons = result.risks
            .sortedByDescending { severityRank(it.severity) }
            .take(3)
            .map { reasonLine(it) }
            .ifEmpty { listOf("No risks detected.") }

        return TrustScore(level = level, numericScore = score, topReasons = reasons)
    }

    private fun weightFor(risk: RiskItem): Int = when (risk.severity) {
        RiskSeverity.DANGER -> 25
        RiskSeverity.WARNING -> 10
        RiskSeverity.INFO -> 3
    }

    private fun severityRank(severity: RiskSeverity): Int = when (severity) {
        RiskSeverity.DANGER -> 3
        RiskSeverity.WARNING -> 2
        RiskSeverity.INFO -> 1
    }

    private fun reasonLine(risk: RiskItem): String {
        val tag = when (risk.severity) {
            RiskSeverity.DANGER -> "Danger"
            RiskSeverity.WARNING -> "Warning"
            RiskSeverity.INFO -> "Info"
        }
        return "$tag: ${risk.title}"
    }
}
