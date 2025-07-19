/*
Sourced from https://github.com/SilkMC/silk/blob/main/silk-commands/src/main/kotlin/net/silkmc/silk/commands/CommandBuilder.kt,
which is licensed under GPL v3.0.

Modifications:
- Changed package to 'dev.kikugie.karpet.impldep.silk'.
- Updated Minecraft references to match Yarn mappings.
- Removed unused and deprecated functions.
 */

@file:Suppress("unused")

package dev.kikugie.karpet.impldep.silk

import com.mojang.brigadier.Message
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.mojang.brigadier.tree.ArgumentCommandNode
import com.mojang.brigadier.tree.CommandNode
import com.mojang.brigadier.tree.LiteralCommandNode
import dev.kikugie.karpet.KarpetMod
import dev.kikugie.karpet.impldep.silk.DslAnnotations.NodeLevel.RunsDsl
import dev.kikugie.karpet.impldep.silk.DslAnnotations.NodeLevel.SuggestsDsl
import dev.kikugie.karpet.impldep.silk.DslAnnotations.TopLevel.NodeDsl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.launch
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.command.CommandSource
import net.minecraft.command.PermissionLevelSource
import java.util.concurrent.CompletableFuture

/**
 * An argument resolver extracts the argument value out of the current [CommandContext].
 */
typealias ArgumentResolver<S, T> = CommandContext<S>.() -> T

/**
 * The simple argument builder is a variant of an [ArgumentCommandBuilder] lambda function
 * that supports [ArgumentResolver] (passed as `it`).
 */
typealias SimpleArgumentBuilder<Source, T> = ArgumentCommandBuilder<Source, T>.(argument: ArgumentResolver<Source, T>) -> Unit

typealias BrigadierBuilder<Builder> = Builder.(context: CommandRegistryAccess) -> Unit

