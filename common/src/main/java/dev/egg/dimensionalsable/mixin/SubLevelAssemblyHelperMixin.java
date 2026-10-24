package dev.egg.dimensionalsable.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SubLevelAssemblyHelper.class)
public class SubLevelAssemblyHelperMixin {
    @WrapOperation(method = "moveBlocks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BlockEntity;saveWithFullMetadata(Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/nbt/CompoundTag;"))
    private static CompoundTag dimensionalsable$transformBlockEntity(BlockEntity instance, HolderLookup.Provider registries, Operation<CompoundTag> original) {
        CompoundTag tag = original.call(instance, registries);

        return tag;
    }
}
