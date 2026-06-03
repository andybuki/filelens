package com.filelens.commander.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filelens.commander.domain.usecase.SqliteQueryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SqliteBrowserState(
    val dbPath: String = "",
    val tables: List<String> = emptyList(),
    val selectedTable: String? = null,
    val query: String = "",
    val isRunning: Boolean = false,
    val result: SqliteQueryUseCase.QueryResult? = null,
    val error: String? = null
)

@HiltViewModel
class SqliteBrowserViewModel @Inject constructor(
    private val sqlite: SqliteQueryUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SqliteBrowserState())
    val state: StateFlow<SqliteBrowserState> = _state.asStateFlow()

    fun load(dbPath: String) {
        if (_state.value.dbPath == dbPath && _state.value.tables.isNotEmpty()) return
        _state.value = SqliteBrowserState(dbPath = dbPath, isRunning = true)
        viewModelScope.launch {
            val tables = sqlite.listTables(dbPath)
            val first = tables.firstOrNull()
            _state.value = _state.value.copy(
                tables = tables,
                selectedTable = first,
                isRunning = false
            )
            if (first != null) selectTable(first)
        }
    }

    fun selectTable(table: String) {
        val path = _state.value.dbPath
        _state.value = _state.value.copy(selectedTable = table, isRunning = true, error = null)
        viewModelScope.launch {
            when (val r = sqlite.previewTable(path, table)) {
                is SqliteQueryUseCase.Result.Ok -> _state.value = _state.value.copy(
                    isRunning = false,
                    result = r.result,
                    error = null,
                    query = "SELECT * FROM \"${table.replace("\"", "\"\"")}\" LIMIT 100"
                )
                is SqliteQueryUseCase.Result.Error -> _state.value = _state.value.copy(
                    isRunning = false,
                    error = r.error.message,
                    result = null
                )
            }
        }
    }

    fun setQuery(value: String) {
        _state.value = _state.value.copy(query = value)
    }

    fun runQuery() {
        val path = _state.value.dbPath
        val sql = _state.value.query.trim()
        if (sql.isEmpty()) return
        _state.value = _state.value.copy(isRunning = true, error = null)
        viewModelScope.launch {
            when (val r = sqlite.query(path, sql)) {
                is SqliteQueryUseCase.Result.Ok -> _state.value = _state.value.copy(
                    isRunning = false,
                    result = r.result,
                    error = null
                )
                is SqliteQueryUseCase.Result.Error -> _state.value = _state.value.copy(
                    isRunning = false,
                    error = r.error.message
                )
            }
        }
    }
}
