package io.github.tombolinero.ordo.core.model

/** Injectable time source, so tests can control "now". */
fun interface Clock {
    fun now(): Long

    companion object {
        val System = Clock { java.lang.System.currentTimeMillis() }
    }
}
