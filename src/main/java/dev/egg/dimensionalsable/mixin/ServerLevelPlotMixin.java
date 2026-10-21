package dev.egg.dimensionalsable.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.egg.dimensionalsable.utils.BlockEntityTransformer;
import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevelPlot.class)
public class ServerLevelPlotMixin {

    @Unique
    BlockEntityTransformer dimensionalsable$blockEntityTransformer = null;

    @Inject(method = "load", at = @At("HEAD"))
    private void dimensionalsable$getTransformer(CompoundTag tag, CallbackInfo ci) {
        dimensionalsable$blockEntityTransformer = BlockEntityTransformer.getTransform(tag, ((ServerLevelPlot)(Object)this).getSubLevel().getLevel().getServer());
    }

    // im injecting into the loading instead of changing it ahead of time so that we can avoid looping through this potentially massive list twice
    @WrapOperation(method = "load", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BlockEntity;loadStatic(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    private BlockEntity dimensionalsable$modifyBlockEntities(BlockPos pos, BlockState state, CompoundTag tag, HolderLookup.Provider registries, Operation<BlockEntity> original) {
        if (dimensionalsable$blockEntityTransformer != null) {
            dimensionalsable$blockEntityTransformer.transformBlockEntityTag(tag);
        }
        return original.call(pos, state, tag, registries);
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void dimensionalsable$removeTransform(CompoundTag tag, CallbackInfo ci) {
        dimensionalsable$blockEntityTransformer = null;
    }

    @Unique
    int dimensionalsable$sectionOffset = 0;

    @Inject(method = "save", at = @At("RETURN"))
    private void dimensionalsable$saveMinSectionY(CallbackInfoReturnable<CompoundTag> cir) {
        cir.getReturnValue().putInt("min_section_y", ((ServerLevelPlot)(Object)this).getSubLevel().getLevel().getMinSection());
    }

    @Inject(method = "load", at = @At("HEAD"))
    private void dimensionalsable$getSavedSectionOffset(CompoundTag tag, CallbackInfo ci) {
        ServerLevel level = ((ServerLevelPlot)(Object)this).getSubLevel().getLevel();
        final int savedMinSectionY = tag.contains("min_section_y") ? tag.getInt("min_section_y") : level.getMinSection();
        dimensionalsable$sectionOffset = savedMinSectionY - level.getMinSection();
    }

    @WrapOperation(method = "load", at = @At(value = "INVOKE", target = "Ljava/lang/Integer;parseInt(Ljava/lang/String;)I"))
    private int dimensionalsable$modifyLoadedSections(String s, Operation<Integer> original) {
        return original.call(s) + dimensionalsable$sectionOffset;
    }
}
