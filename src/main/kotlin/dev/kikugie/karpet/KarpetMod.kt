package dev.kikugie.karpet

import carpet.CarpetExtension
import carpet.CarpetServer
import net.fabricmc.api.ModInitializer
import net.minecraft.server.MinecraftServer

object KarpetMod : ModInitializer, CarpetExtension {
    override fun version() = Reference.MOD_VERSION

    override fun onInitialize() {
        CarpetServer.manageExtension(this)
    }

    override fun onGameStarted() {
        CarpetServer.settingsManager.parseSettingsClass(KarpetSettings::class.java)
    }

    override fun onServerLoadedWorlds(server: MinecraftServer) {
        LinkedAccountManager.onServerLoadedWorlds(server)
    }
}