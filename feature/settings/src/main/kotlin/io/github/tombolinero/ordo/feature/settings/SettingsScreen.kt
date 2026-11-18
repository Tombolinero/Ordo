package io.github.tombolinero.ordo.feature.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.tombolinero.ordo.core.backup.BackupArchive
import io.github.tombolinero.ordo.core.data.BackupService
import io.github.tombolinero.ordo.core.designsystem.component.DotMatrixText
import io.github.tombolinero.ordo.core.designsystem.component.GlassIconButton
import io.github.tombolinero.ordo.core.designsystem.component.GlassPillButton
import io.github.tombolinero.ordo.core.designsystem.component.OrdoBackground
import io.github.tombolinero.ordo.core.designsystem.component.OrdoTopBar
import io.github.tombolinero.ordo.core.designsystem.component.PillowSurface
import io.github.tombolinero.ordo.core.designsystem.icon.OrdoIcons
import io.github.tombolinero.ordo.core.designsystem.motion.OrdoMotion
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import io.github.tombolinero.ordo.core.ui.OrdoSheet
import io.github.tombolinero.ordo.core.ui.R as UiR
import io.github.tombolinero.ordo.core.ui.UndoBar
import io.github.tombolinero.ordo.core.ui.UndoMessage
import io.github.tombolinero.ordo.core.ui.ordoViewModel
import io.github.tombolinero.ordo.core.ui.rememberUndoState
import java.time.LocalDate

const val SOURCE_URL = "https://github.com/Tombolinero/Ordo"

@Composable
fun SettingsRoute(versionName: String, onBack: () -> Unit, onOpenTrash: () -> Unit) {
    val vm = ordoViewModel { SettingsViewModel(it) }
    val trash by vm.trashCount.collectAsStateWithLifecycle()
    val backup by vm.backup.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val createDoc = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) vm.export(context.contentResolver, uri)
    }
    val openDoc = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.prepare(context.contentResolver, uri)
    }
    SettingsScreen(
        versionName = versionName,
        trashCount = trash,
        backup = backup,
        onBack = onBack,
        onCreateBackup = { createDoc.launch("Ordo-Backup-${LocalDate.now()}.${BackupArchive.FILE_EXTENSION}") },
        onRestoreBackup = { openDoc.launch(arrayOf("*/*")) },
        onApply = vm::apply,
        onCancelRestore = vm::cancel,
        onMessageShown = vm::dismissMessage,
        onOpenTrash = onOpenTrash,
    )
}

@Composable
internal fun SettingsScreen(
    versionName: String,
    trashCount: Int,
    backup: BackupState,
    onBack: () -> Unit,
    onCreateBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onApply: (BackupService.Prepared, BackupService.Mode) -> Unit,
    onCancelRestore: () -> Unit,
    onMessageShown: () -> Unit,
    onOpenTrash: () -> Unit,
) {
    val context = LocalContext.current
    val messages = rememberUndoState()
    val messageText = (backup as? BackupState.Message)?.let { stringResource(it.text) }
    LaunchedEffect(backup) {
        if (backup is BackupState.Message && messageText != null) {
            messages.show(UndoMessage(messageText))
            onMessageShown()
        }
    }
    OrdoBackground {
        Column(Modifier.fillMaxSize()) {
            OrdoTopBar(stringResource(R.string.settings_title), navigation = { GlassIconButton(OrdoIcons.Back, stringResource(UiR.string.action_back), onBack) })
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).windowInsetsPadding(WindowInsets.navigationBars),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Section(stringResource(R.string.section_backup)) {
                    Entry(OrdoIcons.Backup, stringResource(R.string.backup_create), stringResource(R.string.backup_create_hint), onCreateBackup)
                    Entry(OrdoIcons.Upload, stringResource(R.string.backup_restore), stringResource(R.string.backup_restore_hint), onRestoreBackup)
                    AnimatedVisibility(backup is BackupState.Working, enter = fadeIn(OrdoMotion.smooth()), exit = fadeOut(OrdoMotion.smooth())) {
                        Text(
                            (backup as? BackupState.Working)?.let { stringResource(it.text) }.orEmpty(),
                            style = OrdoTheme.type.label, color = OrdoTheme.colors.accent,
                            modifier = Modifier.padding(start = 52.dp, top = 4.dp),
                        )
                    }
                }
                Section(stringResource(R.string.section_trash)) {
                    Entry(OrdoIcons.Trash, stringResource(R.string.trash_title), stringResource(R.string.trash_hint), onOpenTrash) {
                        if (trashCount > 0) DotMatrixText(trashCount.coerceAtMost(99).toString().padStart(2, '0'), 18.dp)
                    }
                }
                if (Build.VERSION.SDK_INT >= 33) {
                    Section(stringResource(R.string.section_language)) {
                        Entry(OrdoIcons.Language, stringResource(R.string.language_open), stringResource(R.string.language_hint), {
                            runCatching {
                                context.startActivity(Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.fromParts("package", context.packageName, null)))
                            }
                        })
                    }
                }
                Section(stringResource(R.string.section_about)) {
                    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        DotMatrixText("01", 20.dp, color = OrdoTheme.colors.accent)
                        Column {
                            Text("Ordo", style = OrdoTheme.type.cardTitle, color = OrdoTheme.colors.textPrimary)
                            Text(stringResource(R.string.about_version, versionName), style = OrdoTheme.type.label, color = OrdoTheme.colors.textSecondary)
                        }
                    }
                    Text(stringResource(R.string.about_license), style = OrdoTheme.type.label, color = OrdoTheme.colors.textSecondary, modifier = Modifier.padding(top = 4.dp))
                    Entry(OrdoIcons.ChevronRight, stringResource(R.string.about_source), null, {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SOURCE_URL))) }
                    })
                }
                Spacer(Modifier.height(90.dp))
            }
        }
        UndoBar(messages, Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 24.dp, start = 18.dp, end = 18.dp))
    }
    if (backup is BackupState.Ready) RestoreSheet(backup.prepared, onApply, onCancelRestore)
}

