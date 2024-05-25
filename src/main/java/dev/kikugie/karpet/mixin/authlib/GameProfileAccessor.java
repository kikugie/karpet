package dev.kikugie.karpet.mixin.authlib;

import com.mojang.authlib.GameProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = GameProfile.class, remap = false)
public interface GameProfileAccessor {
    @Accessor @Mutable
    void setName(String name);
}
