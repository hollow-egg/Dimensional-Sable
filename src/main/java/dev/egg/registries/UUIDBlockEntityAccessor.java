package dev.egg.registries;

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
                    UUID goalSubLevel = newSubLevel.first();
                    info.tag().putUUID(tag, goalSubLevel);
                }
            }
        }
        return info.tag();
    }
}
