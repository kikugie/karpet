/*
Sourced from https://github.com/SilkMC/silk/blob/main/silk-commands/src/main/kotlin/net/silkmc/silk/commands/internal/ArgumentTypeUtils.kt,
which is licensed under GPL v3.0.

Modifications:
- Changed package to 'dev.kikugie.karpet.impldep.silk'.
- Updated Minecraft references to match Yarn mappings.
 */

package dev.kikugie.karpet.impldep.silk

import com.mojang.brigadier.arguments.*
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.command.argument.*
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.particle.ParticleEffect
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import java.util.*

object ArgumentTypeUtils {

    /**
     * Converts the given reified type [T] to an [ArgumentType].
     * Note that this function fails if there is no corresponding
     * [ArgumentType] mapped to [T].
     *
     * @param context the context which provides access to registries
     */
    inline fun <reified T> fromReifiedType(context: CommandRegistryAccess): ArgumentType<T> {
        val type = when (T::class) {
            Boolean::class -> BoolArgumentType.bool()
            Int::class -> IntegerArgumentType.integer()
            Long::class -> LongArgumentType.longArg()
            Float::class -> FloatArgumentType.floatArg()
            Double::class -> DoubleArgumentType.doubleArg()
            String::class -> StringArgumentType.string()

            UUID::class -> UuidArgumentType.uuid()

            CoordinateArgument::class -> BlockPosArgumentType.blockPos()
            BlockStateArgument::class -> BlockStateArgumentType.blockState(context)
            Formatting::class -> ColorArgumentType.color()
            Identifier::class -> IdentifierArgumentType.identifier()
            ItemStackArgument::class -> ItemStackArgumentType.itemStack(context)
            NbtCompound::class -> NbtCompoundArgumentType.nbtCompound()
            NbtElement::class -> NbtElementArgumentType.nbtElement()
            ParticleEffect::class -> ParticleEffectArgumentType.particleEffect(context)
            Text::class -> TextArgumentType.text(context)
            GameProfileArgumentType.GameProfileArgument::class -> GameProfileArgumentType.gameProfile()

            // see ResourceArgument
            // Enchantment::class -> ItemEnchantmentArgument.enchantment()
            // MobEffect::class -> MobEffectArgument.effect()

            else -> throw IllegalArgumentException("The specified type '${T::class.qualifiedName}' does not have corresponding default argument type")
        }

        @Suppress("UNCHECKED_CAST")
        return type as ArgumentType<T>
    }
}