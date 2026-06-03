package com.filelens.commander.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filelens.commander.domain.model.FileChecksum
import com.filelens.commander.domain.usecase.ChecksumUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class HashVerifyState(
    val path: String = "",
    val name: String = "",
    val isComputing: Boolean = false,
    val checksum: FileChecksum? = null,
    val expected: String = "",
    val matches: Match = Match.UNKNOWN
)

enum class Match { UNKNOWN, MATCH, MISMATCH }

@HiltViewModel
class HashVerifyViewModel @Inject constructor(
    private val checksumUseCase: ChecksumUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(HashVerifyState())
    val state: StateFlow<HashVerifyState> = _state.asStateFlow()

    fun load(path: String, name: String) {
        // Re-running for the same path would just recompute the same hash;
        // bail to avoid wasted IO when the dialog recomposes.
        if (_state.value.path == path && _state.value.checksum != null) return
        _state.value = HashVerifyState(path = path, name = name, isComputing = true)
        viewModelScope.launch {
            val checksum = checksumUseCase.compute(File(path))
            _state.value = _state.value.copy(
                isComputing = false,
                checksum = checksum,
                matches = compareIfReady(_state.value.expected, checksum)
            )
        }
    }

    fun setExpected(value: String) {
        val trimmed = value.trim()
        _state.value = _state.value.copy(
            expected = value,
            matches = compareIfReady(trimmed, _state.value.checksum)
        )
    }

    fun reset() {
        _state.value = HashVerifyState()
    }

    private fun compareIfReady(expected: String, checksum: FileChecksum?): Match {
        val trimmed = expected.trim().lowercase()
        if (trimmed.isEmpty() || checksum == null) return Match.UNKNOWN
        val sha = checksum.sha256?.lowercase()
        val md5 = checksum.md5?.lowercase()
        // Accept either SHA-256 or MD5 — pasted hash length picks the algorithm.
        return when (trimmed.length) {
            64 -> if (sha == trimmed) Match.MATCH else Match.MISMATCH
            32 -> if (md5 == trimmed) Match.MATCH else Match.MISMATCH
            else -> Match.UNKNOWN
        }
    }
}
