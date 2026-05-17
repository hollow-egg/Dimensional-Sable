package dev.egg.registries;

import dev.egg.DimensionalSable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.Set;

public class PosBlockEntityAccessor extends BlockEntityAccessor {
    private final Set<String> positionTags;

    public PosBlockEntityAccessor(final Set<String> tags)
    {
        positionTags = tags;
    }
    @Override
    public CompoundTag modifyNBT(final BlockEntityRegistry.BlockEntityInfo info) {
        for (String tag : positionTags) {
            if (info.tag().contains(tag)) {
                var list = info.tag().getIntArray(tag);
                BlockPos pos = new BlockPos(list[0],list[1],list[2]);
                pos = pos.offset(info.moveInfo().oldToNewSubLevelMap().get(info.oldSubLevelID()).second());
                info.tag().putIntArray(tag, new int[]{pos.getX(),pos.getY(),pos.getZ()});
            }
        }
        return info.tag();
    }
}
