package dev.egg.dimensionalsable.registries.blockentities.create;

import dev.egg.dimensionalsable.registries.BlockEntityAccessor;
import dev.egg.dimensionalsable.registries.BlockEntityRegistry;
import net.minecraft.nbt.CompoundTag;

public class MechanicalPistonBlockEntity extends BlockEntityAccessor {
    @Override
    public CompoundTag modifyNBT(final BlockEntityRegistry.BlockEntityInfo info) {

        if (info.tag().contains("Running"))
            info.tag().putBoolean("Running", false);
        if (info.tag().contains("Offset"))
            info.tag().putFloat("Offset", 0);

        return info.tag();
    }
}
