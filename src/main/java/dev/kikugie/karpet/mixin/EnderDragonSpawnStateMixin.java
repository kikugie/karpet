package dev.kikugie.karpet.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.kikugie.karpet.KarpetSettings;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collections;

@Mixin(targets = "net/minecraft/entity/boss/dragon/EnderDragonSpawnState$3")
public class EnderDragonSpawnStateMixin {
    @ModifyExpressionValue(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;iterate(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/BlockPos;)Ljava/lang/Iterable;"))
    private Iterable<BlockPos> educateEndPillarToBePoliteWithOtherBlocksHighSchoolEdition(Iterable<BlockPos> original) {
        return KarpetSettings.noEndPillarFill ? Collections.emptyList() : original;
    }
}