package dev.egg.registries;

import dev.egg.DimensionalSable;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.UUID;

public class BlockEntityRegistry {
    private final static HashMap<String,BlockEntityAccessor> blockEntityMap = new HashMap<>();

    public static CompoundTag modifyNBT(final CompoundTag tag, final HashMap<UUID, UUID> oldToNewSubLevelIDMap, final Vec3i offset)
    {
        BlockEntityAccessor accessor = blockEntityMap.get(tag.getString("id"));
        if (accessor != null)
            return accessor.modifyNBT(tag, oldToNewSubLevelIDMap, offset);
        return tag;
    }

    public static void PublishBlockEntityAccessor(final String BlockEntityLocation, final BlockEntityAccessor blockEntityAccessor)
    {
        blockEntityMap.put(BlockEntityLocation,blockEntityAccessor);
    }
}
