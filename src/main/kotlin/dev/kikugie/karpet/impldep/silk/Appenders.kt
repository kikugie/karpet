package dev.kikugie.karpet.impldep.silk

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.ArgumentCommandNode
import com.mojang.brigadier.tree.LiteralCommandNode
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.command.CommandSource
import net.minecraft.command.PermissionLevelSource
import kotlin.collections.forEach

inline fun <reified T : Any> CommandContext<*>.get(name: String): T =
    getArgument(name, T::class.java)

inline fun <S> ArgumentBuilder<S, *>.literal(
    name: String,
    access: CommandRegistryAccess,
    builder: LiteralCommandBuilder<S>.() -> Unit
) where S : CommandSource, S : PermissionLevelSource = apply {
    LiteralCommandBuilder<S>(name).apply(builder).toBrigadier(access).forEach<LiteralCommandNode<S>>(::then)
}

inline fun <S, T> ArgumentBuilder<S, *>.argument(
    name: String,
    noinline type: (CommandRegistryAccess) -> ArgumentType<T>,
    access: CommandRegistryAccess,
    builder: ArgumentCommandBuilder<S, T>.() -> Unit
) where S : CommandSource, S : PermissionLevelSource = apply {
    ArgumentCommandBuilder<S, T>(name, type).apply(builder).toBrigadier(access).forEach<ArgumentCommandNode<S, T>>(::then)
}