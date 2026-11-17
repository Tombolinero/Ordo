package io.github.tombolinero.ordo.core.backup

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream

class BackupArchiveTest {

    private fun read(bytes: ByteArray, appCode: Int = 10000): Pair<BackupContent, Map<String, ByteArray>> {
        val images = HashMap<String, ByteArray>()
        val content = BackupArchive.read(ByteArrayInputStream(bytes), appCode) { name, input -> images[name] = input.readBytes() }
        return content to images
    }

    @Test fun `round trip keeps everything`() {
        val (content, images) = read(Fixtures.write())
        assertThat(content.data).isEqualTo(Fixtures.data)
        assertThat(images.keys).containsExactly("img-jesko.webp", "img-taycan.webp")
        assertThat(images["img-jesko.webp"]).isEqualTo(Fixtures.image1)
        assertThat(content.manifest.counts).isEqualTo(BackupManifest.Counts(lists = 2, items = 7, images = 2))
        assertThat(content.fromNewerApp).isFalse()
    }

    @Test fun `writing is reproducible`() {
        assertThat(Fixtures.write()).isEqualTo(Fixtures.write())
    }

    @Test fun `unreferenced images are not written`() {
        val data = Fixtures.data.copy(lists = Fixtures.data.lists.map { l -> l.copy(items = l.items.map { it.copy(image = null) }) })
        val (content, images) = read(Fixtures.write(data))
        assertThat(images).isEmpty()
        assertThat(content.manifest.counts.images).isEqualTo(0)
    }

    @Test fun `damaged image is detected`() {
        val manifest = Fixtures.manifest(files = mapOf("data.json" to BackupArchive.sha256("{}".toByteArray()), "images/a.webp" to BackupArchive.sha256("good".toByteArray())))
        val zip = Fixtures.zip("data.json" to "{}".toByteArray(), "images/a.webp" to "evil".toByteArray(), "manifest.json" to manifest)
        assertThrows(BackupException.Corrupted::class.java) { read(zip) }
    }

    @Test fun `missing file listed in manifest is detected`() {
        val manifest = Fixtures.manifest(files = mapOf("data.json" to BackupArchive.sha256("{}".toByteArray()), "images/a.webp" to "00"))
        val zip = Fixtures.zip("data.json" to "{}".toByteArray(), "manifest.json" to manifest)
        assertThrows(BackupException.Corrupted::class.java) { read(zip) }
    }

    @Test fun `random file is not a backup`() {
        assertThrows(BackupException::class.java) { read("hello, this is a holiday photo".toByteArray()) }
        assertThrows(BackupException.NotABackup::class.java) { read(Fixtures.zip("readme.txt" to "hi".toByteArray())) }
    }

    @Test fun `other format id is rejected`() {
        val zip = Fixtures.zip("manifest.json" to """{"format":"something-else","formatVersion":1}""".toByteArray())
        assertThrows(BackupException.NotABackup::class.java) { read(zip) }
    }

    @Test fun `newer incompatible format asks for an app update`() {
        val zip = Fixtures.zip("data.json" to "{}".toByteArray(), "manifest.json" to Fixtures.manifest(formatVersion = 99, files = emptyMap()))
        val e = assertThrows(BackupException.NewerFormat::class.java) { read(zip) }
        assertThat(e.formatVersion).isEqualTo(99)
    }

    @Test fun `path traversal in image names is rejected`() {
        val zip = Fixtures.zip("data.json" to "{}".toByteArray(), "images/../../evil.so" to "x".toByteArray(), "manifest.json" to Fixtures.manifest(files = emptyMap()))
        assertThrows(BackupException.Corrupted::class.java) { read(zip) }
    }

    @Test fun `fields and files from newer app versions are ignored`() {
        // A future version added "rating" to entries, "sections" to lists and an extra folder.
        val data = """{"lists":[{"id":"l","title":"T","sections":[{"name":"Top"}],"items":[{"id":"i","title":"A","rating":87}]}],"newThing":{"a":1}}""".toByteArray()
        val zip = Fixtures.zip(
            "data.json" to data,
            "extras/history.json" to "[]".toByteArray(),
            "manifest.json" to Fixtures.manifest(files = mapOf("data.json" to BackupArchive.sha256(data)), appCode = 20500),
        )
        val (content, _) = read(zip)
        assertThat(content.data.lists.single().items.single().title).isEqualTo("A")
        assertThat(content.fromNewerApp).isTrue()
    }

    @Test fun `missing optional fields get their defaults`() {
        val data = """{"lists":[{"id":"l","title":"T","items":[{"id":"i","title":"A"}]}]}""".toByteArray()
        val zip = Fixtures.zip("data.json" to data, "manifest.json" to Fixtures.manifest(files = mapOf("data.json" to BackupArchive.sha256(data))))
        val item = read(zip).first.data.lists.single().items.single()
        assertThat(item.note).isEmpty()
        assertThat(item.place).isNull()
        assertThat(read(zip).first.data.lists.single().color).isEqualTo("graphite")
    }

    @Test fun `migration chain is complete`() {
        assertThat(BackupMigrations.isComplete()).isTrue()
    }
}