@Composable
private fun RestoreSheet(prepared: BackupService.Prepared, onApply: (BackupService.Prepared, BackupService.Mode) -> Unit, onCancel: () -> Unit) {
    val s = prepared.summary
    OrdoSheet(onDismiss = onCancel) {
        Text(stringResource(R.string.restore_title), style = OrdoTheme.type.screenTitle, color = OrdoTheme.colors.textPrimary, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        Text(stringResource(R.string.restore_summary, s.lists, s.items, s.images), style = OrdoTheme.type.body, color = OrdoTheme.colors.textSecondary, modifier = Modifier.padding(start = 4.dp))
        if (s.createdAt.length >= 10) {
            Text(stringResource(R.string.restore_created, s.createdAt.take(10)), style = OrdoTheme.type.label, color = OrdoTheme.colors.textTertiary, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
        }
        if (s.fromNewerApp) {
            Text(stringResource(R.string.restore_newer), style = OrdoTheme.type.label, color = OrdoTheme.colors.accent, modifier = Modifier.padding(start = 4.dp, top = 12.dp))
        }
        Spacer(Modifier.height(18.dp))
        ModeChoice(stringResource(R.string.restore_merge), stringResource(R.string.restore_merge_hint), OrdoIcons.Merge) { onApply(prepared, BackupService.Mode.Merge) }
        Spacer(Modifier.height(10.dp))
        ModeChoice(stringResource(R.string.restore_replace), stringResource(R.string.restore_replace_hint), OrdoIcons.Restore) { onApply(prepared, BackupService.Mode.Replace) }
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.restore_safety), style = OrdoTheme.type.meta, color = OrdoTheme.colors.textTertiary, modifier = Modifier.padding(horizontal = 4.dp))
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) { GlassPillButton(stringResource(UiR.string.action_cancel), onCancel) }
    }
}

@Composable
private fun ModeChoice(title: String, hint: String, icon: ImageVector, onClick: () -> Unit) {
    PillowSurface(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(icon, null, Modifier.size(22.dp), tint = OrdoTheme.colors.accent)
            Column {
                Text(title, style = OrdoTheme.type.itemTitle, color = OrdoTheme.colors.textPrimary)
                Text(hint, style = OrdoTheme.type.label, color = OrdoTheme.colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    PillowSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text(title.uppercase(), style = OrdoTheme.type.overline, color = OrdoTheme.colors.textTertiary, modifier = Modifier.padding(bottom = 6.dp))
            content()
        }
    }
}

@Composable
private fun Entry(icon: ImageVector, title: String, hint: String?, onClick: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = OrdoTheme.colors.textPrimary)
        Column(Modifier.weight(1f)) {
            Text(title, style = OrdoTheme.type.itemTitle, color = OrdoTheme.colors.textPrimary)
            if (hint != null) Text(hint, style = OrdoTheme.type.label, color = OrdoTheme.colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
        }
        trailing()
    }
}