@NodeDsl
abstract class CommandBuilder<Source, Builder, Node>
    where Source : CommandSource,
          Source : PermissionLevelSource,
          Builder : ArgumentBuilder<Source, Builder>,
          Node : CommandNode<Source> {
    @PublishedApi
    internal val children = ArrayList<CommandBuilder<Source, *, *>>()

    @PublishedApi
    internal val brigadierBuilders = ArrayList<BrigadierBuilder<Builder>>()

    /**
     * Adds execution logic to this command. The place where this function
     * is called matters, as this defines for which path in the command tree
     * this executor should be called.
     *
     * possible usage:
     * ```kt
     * command("mycommand") {
     *     // defining runs in the body:
     *     runs { }
     *
     *     // calling runs as an infix function directly after literal or argument:
     *     literal("subcommand") runs { }
     * }
     * ```
     *
     * Note that this function will always return 1 as the exit code.
     *
     * @see com.mojang.brigadier.builder.ArgumentBuilder.executes
     */
    @RunsDsl
    inline infix fun runs(crossinline block: CommandContext<Source>.() -> Unit) = apply {
        brigadierBuilders += {
            val previousCommand = this.command
            this.executes {
                val previousResult = previousCommand?.run(it)
                try {
                    block(it)
                    if (previousResult != null && previousResult < 0) -1 else 0
                } catch (exc: Throwable) {
                    KarpetMod.LOGGER.error("Command run failed", exc)
                    throw exc
                }
            }
        }
    }

    /**
     * Does the same as [runs] (see its docs for more information), but launches the command
     * logic in an async coroutine.
     *
     * @see runs
     */
    @RunsDsl
    inline infix fun runsAsync(crossinline block: suspend CommandContext<Source>.() -> Unit) = runs {
        KARPET_COROUTINE_SCOPE.launch {
            block(this@runs)
        }
    }

    /**
     * Adds a new subcommand / literal to this command.
     *
     * possible usage:
     * ```kt
     * command("mycommand") {
     *     literal("subcommand") {
     *         // the body of the subcommand
     *     }
     * }
     * ```
     *
     * @param name the name of the subcommand
     */
    @NodeDsl
    inline fun literal(name: String, builder: LiteralCommandBuilder<Source>.() -> Unit = {}) =
        LiteralCommandBuilder<Source>(name).apply(builder).also { children += it }

    /**
     * Adds a new argument to this command. This variant of the argument function allows you to specify
     * the [ArgumentType] in the classical Brigadier way.
     *
     * @param name the name of the argument - This will be displayed to the player, if there is enough room for the
     * tooltip.
     * @param type the type of the argument - There are predefined types like `StringArgumentType.string()` or
     * `IdentifierArgumentType.identifier()`. You can also pass a lambda, as [ArgumentType] is a functional
     * interface. For simple types, consider using the `inline reified` version of this function instead.
     */
    @NodeDsl
    inline fun <reified T> argument(
        name: String,
        type: ArgumentType<T>,
        builder: SimpleArgumentBuilder<Source, T> = {},
    ) = ArgumentCommandBuilder<Source, T>(name) { type }
        .apply { builder { getArgument(name, T::class.java) } }
        .also { children += it }

    /**
     * Adds a new argument to this command. This variant of the argument function allows you to pass and argument
     * which depends on the [CommandBuildContext].
     *
     * @param name the name of the argument - This will be displayed to the player, if there is enough room for the
     * tooltip.
     * @param typeProvider the provider for the [ArgumentType] - there are predefined types like
     * `BlockStateArgument.block(context)` and `ItemArgument.item(context)` or you can pass your own
     */
    @JvmName("argumentWithContextualType")
    @NodeDsl
    inline fun <reified T> argument(
        name: String,
        noinline typeProvider: (CommandRegistryAccess) -> ArgumentType<T>,
        builder: SimpleArgumentBuilder<Source, T> = {},
    ) = ArgumentCommandBuilder<Source, T>(name, typeProvider)
        .apply { builder { getArgument(name, T::class.java) } }
        .also { children += it }

    /**
     * Adds a new argument to this command. This variant of the argument function you to specifiy the
     * argument parse logic using a Kotlin lambda function ([parser]).
     *
     * @param name the name of the argument - This will be displayed to the player, if there is enough room for the
     * tooltip.
     * @param parser gives you a [StringReader], which allows you to parse the input of the user - you should return a
     * value of the given type [T], which will be the argument value
     */
    @JvmName("argumentWithCustomParser")
    @NodeDsl
    inline fun <reified T> argument(
        name: String,
        crossinline parser: (StringReader) -> T,
        builder: SimpleArgumentBuilder<Source, T> = {},
    ) = ArgumentCommandBuilder<Source, T>(name) { ArgumentType { parser(it) } }
        .apply { builder { getArgument(name, T::class.java) } }
        .also { children += it }

    /**
     * Adds a new argument to this command. The [ArgumentType] will be resolved using the reified
     * type [T]. For a list of supported types, have a look at [ArgumentTypeUtils.fromReifiedType], as it is
     * the function used by this builder function.
     *
     * @param name the name of the argument - This will be displayed to the player, if there is enough room for the
     * tooltip.
     */
    @NodeDsl
    inline fun <reified T> argument(name: String, builder: SimpleArgumentBuilder<Source, T> = {}) =
        ArgumentCommandBuilder<Source, T>(name) { ArgumentTypeUtils.fromReifiedType(it) }
            .apply { builder { getArgument(name, T::class.java) } }
            .also { children += it }

    /**
     * Specifies that the given predicate must return true for the [Source]
     * in order for it to be able to execute this part of the command tree. Use
     * this function on the root command node to secure the whole command.
     */
    @RunsDsl
    fun requires(predicate: (source: Source) -> Boolean) = this.also {
        brigadierBuilders += {
            this.requires(this.requirement.and(predicate))
        }
    }

    /**
     * Specifies that the given permission [level] is required to execute this part of the command tree.
     * A shortcut delegating to [requires].
     */
    @RunsDsl
    fun requiresPermissionLevel(level: Int) =
        requires { it.hasPermissionLevel(level) }

    /**
     * Specifies that the [PermissionLevel] given as [level] is required to execute this part of the command tree.
     * A shortcut delegating to [requires].
     */
    @RunsDsl
    fun requiresPermissionLevel(level: PermissionLevel) =
        requires { it.hasPermissionLevel(level.level) }

    /**
     * This function allows you to access the regular Brigadier builder. The type of
     * `this` in its context will equal the type of [Builder].
     */
    fun brigadier(block: (@NodeDsl Builder).(context: CommandRegistryAccess) -> Unit) = this.also {
        brigadierBuilders += block
    }

    protected abstract fun createBuilder(context: CommandRegistryAccess): Builder

    /**
     * Converts this Kotlin command builder abstraction to an [ArgumentBuilder] of Brigadier.
     */
    @PublishedApi
    internal open fun toBrigadier(context: CommandRegistryAccess): List<Node> {
        val builder = createBuilder(context)

        brigadierBuilders.forEach { it(builder, context) }

        children.forEach { child ->
            child.toBrigadier(context).forEach {
                builder.then(it)
            }
        }

        @Suppress("UNCHECKED_CAST")
        return listOf(builder.build() as Node)
    }
}

