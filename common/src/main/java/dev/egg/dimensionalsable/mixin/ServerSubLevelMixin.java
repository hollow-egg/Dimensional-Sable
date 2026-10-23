package dev.egg.dimensionalsable.mixin;

import dev.egg.dimensionalsable.utils.ServerSubLevelOccupancy;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerSubLevel.class)
public class ServerSubLevelMixin implements ServerSubLevelOccupancy {

    @Unique
    private boolean dimensionalsable$isDimensionalRemove = false;

    @Override
    public boolean dimensionalsable$isDimensionalRemove() {
        return dimensionalsable$isDimensionalRemove;
    }

    @Override
    public void dimensionalsable$markDimensionalRemove() {
        dimensionalsable$isDimensionalRemove = true;
    }
}
