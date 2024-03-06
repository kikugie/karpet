package dev.kikugie.karpet.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.kikugie.karpet.KarpetSettings;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ModifiableWorld;
import net.minecraft.world.gen.feature.EndSpikeFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EndSpikeFeature.class)
public class EndSpikeFeatureMixin {
    @WrapWithCondition(method = "generateSpike", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/gen/feature/EndSpikeFeature;setBlockState(Lnet/minecraft/world/ModifiableWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V", ordinal = 1))
    private boolean educateEndPillarToBePoliteWithOtherBlocks(EndSpikeFeature instance, ModifiableWorld world, BlockPos pos, BlockState state) {
        return !KarpetSettings.noEndPillarFill;
    }
}