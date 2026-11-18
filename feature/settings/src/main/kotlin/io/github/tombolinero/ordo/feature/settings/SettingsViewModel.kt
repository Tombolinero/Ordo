package io.github.tombolinero.ordo.feature.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.tombolinero.ordo.core.backup.BackupException
import io.github.tombolinero.ordo.core.data.BackupService
import io.github.tombolinero.ordo.core.data.DataGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface BackupState {
    data object Idle : BackupState
    data class Working(@StringRes val text: Int) : BackupState
    data class Ready(val prepared: BackupService.Prepared) : BackupState
    data class Message(@StringRes val text: Int, val error: Boolean) : BackupState
}

class SettingsViewModel(private val graph: DataGraph) : ViewModel() {
    val trashCount: StateFlow<Int> = graph.repository.observeTrash().map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val backup = MutableStateFlow<BackupState>(BackupState.Idle)

    fun export(resolver: ContentResolver, uri: Uri) = viewModelScope.launch {
        backup.value = BackupState.Working(R.string.backup_saving)
        backup.value = runCatching {
            withContext(Dispatchers.IO) {
                resolver.openOutputStream(uri, "wt")?.use { graph.backups.export(it) } ?: error("no stream")
            }
        }.fold(
            onSuccess = { BackupState.Message(R.string.backup_saved, error = false) },
            onFailure = { BackupState.Message(R.string.backup_failed, error = true) },
        )
    }

    fun prepare(resolver: ContentResolver, uri: Uri) = viewModelScope.launch {
        backup.value = BackupState.Working(R.string.restore_reading)
        backup.value = try {
            val prepared = withContext(Dispatchers.IO) {
                resolver.openInputStream(uri)?.use { graph.backups.prepare(it) } ?: throw BackupException.NotABackup()
            }
            BackupState.Ready(prepared)
        } catch (e: BackupException) {
            BackupState.Message(errorText(e), error = true)
        } catch (e: Exception) {
            BackupState.Message(R.string.restore_error_generic, error = true)
        }
    }

    fun apply(prepared: BackupService.Prepared, mode: BackupService.Mode) = viewModelScope.launch {
        backup.value = BackupState.Working(R.string.restore_reading)
        backup.value = runCatching { graph.backups.apply(prepared, mode, graph.safetyBackupDir) }.fold(
            onSuccess = { BackupState.Message(R.string.restore_done, error = false) },
            onFailure = { BackupState.Message(R.string.restore_error_generic, error = true) },
        )
    }

    fun cancel() {
        (backup.value as? BackupState.Ready)?.prepared?.discard()
        backup.value = BackupState.Idle
    }

    fun dismissMessage() {
        if (backup.value is BackupState.Message) backup.value = BackupState.Idle
    }

    @StringRes
    private fun errorText(e: BackupException): Int = when (e) {
        is BackupException.NotABackup -> R.string.restore_error_not_backup
        is BackupException.Corrupted -> R.string.restore_error_damaged
        is BackupException.NewerFormat -> R.string.restore_error_newer
        is BackupException.TooLarge -> R.string.restore_error_large
    }
}
