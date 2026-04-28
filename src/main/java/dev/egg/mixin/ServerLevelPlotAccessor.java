package dev.egg.mixin;

import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

//Copyright (c) 2025 KyanBirb (sable touys)
@Mixin(ServerLevelPlot.class)
public interface ServerLevelPlotAccessor {

    @Accessor(value = "lightEngine", remap = false)
    LevelLightEngine dimensionalsable$getLightEngine();

    @Invoker(value = "newNonLitChunk", remap = false)
    void dimensionalsable$invokeNewNonLitChunk(final ChunkPos pos);

    @Invoker(value = "logLoadingErrors", remap = false)
    static void dimensionalsable$invokeLogLoadingErrors(final ChunkPos chunkPos, final int y, final String errorText) {}

    @Invoker(value = "lightChunk", remap = false)
    void dimensionalsable$invokeLightChunk(final LevelChunk chunk);
}
