package dev.egg.dimensionalsable.utils;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;

public class BlockEntityAssemblyTransform extends SubLevelAssemblyHelper.AssemblyTransform {

    public final BlockEntityTransformer transformer;

    public BlockEntityAssemblyTransform(BlockPos anchorPos, BlockPos resultingAnchorPos, int angle, Rotation rotation, ServerLevel resultingLevel, BlockEntityTransformer transformer) {
        super(anchorPos, resultingAnchorPos, angle, rotation, resultingLevel);
        this.transformer = transformer;
    }
}
