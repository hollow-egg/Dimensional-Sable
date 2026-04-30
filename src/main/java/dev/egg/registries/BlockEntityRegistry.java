package dev.egg.registries;

import com.ibm.icu.impl.Pair;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.UUID;

public class BlockEntityRegistry {
    private final static HashMap<String,BlockEntityAccessor> blockEntityMap = new HashMap<>();

    public static CompoundTag modifyNBT(final CompoundTag tag, final HashMap<UUID,Pair<UUID,Vec3i>> oldToNewSubLevelIDMap, final Pair<Vector3d,Vector3d> translation)
    {
        BlockEntityAccessor accessor = blockEntityMap.get(tag.getString("id"));
        if (accessor != null)
            return accessor.modifyNBT(tag, oldToNewSubLevelIDMap, translation);
        return tag;
    }

    public static void PublishBlockEntityAccessor(final String BlockEntityLocation, final BlockEntityAccessor blockEntityAccessor)
    {
        blockEntityMap.put(BlockEntityLocation,blockEntityAccessor);
    }
}
