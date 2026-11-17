package io.github.tombolinero.ordo.core.data

import android.content.Context
import io.github.tombolinero.ordo.core.database.OrdoDatabase
import io.github.tombolinero.ordo.core.model.Clock
import java.io.File

/** Wires the data layer together. Created once per process by the app. */
class DataGraph(
    context: Context,
    appVersionName: String,
    appVersionCode: Int,
    databaseName: String? = OrdoDatabase.NAME,
    clock: Clock = Clock.System,
) {
    private val app = context.applicationContext
    val database: OrdoDatabase = OrdoDatabase.create(app, databaseName)
    val images: ImageStore = ImageStore(app)
    val repository: OrdoRepository = OrdoRepository(database, images, clock)
    val backups: BackupService = BackupService(database, images, File(app.cacheDir, "backup"), appVersionName, appVersionCode, clock)
    val safetyBackupDir: File = File(app.filesDir, "safety-backups")
}
