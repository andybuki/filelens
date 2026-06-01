package com.filelens.commander.domain.usecase

import com.filelens.commander.domain.model.SecretMatch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecretScanner @Inject constructor() {

    private data class Pattern(val kind: String, val regex: Regex)

    private val patterns: List<Pattern> = listOf(
        Pattern("AWS access key", Regex("\\bAKIA[0-9A-Z]{16}\\b")),
        Pattern("AWS secret key", Regex("(?i)aws[_-]?secret[_-]?access[_-]?key\\s*[:=]\\s*[\"']?([A-Za-z0-9/+=]{40})[\"']?")),
        Pattern("Google API key", Regex("\\bAIza[0-9A-Za-z_-]{35}\\b")),
        Pattern("GCP service account", Regex("\"type\"\\s*:\\s*\"service_account\"")),
        Pattern("GitHub PAT", Regex("\\bghp_[A-Za-z0-9]{36}\\b")),
        Pattern("GitHub OAuth", Regex("\\bgho_[A-Za-z0-9]{36}\\b")),
        Pattern("GitHub fine-grained PAT", Regex("\\bgithub_pat_[A-Za-z0-9_]{82}\\b")),
        Pattern("Slack token", Regex("\\bxox[abrs]-[A-Za-z0-9-]{10,}\\b")),
        Pattern("Stripe live key", Regex("\\bsk_live_[0-9A-Za-z]{24,}\\b")),
        Pattern("Stripe test key", Regex("\\bsk_test_[0-9A-Za-z]{24,}\\b")),
        Pattern("OpenAI key", Regex("\\bsk-[A-Za-z0-9]{20,}\\b")),
        Pattern("Anthropic key", Regex("\\bsk-ant-[A-Za-z0-9_-]{20,}\\b")),
        Pattern("Twilio SID", Regex("\\bAC[a-f0-9]{32}\\b")),
        Pattern("SendGrid key", Regex("\\bSG\\.[A-Za-z0-9_-]{22}\\.[A-Za-z0-9_-]{43}\\b")),
        Pattern("Heroku key", Regex("(?i)heroku[_-]?api[_-]?key\\s*[:=]\\s*[\"']?[0-9a-f]{8}-([0-9a-f]{4}-){3}[0-9a-f]{12}")),
        Pattern("JWT", Regex("\\beyJ[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}\\b")),
        Pattern("PEM private key", Regex("-----BEGIN\\s+(?:RSA |EC |DSA |OPENSSH |PGP )?PRIVATE KEY-----")),
        Pattern("PKCS8 private key", Regex("-----BEGIN ENCRYPTED PRIVATE KEY-----")),
        Pattern("SSH private key", Regex("-----BEGIN OPENSSH PRIVATE KEY-----")),
        Pattern("Authorization Bearer", Regex("(?i)Authorization\\s*:\\s*Bearer\\s+[A-Za-z0-9._~+/=-]{16,}")),
        Pattern("Authorization Basic", Regex("(?i)Authorization\\s*:\\s*Basic\\s+[A-Za-z0-9+/=]{8,}")),
        Pattern("Database URL", Regex("(?i)(jdbc:|postgres(?:ql)?://|mysql://|mongodb(?:\\+srv)?://|redis://|amqps?://)[^\\s\"'<>`]{8,}")),
        Pattern("Generic API key", Regex("(?i)(api[_-]?key|apikey)\\s*[:=]\\s*[\"']?([A-Za-z0-9_\\-]{20,})[\"']?")),
        Pattern("Generic token", Regex("(?i)(access[_-]?token|auth[_-]?token|bearer[_-]?token|secret[_-]?token)\\s*[:=]\\s*[\"']?([A-Za-z0-9_\\-\\.]{16,})[\"']?")),
        Pattern("Generic password", Regex("(?i)(password|passwd|pwd)\\s*[:=]\\s*[\"']([^\"']{6,})[\"']")),
        Pattern("Generic secret", Regex("(?i)(client[_-]?secret|app[_-]?secret|webhook[_-]?secret)\\s*[:=]\\s*[\"']?([A-Za-z0-9_\\-]{16,})[\"']?"))
    )

    fun scan(text: String, location: String? = null, maxMatches: Int = 50): List<SecretMatch> {
        if (text.isEmpty()) return emptyList()
        val out = mutableListOf<SecretMatch>()
        val seen = mutableSetOf<String>()
        for (p in patterns) {
            for (match in p.regex.findAll(text)) {
                val raw = match.value
                val dedupeKey = p.kind + "::" + raw.hashCode()
                if (!seen.add(dedupeKey)) continue
                val redacted = redact(raw)
                val line = lineNumberFor(text, match.range.first)
                out += SecretMatch(
                    kind = p.kind,
                    redactedSample = redacted,
                    location = location,
                    lineNumber = line
                )
                if (out.size >= maxMatches) return out
            }
        }
        return out
    }

    private fun redact(raw: String): String {
        val head = raw.take(4)
        val tail = if (raw.length > 8) raw.takeLast(2) else ""
        return "$head…$tail"
    }

    private fun lineNumberFor(text: String, charIndex: Int): Int {
        if (charIndex <= 0) return 1
        var line = 1
        val limit = charIndex.coerceAtMost(text.length)
        for (i in 0 until limit) {
            if (text[i] == '\n') line++
        }
        return line
    }
}
