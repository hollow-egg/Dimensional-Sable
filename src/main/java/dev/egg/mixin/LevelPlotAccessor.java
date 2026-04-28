package dev.egg.mixin;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//Copyright (c) 2025 KyanBirb (sable touys)
@Mixin(LevelPlot.class)
public interface LevelPlotAccessor {

    @Accessor(value = "container", remap = false)
    SubLevelContainer dimensionalsable$getContainer();

    @Accessor(value = "logSize", remap = false)
    int dimensionalsable$getLogSize();

    @Accessor(value = "biome", remap = false)
    ResourceKey<Biome> dimensionalsable$getBiome();

    @Accessor(value = "expandPlotIfNecessary", remap = false)
    boolean dimensionalsable$getExpandPlotIfNecessary();

    @Accessor(value = "expandPlotIfNecessary", remap = false)
    void dimensionalsable$setExpandPlotIfNecessary(boolean expandPlotIfNecessary);

}