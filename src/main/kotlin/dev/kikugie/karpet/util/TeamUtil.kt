package dev.kikugie.karpet.util

import net.minecraft.server.network.ServerPlayerEntity

object TeamUtil {
    @JvmStatic
    fun ServerPlayerEntity.addTeam(name: String) {
        val scoreboard = server.scoreboard
        val team = scoreboard.getTeam(name) ?: return
        scoreboard.addPlayerToTeam(entityName, team)
    }
    @JvmStatic
    fun ServerPlayerEntity.removeTeam(name: String) {
        val scoreboard = server.scoreboard
        val team = scoreboard.getTeam(name) ?: return
        scoreboard.removePlayerFromTeam(entityName, team)
    }
}