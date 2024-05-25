package dev.kikugie.karpet

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.minecraft.server.MinecraftServer
import net.minecraft.util.WorldSavePath
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.exists
import kotlin.io.path.reader
import kotlin.io.path.writeText

object LinkedAccountManager {
    private val gson = Gson()
    private val relatedAccounts: MutableMap<String, MutableSet<String>> = mutableMapOf()
    private lateinit var relatedAccountsSave: Path

    private fun writeMap() {
        try {
            relatedAccountsSave.writeText(
                gson.toJson(relatedAccounts),
                StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.CREATE
            )
        } catch (e: IOException) {
            Reference.LOGGER.error("Failed to save related account data", e)
        }
    }

    fun onServerLoadedWorlds(server: MinecraftServer) {
        relatedAccountsSave = server.getSavePath(WorldSavePath.ROOT).resolve("related-accounts.json")
        if (relatedAccountsSave.exists()) try {
            val type = object : TypeToken<Map<String, MutableSet<String>>>() {}.type
            relatedAccounts.clear()
            relatedAccounts.putAll(gson.fromJson(relatedAccountsSave.reader(), type))
        } catch (e: Exception) {
            Reference.LOGGER.error("Failed to load related account data", e)
        }
    }

    @JvmStatic
    fun linkPlayers(p1: String, p2: String) {
        relatedAccounts.computeIfAbsent(p1) { mutableSetOf() }.add(p2)
        relatedAccounts.computeIfAbsent(p2) { mutableSetOf() }.add(p1)
        writeMap()
    }

    @JvmStatic
    fun unlinkPlayers(p1: String, p2: String) {
        relatedAccounts[p1]?.remove(p2)
        relatedAccounts[p2]?.remove(p1)
        writeMap()
    }


    @JvmStatic
    fun getAllowedTargets(player: String): Set<String> = buildSet {
        add(player)
        relatedAccounts[player]?.also { addAll(it) }
    }
}