class LiteralCommandBuilder<Source>(
    private val name: String,
) : CommandBuilder<Source, LiteralArgumentBuilder<Source>, LiteralCommandNode<Source>>()
    where Source : CommandSource,
          Source : PermissionLevelSource {

    override fun createBuilder(context: CommandRegistryAccess): LiteralArgumentBuilder<Source> =
        LiteralArgumentBuilder.literal(name)

    private val aliases = mutableListOf<String>()

    /**
     * Adds an alias for this literal command node, which can be used
     * instead of the main [LiteralCommandBuilder.name].
     */
    @NodeDsl
    fun alias(vararg name: String) {
        aliases += name
    }

    @PublishedApi
    override fun toBrigadier(context: CommandRegistryAccess): List<LiteralCommandNode<Source>> {
        return super.toBrigadier(context).let { mainNodes ->
            if (aliases.isEmpty() || mainNodes.size != 1) mainNodes else {
                val mainNode = mainNodes.single()
                mainNodes + aliases.map { alias ->
                    if (mainNode.children.isNotEmpty() && mainNode.command == null) {
                        LiteralArgumentBuilder
                            .literal<Source>(alias)
                            .redirect(mainNode)
                            .build()
                    } else {
                        // we cannot use redirect here because of
                        // https://github.com/Mojang/brigadier/issues/46
                        // fix: create a new node here instead
                        LiteralCommandNode(
                            alias,
                            mainNode.command,
                            mainNode.requirement,
                            mainNode.redirect,
                            mainNode.redirectModifier,
                            mainNode.isFork
                        )
                            .apply {
                                for (child in mainNode.children) {
                                    addChild(child)
                                }
                            }
                    }
                }
            }
        }
    }
}

