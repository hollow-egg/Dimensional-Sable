package dev.egg.dimensionalsable.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.egg.dimensionalsable.SharedOccupancyContainer;
import dev.egg.dimensionalsable.utils.ServerSubLevelOccupancy;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.BitSet;
import java.util.UUID;

@Mixin(SubLevelContainer.class)
public class SubLevelContainerMixin {

    @WrapOperation(method = "getFirstEmptyPlot", at = @At(value = "INVOKE", target = "Ljava/util/BitSet;get(I)Z"))
    private boolean dimensionalsable$getFirstEmptyPlot(BitSet instance, int bitIndex, Operation<Boolean> original) {
        return original.call(instance, bitIndex) || original.call(SharedOccupancyContainer.INSTANCE.getOccupancy(), bitIndex);
    }

    @Inject(method = "allocateSubLevel", at = @At(value = "INVOKE", target = "Ljava/util/BitSet;set(I)V"))
    private void dimensionalsable$setOccupancy(UUID uuid, int x, int z, Pose3d pose, CallbackInfoReturnable<SubLevel> cir, @Local(name = "index") int index) {
        SharedOccupancyContainer.INSTANCE.getOccupancy().set(index);
    }

    @WrapOperation(method = "removeSubLevel(IILdev/ryanhcode/sable/sublevel/storage/SubLevelRemovalReason;)V", at = @At(value = "INVOKE", target = "Ljava/util/BitSet;clear(I)V"))
    private void dimensionalsable$clearOccupancy(BitSet instance, int bitIndex, Operation<Void> original, @Local(name = "subLevel") SubLevel subLevel) {
        if (subLevel instanceof ServerSubLevel serverSubLevel &&
                !((ServerSubLevelOccupancy)serverSubLevel).dimensionalsable$isDimensionalRemove()) {
            original.call(SharedOccupancyContainer.INSTANCE.getOccupancy(), bitIndex);
        }
        original.call(instance, bitIndex);
    }
}
