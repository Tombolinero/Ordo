package io.github.tombolinero.ordo.core.backup

import kotlinx.serialization.json.JsonObject

/**
 * Upgrades the raw JSON of older backup formats step by step to [CURRENT_FORMAT_VERSION]:
 * v1 → v2 → … → current. Each step only knows its two neighbouring versions.
 *
 * To introduce format v2:
 *  1. bump [CURRENT_FORMAT_VERSION] to 2,
 *  2. add `1 to { data -> … }` to [steps], turning a v1 data.json into a v2 one,
 *  3. keep the golden file backup-v1.ordobackup in the tests and add backup-v2.ordobackup.
 */
object BackupMigrations {
    const val CURRENT_FORMAT_VERSION = 1

    /** Step from version `key` to `key + 1`. */
    private val steps: Map<Int, (JsonObject) -> JsonObject> = mapOf()

    fun upgrade(data: JsonObject, fromVersion: Int): JsonObject {
        require(fromVersion >= 1)
        if (fromVersion > CURRENT_FORMAT_VERSION) throw BackupException.NewerFormat(fromVersion)
        var json = data
        for (v in fromVersion until CURRENT_FORMAT_VERSION) {
            val step = steps[v] ?: error("Missing backup migration $v → ${v + 1}")
            json = step(json)
        }
        return json
    }

    /** For tests: all steps from 1 to current exist. */
    internal fun isComplete(): Boolean = (1 until CURRENT_FORMAT_VERSION).all { it in steps }
}
