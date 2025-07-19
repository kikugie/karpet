package dev.kikugie.karpet.mixin.carpet;

import carpet.commands.PlayerCommand;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.kikugie.karpet.command.LinkPlayerCommandKt;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(value = PlayerCommand.class, remap = false)
public abstract class CarpetPlayerCommandMixin {
    @ModifyReturnValue(method = "getPlayerSuggestions", at = @At("RETURN"))
    private static Collection<String> listAllowedPlayers(Collection<String> original, @Local(argsOnly = true) ServerCommandSource source) {
        return LinkPlayerCommandKt.listAllowedPlayers(source, original);
    }

    @Inject(method = "cantSpawn", at = @At("HEAD"), cancellable = true)
    private static void restrictPlayerSpawn(CommandContext<ServerCommandSource> context, CallbackInfoReturnable<Boolean> cir) {
        if (!LinkPlayerCommandKt.restrictPlayerSpawn(context)) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = "cantManipulate", at = @At("HEAD"), cancellable = true)
    private static void restrictPlayerManipulate(CommandContext<ServerCommandSource> context, CallbackInfoReturnable<Boolean> cir) {
        if (!LinkPlayerCommandKt.restrictPlayerManipulation(context)) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @ModifyExpressionValue(method = "register", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/builder/RequiredArgumentBuilder;then(Lcom/mojang/brigadier/builder/ArgumentBuilder;)Lcom/mojang/brigadier/builder/ArgumentBuilder;", ordinal = 1))
    private static ArgumentBuilder<ServerCommandSource, ?> linkAltAccount(ArgumentBuilder<ServerCommandSource, ?> original,
                                                                          @Local(argsOnly = true) CommandRegistryAccess commandBuildContext) {
        return LinkPlayerCommandKt.buildPlayerLinkCommand(original, commandBuildContext);
    }
}