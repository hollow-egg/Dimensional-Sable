package dev.egg.registries;

import com.ibm.icu.impl.Pair;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.UUID;

public class BlockEntityAccessor {
    public CompoundTag modifyNBT(final CompoundTag tag, final HashMap<UUID, Pair<UUID,Vec3i>> oldToNewSubLevelIDMap, final Pair<Vector3d,Vector3d> translation) {
        return tag;
    }
}