class ArgumentCommandBuilder<Source, T>(
    private val name: String,
    private val typeProvider: (CommandRegistryAccess) -> ArgumentType<T>,
) : CommandBuilder<Source, RequiredArgumentBuilder<Source, T>, ArgumentCommandNode<Source, T>>()
    where Source : CommandSource,
          Source : PermissionLevelSource {

    override fun createBuilder(context: CommandRegistryAccess): RequiredArgumentBuilder<Source, T> =
        RequiredArgumentBuilder.argument(name, typeProvider(context))

    @PublishedApi
    internal inline fun suggests(
        crossinline block: (context: CommandContext<Source>, builder: SuggestionsBuilder) -> CompletableFuture<Suggestions>,
    ): ArgumentCommandBuilder<Source, T> {
        brigadierBuilders += {
            this.suggests { context, builder ->
                block(context, builder)
            }
        }
        return this
    }

    /**
     * Suggest the value which is the result of the [suggestionBuilder].
     */
    @SuggestsDsl
    inline fun suggestSingle(crossinline suggestionBuilder: (CommandContext<Source>) -> Any?) =
        suggests { context, builder ->
            builder.applyAny(suggestionBuilder(context))
            builder.buildFuture()
        }

    /**
     * Suggest the value which is the result of the [suggestionBuilder].
     * Additionaly, a separate tooltip associated with the suggestion
     * will be shown as well.
     */
    @SuggestsDsl
    inline fun suggestSingleWithTooltip(crossinline suggestionBuilder: (CommandContext<Source>) -> Pair<Any, Message>?) =
        suggests { context, builder ->
            builder.applyAnyWithTooltip(suggestionBuilder(context))
            builder.buildFuture()
        }

    /**
     * Suggest the value which is the result of the [suggestionBuilder].
     *
     * @param coroutineScope the [CoroutineScope] where the suggestion should be built in - an async scope by default,
     * but you can change this to a synchronous scope using [net.silkmc.silk.core.task.mcCoroutineScope]
     */
    @SuggestsDsl
    inline fun suggestSingleSuspending(
        coroutineScope: CoroutineScope = KARPET_COROUTINE_SCOPE,
        crossinline suggestionBuilder: suspend (CommandContext<Source>) -> Any?,
    ) = suggests { context, builder ->
        coroutineScope.async {
            builder.applyAny(suggestionBuilder(context))
            builder.build()
        }.asCompletableFuture()
    }

    /**
     * Suggest the value which is the result of the [suggestionBuilder].
     * Additionaly, a separate tooltip associated with the suggestion
     * will be shown as well.
     *
     * @param coroutineScope the [CoroutineScope] where the suggestion should be built in - an async scope by default,
     * but you can change this to a synchronous scope using [net.silkmc.silk.core.task.mcCoroutineScope]
     */
    @SuggestsDsl
    inline fun suggestSingleWithTooltipSuspending(
        coroutineScope: CoroutineScope = KARPET_COROUTINE_SCOPE,
        crossinline suggestionBuilder: suspend (CommandContext<Source>) -> Pair<Any?, Message>?,
    ) = suggests { context, builder ->
        coroutineScope.async {
            builder.applyAnyWithTooltip(suggestionBuilder(context))
            builder.build()
        }.asCompletableFuture()
    }

    /**
     * Suggest the entries of the iterable which is the result of the
     * [suggestionsBuilder].
     */
    @SuggestsDsl
    inline fun suggestList(crossinline suggestionsBuilder: (CommandContext<Source>) -> Iterable<Any?>?) =
        suggests { context, builder ->
            builder.applyIterable(suggestionsBuilder(context))
            builder.buildFuture()
        }

    /**
     * Suggest the entries of the iterable which is the result of the
     * [suggestionsBuilder].
     * Additionaly, a separate tooltip associated with each suggestion
     * will be shown as well.
     */
    @SuggestsDsl
    inline fun suggestListWithTooltips(crossinline suggestionsBuilder: (CommandContext<Source>) -> Iterable<Pair<Any?, Message>?>?) =
        suggests { context, builder ->
            builder.applyIterableWithTooltips(suggestionsBuilder(context))
            builder.buildFuture()
        }

    /**
     * Suggest the entries of the iterable which is the result of the
     * [suggestionsBuilder].
     *
     * @param coroutineScope the [CoroutineScope] where the suggestions should be built in - an async scope by default,
     * but you can change this to a synchronous scope using [net.silkmc.silk.core.task.mcCoroutineScope]
     */
    @SuggestsDsl
    inline fun suggestListSuspending(
        coroutineScope: CoroutineScope = KARPET_COROUTINE_SCOPE,
        crossinline suggestionsBuilder: suspend (CommandContext<Source>) -> Iterable<Any?>?,
    ) = suggests { context, builder ->
        coroutineScope.async {
            builder.applyIterable(suggestionsBuilder(context))
            builder.build()
        }.asCompletableFuture()
    }

    /**
     * Suggest the entries of the iterable which is the result of the
     * [suggestionsBuilder].
     * Additionaly, a separate tooltip associated with each suggestion
     * will be shown as well.
     *
     * @param coroutineScope the [CoroutineScope] where the suggestions should be built in - an async scope by default,
     * but you can change this to a synchronous scope using [net.silkmc.silk.core.task.mcCoroutineScope]
     */
    @SuggestsDsl
    inline fun suggestListWithTooltipsSuspending(
        coroutineScope: CoroutineScope = KARPET_COROUTINE_SCOPE,
        crossinline suggestionsBuilder: (CommandContext<Source>) -> Iterable<Pair<Any?, Message>?>?,
    ) = suggests { context, builder ->
        coroutineScope.async {
            builder.applyIterableWithTooltips(suggestionsBuilder(context))
            builder.build()
        }.asCompletableFuture()
    }

    @PublishedApi
    internal fun SuggestionsBuilder.applyAny(any: Any?) {
        when (any) {
            is Int -> suggest(any)
            is String -> suggest(any)
            else -> suggest(any.toString())
        }
    }


    @PublishedApi
    internal fun SuggestionsBuilder.applyAnyWithTooltip(pair: Pair<Any?, Message>?) {
        if (pair == null) return
        val (any, message) = pair
        when (any) {
            is Int -> suggest(any, message)
            is String -> suggest(any, message)
            else -> suggest(any.toString(), message)
        }
    }

    @PublishedApi
    internal fun SuggestionsBuilder.applyIterable(iterable: Iterable<Any?>?) =
        iterable?.forEach { applyAny(it) }

    @PublishedApi
    internal fun SuggestionsBuilder.applyIterableWithTooltips(iterable: Iterable<Pair<Any?, Message>?>?) =
        iterable?.forEach { applyAnyWithTooltip(it) }
}

private class DslAnnotations {
    class TopLevel {
        @Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.CLASS)
        @DslMarker
        annotation class NodeDsl
    }

    class NodeLevel {
        @DslMarker
        annotation class RunsDsl

        @DslMarker
        annotation class SuggestsDsl
    }
}