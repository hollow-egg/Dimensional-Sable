package dev.egg.registries;

import dev.egg.DimensionalSable;
import net.minecraft.nbt.CompoundTag;

import java.util.Set;
import java.util.UUID;

public class UUIDBlockEntityAccessor extends BlockEntityAccessor {
    private final Set<String> UUIDTags;

    public UUIDBlockEntityAccessor(final Set<String> tags)
    {
        UUIDTags = tags;
    }
    @Override
    public CompoundTag modifyNBT(final BlockEntityRegistry.BlockEntityInfo info) {
        for (String tag : UUIDTags) {
            if (info.tag().hasUUID(tag)) {
                var newSubLevel = info.moveInfo().oldToNewSubLevelMap().get(info.oldSubLevelID());
                if (newSubLevel != null) {
                    UUID goalSubLevel = newSubLevel.first;
                    info.tag().putUUID(tag, goalSubLevel);
                }
            }
            else
                DimensionalSable.LOGGER.warn("[UUIDBlockEntityAccessor] Invalid tag: " + tag);
        }
        return info.tag();
    }
}
