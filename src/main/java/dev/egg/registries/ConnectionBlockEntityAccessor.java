package dev.egg.registries;

import dev.egg.DimensionalSable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public class ConnectionBlockEntityAccessor extends BlockEntityAccessor {
    private final String subLevelTag;
    private final String positionTag;

    ConnectionBlockEntityAccessor(String subLevelTag, String positionTag) {
        this.subLevelTag = subLevelTag;
        this.positionTag = positionTag;
    }

    @Override
    public CompoundTag modifyNBT(final BlockEntityRegistry.BlockEntityInfo info) {
        if (info.tag().hasUUID(subLevelTag)) {
            var subLevel = info.tag().getUUID(subLevelTag);
            var newSubLevel = info.moveInfo().oldToNewSubLevelMap().get(subLevel);
            info.tag().putUUID(subLevelTag, newSubLevel.first());

            if (info.tag().contains(positionTag)) {
                var point = info.tag().getIntArray(positionTag);
                BlockPos pos = new BlockPos(point[0], point[1], point[2]);
                pos = pos.offset(newSubLevel.second());
                info.tag().putIntArray(positionTag, new int[]{pos.getX(), pos.getY(), pos.getZ()});
            }
        }
        return info.tag();
    }
}
