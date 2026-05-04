package dev.egg.registries.blockentities.create;

import dev.egg.registries.BlockEntityAccessor;
import dev.egg.registries.BlockEntityRegistry;
import net.minecraft.nbt.CompoundTag;

public class BearingBlockEntity extends BlockEntityAccessor {
    @Override
    public CompoundTag modifyNBT(final BlockEntityRegistry.BlockEntityInfo info) {

        if (info.tag().contains("Running"))
            info.tag().putBoolean("Running", false);
        if (info.tag().contains("Angle"))
            info.tag().putFloat("Angle", 0);

        return info.tag();
    }
}
