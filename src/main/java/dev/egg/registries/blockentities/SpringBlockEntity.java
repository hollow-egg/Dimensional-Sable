package dev.egg.registries.blockentities;

import dev.egg.DimensionalSable;
import dev.egg.registries.BlockEntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.UUID;

public class SpringBlockEntity extends BlockEntityAccessor{
    @Override
    public CompoundTag modifyNBT(final CompoundTag tag, final HashMap<UUID, UUID> oldToNewSubLevelIDMap, final Vec3i offset) {

        if (tag.hasUUID("GoalSubLevel"))
            tag.putUUID("GoalSubLevel", oldToNewSubLevelIDMap.get(tag.getUUID("GoalSubLevel")));
        if (tag.contains("Goal"))
        {
            BlockPos pos = BlockPos.of(tag.getLong("Goal"));
            DimensionalSable.LOGGER.info(pos.getX() + " " + pos.getY() + " " + pos.getZ());
            pos = pos.offset(offset);
            DimensionalSable.LOGGER.info(pos.getX() + " " + pos.getY() + " " + pos.getZ());
            tag.putLong("Goal", pos.asLong());
        }

        return tag;
    }
}
