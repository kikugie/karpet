package dev.kikugie.karpet.command

import carpet.CarpetSettings
import carpet.patches.EntityPlayerMPFake
import carpet.utils.CommandHelper
import carpet.utils.Messenger
import carpet.utils.Messenger.CarpetFormatting
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import dev.kikugie.karpet.KarpetMod
import dev.kikugie.karpet.KarpetSettings
import dev.kikugie.karpet.impldep.silk.get
import dev.kikugie.karpet.impldep.silk.literal
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.server.command.ServerCommandSource

private typealias ServerCommandContext = CommandContext<ServerCommandSource>

private val ServerCommandSource.canUseCarpetCommand: Boolean
    get() = CommandHelper.canUseCommand(this, CarpetSettings.carpetCommandPermissionLevel)

private fun ServerCommandContext.message(text: String, vararg styles: CarpetFormatting) =
    Messenger.m(source, "${styles.joinToString("") { it.code.toString() }} $text")

private val String.linked: Set<String> get() = KarpetMod.playerLinks.query(this)
private fun String.link(target: String): Boolean = KarpetMod.playerLinks.link(this, target)
private fun String.unlink(target: String): Boolean = KarpetMod.playerLinks.unlink(this, target)

private val ServerCommandSource.bypassesBotRestriction: Boolean
    get() = !KarpetSettings.allowOnlyOwnedBots || !isExecutedByPlayer || canUseCarpetCommand

fun ServerCommandSource.listAllowedPlayers(defined: Collection<String>): Collection<String> = buildSet {
    if (!KarpetSettings.allowOnlyOwnedBots) return defined
    if (canUseCarpetCommand)
        this += defined
    if (isExecutedByPlayer) {
        this += name
        this += name.linked
    }
}

fun ServerCommandContext.restrictPlayerSpawn(): Boolean {
    if (!restrictPlayerManipulation()) return false
    if (source.bypassesBotRestriction || KarpetSettings.limitBots < 0) return true

    val manager = source.server.playerManager
    val bots = source.playerNames.count { manager.getPlayer(it) is EntityPlayerMPFake }
    return (bots < KarpetSettings.limitBots).also {
        if (it.not()) message("Reached bot limit of ${KarpetSettings.limitBots}", CarpetFormatting.RED)
    }
}

fun ServerCommandContext.restrictPlayerManipulation(): Boolean {
    if (source.bypassesBotRestriction) return true
    val me: String = source.name
    val bot: String = get("player")
    return (bot == me || bot in me.linked).also {
        if (it.not()) message("Player $bot is not owned", CarpetFormatting.RED)
    }
}

fun ArgumentBuilder<ServerCommandSource, *>.buildPlayerLinkCommand(access: CommandRegistryAccess) = apply {
    literal("link", access) {
        requires { it.canUseCarpetCommand }
        argument("alt", StringArgumentType.word()) { alt ->
            runs {
                val me = get<String>("player")
                val bot = alt()
                if (me.link(bot)) message("Linked $bot to $me", CarpetFormatting.GREEN)
                else message("$bot is already linked to $me", CarpetFormatting.RED)
            }
        }
    }

    literal("unlink", access) {
        requires { it.canUseCarpetCommand }
        argument("alt", StringArgumentType.word()) { alt ->
            suggestList { it.get<String>("player").linked }
            runs {
                val me: String = get("player")
                val bot: String = alt()
                if (me.unlink(bot)) message("Unlinked $bot from $me", CarpetFormatting.GREEN)
                else message("$bot is not linked to $me", CarpetFormatting.RED)
            }
        }
    }
}