package dev.kikugie.karpet

import carpet.CarpetExtension
import carpet.CarpetServer
import carpet.patches.EntityPlayerMPFake
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.kikugie.karpet.util.TeamUtil.addTeam
import dev.kikugie.karpet.util.TeamUtil.removeTeam
import net.fabricmc.api.ModInitializer
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.WorldSavePath
import java.io.IOException
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.*

object KarpetMod : ModInitializer, CarpetExtension {
    private var relatedAccountsSave: Path = Path(".")
    private var relatedAccounts: MutableMap<String, MutableSet<String>> = mutableMapOf()
    private val gson = Gson()
    override fun version() = Reference.MOD_VERSION

    override fun onInitialize() {
        CarpetServer.manageExtension(this)
    }

    override fun onGameStarted() {
        CarpetServer.settingsManager.parseSettingsClass(KarpetSettings::class.java)
    }

    override fun onServerLoadedWorlds(server: MinecraftServer) {
        relatedAccountsSave = server.getSavePath(WorldSavePath.ROOT).resolve("related-accounts.json")
        if (relatedAccountsSave.exists()) try {
            val type = object : TypeToken<Map<String, MutableSet<String>>>() {}.type
            relatedAccounts = gson.fromJson(relatedAccountsSave.reader(), type)
        } catch (e: Exception) {
            Reference.LOGGER.error("Failed to load related account data", e)
        }
    }

    override fun onPlayerLoggedIn(player: ServerPlayerEntity) {
        if (KarpetSettings.botTeam != "OFF" && player is EntityPlayerMPFake)
            player.addTeam(KarpetSettings.botTeam)
        else
            player.removeTeam(KarpetSettings.botTeam)
    }

    @JvmStatic
    fun linkPlayers(p1: String, p2: String) {
        relatedAccounts.computeIfAbsent(p1) { mutableSetOf()}.add(p2)
        relatedAccounts.computeIfAbsent(p2) { mutableSetOf()}.add(p1)
        try {
            relatedAccountsSave.writeText(gson.toJson(relatedAccounts), options = arrayOf(StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE))
        } catch (e: IOException) {
            Reference.LOGGER.error("Failed to save related account data", e)
        }
    }

    @JvmStatic
    fun getAllowedTargets(player: String): Set<String> = buildSet {
        add(player)
        relatedAccounts[player]?.also { addAll(it) }
    }
}