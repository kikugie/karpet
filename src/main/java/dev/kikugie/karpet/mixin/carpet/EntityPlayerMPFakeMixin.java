package dev.kikugie.karpet.mixin.carpet;

import carpet.patches.EntityPlayerMPFake;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import dev.kikugie.karpet.mixin.authlib.GameProfileAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(value = EntityPlayerMPFake.class)
public class EntityPlayerMPFakeMixin {
    @ModifyExpressionValue(method = "createFake", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/UserCache;findByName(Ljava/lang/String;)Ljava/util/Optional;"))
    private static Optional<GameProfile> modifyFinalGP(Optional<GameProfile> original, @Local(argsOnly = true) String name) {
        original.ifPresent(gameProfile -> ((GameProfileAccessor) gameProfile).setName(name));
        return original;
    }
}