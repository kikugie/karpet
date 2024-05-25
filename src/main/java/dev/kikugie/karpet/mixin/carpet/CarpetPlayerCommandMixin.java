package dev.kikugie.karpet.mixin.carpet;

import carpet.CarpetSettings;
import carpet.commands.PlayerCommand;
import carpet.patches.EntityPlayerMPFake;
import carpet.utils.CommandHelper;
import carpet.utils.Messenger;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.kikugie.karpet.KarpetMod;
import dev.kikugie.karpet.KarpetSettings;
import dev.kikugie.karpet.LinkedAccountManager;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.Objects;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

@Mixin(value = PlayerCommand.class, remap = false)
public abstract class CarpetPlayerCommandMixin {
    @Shadow
    private static Collection<String> getPlayerSuggestions(ServerCommandSource source) {
        return null;
    }

    @Unique
    private static boolean allowOriginal(ServerCommandSource source) {
        return !KarpetSettings.allowOnlyOwnedBots
                || !source.isExecutedByPlayer()
                || CommandHelper.canUseCommand(source, CarpetSettings.carpetCommandPermissionLevel);
    }

    @Inject(method = "getPlayerSuggestions", at = @At("HEAD"), cancellable = true)
    private static void listAllowedPlayers(ServerCommandSource source, CallbackInfoReturnable<Collection<String>> cir) {
        if (!allowOriginal(source)) {
            cir.setReturnValue(LinkedAccountManager.getAllowedTargets(source.getName()));
            cir.cancel();
        }
    }

    @Inject(method = "cantSpawn", at = @At("HEAD"), cancellable = true)
    private static void restrictPlayerSpawn(CommandContext<ServerCommandSource> context, CallbackInfoReturnable<Boolean> cir) {
        if (allowOriginal(context.getSource())) return;
        int bots = 0;
        for (String name : context.getSource().getPlayerNames()) {
            var player = context.getSource().getServer().getPlayerManager().getPlayer(name);
            if (player instanceof EntityPlayerMPFake) bots++;
        }

        if (KarpetSettings.limitBots != -1 && KarpetSettings.limitBots <= bots) {
            Messenger.m(context.getSource(), "r Reached bot limit");
            cir.setReturnValue(true);
            cir.cancel();
            return;
        }

        var playerName = StringArgumentType.getString(context, "player");
        if (!LinkedAccountManager.getAllowedTargets(context.getSource().getName()).contains(playerName)) {
            Messenger.m(context.getSource(), "r Player %s is not owned".formatted(playerName));
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = "cantManipulate", at = @At("HEAD"), cancellable = true)
    private static void restrictPlayerManipulate(CommandContext<ServerCommandSource> context, CallbackInfoReturnable<Boolean> cir) {
        var playerName = StringArgumentType.getString(context, "player");
        if (allowOriginal(context.getSource())) return;
        if (!LinkedAccountManager.getAllowedTargets(context.getSource().getName()).contains(playerName)) {
            Messenger.m(context.getSource(), "r Player %s is not owned".formatted(playerName));
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @ModifyExpressionValue(method = "register", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/builder/RequiredArgumentBuilder;then(Lcom/mojang/brigadier/builder/ArgumentBuilder;)Lcom/mojang/brigadier/builder/ArgumentBuilder;", ordinal = 1), remap = false)
    private static ArgumentBuilder<ServerCommandSource, ?> linkAltAccount(ArgumentBuilder<ServerCommandSource, ?> original) {
        return original
                .then(literal("link")
                        .requires(source -> CommandHelper.canUseCommand(source, CarpetSettings.carpetCommandPermissionLevel))
                        .then(argument("alt", StringArgumentType.word())
                                .suggests((c, b) -> CommandSource.suggestMatching(Objects.requireNonNull(getPlayerSuggestions(c.getSource())), b))
                                .executes(context -> {
                                    var p1 = StringArgumentType.getString(context, "player");
                                    var p2 = StringArgumentType.getString(context, "alt");
                                    LinkedAccountManager.linkPlayers(p1, p2);
                                    Messenger.m(context.getSource(), "w Linked %s - %s".formatted(p1, p2));
                                    return 1;
                                })))
                .then(literal("unlink")
                        .requires(source -> CommandHelper.canUseCommand(source, CarpetSettings.carpetCommandPermissionLevel))
                        .then(argument("alt", StringArgumentType.word())
                                .suggests((c, b) -> CommandSource.suggestMatching(LinkedAccountManager.getAllowedTargets(StringArgumentType.getString(c, "player")), b))
                                .executes(context -> {
                                    var p1 = StringArgumentType.getString(context, "player");
                                    var p2 = StringArgumentType.getString(context, "alt");
                                    LinkedAccountManager.unlinkPlayers(p1, p2);
                                    Messenger.m(context.getSource(), "w Unlinked %s - %s".formatted(p1, p2));
                                    return 1;
                                })));
    }
}