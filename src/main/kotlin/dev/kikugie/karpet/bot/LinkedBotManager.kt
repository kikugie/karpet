package dev.kikugie.karpet.bot

import dev.kikugie.karpet.KarpetMod
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

@OptIn(ExperimentalSerializationApi::class)
class LinkedBotManager(private val path: Path) {
    @Serializable
    private val accounts: MutableMap<String, MutableSet<String>> = mutableMapOf()

    init {
        if (path.exists()) path.runCatching {
            inputStream().use { Json.decodeFromStream<Map<String, MutableSet<String>>>(it) }.let { accounts.putAll(it) }
        }.onFailure {
            KarpetMod.LOGGER.error("Failed to load account data", it)
        }
    }

    fun query(owner: String): Set<String> = accounts[owner].orEmpty()

    fun link(owner: String, target: String): Boolean =
        accounts.getOrPut(owner, ::mutableSetOf).add(target).also { save() }

    fun unlink(owner: String, target: String): Boolean {
        val set = accounts[owner] ?: return false
        return set.remove(target).also { if (set.isEmpty()) accounts -= owner; save() }
    }

    private fun save() {
        path.runCatching {
            outputStream(StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
                .use { Json.encodeToStream(accounts, it) }
        }.onFailure {
            KarpetMod.LOGGER.error("Failed to save account data", it)
        }
    }
}