package io.github.tombolinero.ordo.core.backup

import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Golden files: one real backup file per format version ever released, committed under
 * src/test/resources/golden. Every future app version must still read all of them exactly.
 *
 * Record a new golden (only when introducing a new format version):
 *   ORDO_RECORD_GOLDEN=1 ./gradlew :core:backup:test --tests '*GoldenBackupTest*'
 */
class GoldenBackupTest {

    private fun golden(version: Int): ByteArray? =
        javaClass.getResourceAsStream("/golden/backup-v$version.${BackupArchive.FILE_EXTENSION}")?.use { it.readBytes() }

    @Test fun `a golden file exists for every format version`() {
        for (v in 1..BackupMigrations.CURRENT_FORMAT_VERSION) {
            assertThat(golden(v)).isNotNull()
        }
    }

    @Test fun `golden v1 still reads completely`() {
        val bytes = golden(1) ?: error("golden v1 missing")
        val images = HashMap<String, ByteArray>()
        val content = BackupArchive.read(bytes.inputStream(), currentAppVersionCode = Int.MAX_VALUE) { n, s -> images[n] = s.readBytes() }
        assertThat(content.manifest.formatVersion).isEqualTo(1)
        assertThat(content.data).isEqualTo(Fixtures.data)
        assertThat(images["img-taycan.webp"]).isEqualTo(Fixtures.image2)
    }

    @Test fun `record golden for the current format`() {
        assumeTrue(System.getenv("ORDO_RECORD_GOLDEN") == "1")
        val v = BackupMigrations.CURRENT_FORMAT_VERSION
        val file = File("src/test/resources/golden/backup-v$v.${BackupArchive.FILE_EXTENSION}")
        file.parentFile.mkdirs()
        file.writeBytes(Fixtures.write())
    }
}
