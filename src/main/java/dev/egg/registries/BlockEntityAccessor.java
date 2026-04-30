package dev.egg.registries;

import com.ibm.icu.impl.Pair;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.UUID;

public class BlockEntityAccessor {
    public CompoundTag modifyNBT(final CompoundTag tag, final HashMap<UUID, Pair<UUID,Vec3i>> oldToNewSubLevelIDMap) {
        return tag;
    }
}
