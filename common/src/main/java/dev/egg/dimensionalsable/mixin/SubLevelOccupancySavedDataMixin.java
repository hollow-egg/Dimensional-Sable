package dev.egg.dimensionalsable.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.egg.dimensionalsable.SharedOccupancyContainer;
import dev.ryanhcode.sable.sublevel.storage.SubLevelOccupancySavedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.BitSet;

@Mixin(SubLevelOccupancySavedData.class)
public class SubLevelOccupancySavedDataMixin {
    @Inject(method = "load", at = @At(value = "INVOKE", target = "Ljava/util/BitSet;or(Ljava/util/BitSet;)V"))
    private static void dimensionalsable$loadOccupancy(ServerLevel level, CompoundTag tag, CallbackInfoReturnable<SubLevelOccupancySavedData> cir, @Local(name = "occupancyData") BitSet occupancyData) {
        SharedOccupancyContainer.INSTANCE.getOccupancy().or(occupancyData);
    }
}
