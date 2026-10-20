package dev.egg.dimensionalsable.registries;

import net.minecraft.nbt.CompoundTag;

public class BlockEntityAccessor {
    public CompoundTag modifyNBT(final BlockEntityRegistry.BlockEntityInfo info) {
        return info.tag();
    }
}
