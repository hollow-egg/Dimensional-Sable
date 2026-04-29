package dev.egg.registries;

import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.UUID;

public class BlockEntityAccessor {
    public CompoundTag modifyNBT(final CompoundTag tag, final HashMap<UUID, UUID> oldToNewSubLevelIDMap, final Vec3i offset) {
        return tag;
    }
}
