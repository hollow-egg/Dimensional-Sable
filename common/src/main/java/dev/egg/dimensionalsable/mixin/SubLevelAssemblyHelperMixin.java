package dev.egg.dimensionalsable.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.egg.dimensionalsable.utils.BlockEntityAssemblyTransform;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SubLevelAssemblyHelper.class)
public class SubLevelAssemblyHelperMixin {

    @Inject(method = "moveBlocks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BlockEntity;saveWithFullMetadata(Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/nbt/CompoundTag;", shift = At.Shift.AFTER))
    private static void dimensionalsable$transformBlockEntity(ServerLevel level, SubLevelAssemblyHelper.AssemblyTransform transform, Iterable<BlockPos> blocks, CallbackInfo ci, @Local(name = "tag") CompoundTag tag) {
        if (transform instanceof BlockEntityAssemblyTransform blockEntityAssemblyTransform) {
            blockEntityAssemblyTransform.transformer.transformBlockEntityTag(tag);
        }
    }
}
