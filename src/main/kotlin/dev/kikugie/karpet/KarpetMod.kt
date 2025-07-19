package dev.kikugie.karpet

import carpet.CarpetExtension
import carpet.CarpetServer
import dev.kikugie.karpet.bot.LinkedBotManager
import net.fabricmc.api.ModInitializer
import net.minecraft.server.MinecraftServer
import net.minecraft.util.WorldSavePath
import org.slf4j.LoggerFactory

object KarpetMod : ModInitializer, CarpetExtension {
    @JvmField val LOGGER = LoggerFactory.getLogger("KarpetMod")
    @JvmStatic lateinit var playerLinks: LinkedBotManager
        private set

    override fun version() = Reference.MOD_VERSION

    override fun onInitialize() {
        CarpetServer.manageExtension(this)
    }

    override fun onGameStarted() {
        CarpetServer.settingsManager.parseSettingsClass(KarpetSettings::class.java)
    }

    override fun onServerLoadedWorlds(server: MinecraftServer) {
        playerLinks = server.getSavePath(WorldSavePath.ROOT)
            .resolve("related-accounts.json")
            .let(::LinkedBotManager)
    }
}