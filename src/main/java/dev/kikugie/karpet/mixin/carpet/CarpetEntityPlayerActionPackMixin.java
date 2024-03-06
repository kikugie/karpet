package dev.kikugie.karpet.mixin.carpet;

import carpet.helpers.EntityPlayerActionPack;
import dev.kikugie.karpet.KarpetSettings;
import dev.kikugie.karpet.util.TeamUtil;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(value = EntityPlayerActionPack.class, remap = false)
public class CarpetEntityPlayerActionPackMixin {
    @Shadow @Final private Map<EntityPlayerActionPack.ActionType, EntityPlayerActionPack.Action> actions;

    @Shadow @Final private ServerPlayerEntity player;

    @Inject(method = "start", at = @At("RETURN"))
    private void addTeam(EntityPlayerActionPack.ActionType type, EntityPlayerActionPack.Action action, CallbackInfoReturnable<EntityPlayerActionPack> cir) {
        if (!actions.isEmpty() && !KarpetSettings.botTeam.isEmpty())
            TeamUtil.addTeam(player, KarpetSettings.botTeam);
    }

    @Inject(method = "stopAll", at = @At("RETURN"))
    private void removeTeam(CallbackInfoReturnable<EntityPlayerActionPack> cir) {
        TeamUtil.addTeam(player, KarpetSettings.botTeam);
    }
}