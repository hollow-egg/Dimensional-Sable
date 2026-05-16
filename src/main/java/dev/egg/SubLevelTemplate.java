package dev.egg;

import com.mojang.serialization.Codec;
import dev.egg.mixin.LevelPlotAccessor;
import dev.egg.mixin.ServerLevelPlotAccessor;
import dev.egg.registries.BlockEntityRegistry;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.mixinterface.plot.serialization.LevelChunkTicksExtension;
import dev.ryanhcode.sable.platform.SablePlotPlatform;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import dev.ryanhcode.sable.sublevel.plot.SubLevelPlayerChunkSender;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.neoforged.fml.ModList;

import java.util.EnumSet;
import java.util.Map;

//modified from
//Copyright (c) 2025 KyanBirb (sable touys)
public record SubLevelTemplate(CompoundTag plotTag) {
    private static final int DATA_VERSION = 1;
    private static final Codec<PalettedContainer<BlockState>> BLOCK_STATE_CODEC = PalettedContainer.codecRW(
            Block.BLOCK_STATE_REGISTRY, BlockState.CODEC, PalettedContainer.Strategy.SECTION_STATES, Blocks.AIR.defaultBlockState()
    );

    public static CompoundTag save(ServerLevelPlot plot) {
        LevelPlotAccessor accessor = (LevelPlotAccessor) plot;
        ServerLevelPlotAccessor accessor1 = (ServerLevelPlotAccessor) plot;
        LevelLightEngine lightEngine = accessor1.dimensionalsable$getLightEngine();
        SubLevelContainer container = accessor.dimensionalsable$getContainer();

        final CompoundTag tag = new CompoundTag();
        tag.putInt("plot_x", plot.plotPos.x - container.getOrigin().x);
        tag.putInt("plot_z", plot.plotPos.z - container.getOrigin().y);
        tag.putInt("log_size", accessor.dimensionalsable$getLogSize());
        tag.putString("biome", accessor.dimensionalsable$getBiome().location().toString());
        tag.putInt("data_version", DATA_VERSION);
        tag.putUUID("SubLevelID", plot.getSubLevel().getUniqueId());

        final ServerLevel level = plot.getSubLevel().getLevel();

        int minY = level.dimensionType().minY();
        final BlockPos center = plot.getCenterBlock().offset(0,minY,0); // minY accounts for the different starting y levels a dimension can have (overworld is -64, nether is 0)

        final CompoundTag chunks = new CompoundTag();
        for (final PlotChunkHolder chunkHolder : plot.getLoadedChunks()) {
            final ChunkPos global = chunkHolder.getPos();
            final ChunkPos local = plot.toLocal(global);
            final LevelChunk chunk = chunkHolder.getChunk();

            final CompoundTag chunkTag = new CompoundTag();
            final CompoundTag sectionsTag = new CompoundTag();

            for (int idx = 0; idx < chunk.getSectionsCount(); idx++) {
                final LevelChunkSection section = chunk.getSection(idx);

                if (section.hasOnlyAir()) {
                    continue;
                }

                final CompoundTag sectionTag = new CompoundTag();
                sectionTag.put("block_states", BLOCK_STATE_CODEC.encodeStart(NbtOps.INSTANCE, section.getStates()).getOrThrow());

                final SectionPos sectionPos = SectionPos.of(global, level.getSectionYFromSectionIndex(idx));
                final DataLayer blockLight = lightEngine.getLayerListener(LightLayer.BLOCK).getDataLayerData(sectionPos);
                final DataLayer skyLight = lightEngine.getLayerListener(LightLayer.SKY).getDataLayerData(sectionPos);

                if (blockLight != null && !blockLight.isEmpty()) {
                    sectionTag.putByteArray("BlockLight", blockLight.getData());
                }

                if (skyLight != null && !skyLight.isEmpty()) {
                    sectionTag.putByteArray("SkyLight", skyLight.getData());
                }

                sectionsTag.put(String.valueOf(idx), sectionTag);
            }
            chunkTag.put("sections", sectionsTag);

            tag.putBoolean("isLightOn", chunk.isLightCorrect());

            final ListTag blockEntitiesTag = new ListTag();
            for (final BlockPos blockPos : chunk.getBlockEntitiesPos()) {
                final CompoundTag blockEntityNBT = chunk.getBlockEntityNbtForSaving(blockPos, level.registryAccess());

                if (blockEntityNBT != null) {
                    int x = blockEntityNBT.getInt("x");
                    int y = blockEntityNBT.getInt("y");
                    int z = blockEntityNBT.getInt("z");
                    blockEntityNBT.putInt("x", x - center.getX());
                    blockEntityNBT.putInt("y", y - center.getY());
                    blockEntityNBT.putInt("z", z - center.getZ());
                    blockEntitiesTag.add(blockEntityNBT);
                }
            }

            chunkTag.put("block_entities", blockEntitiesTag);

            final ChunkAccess.TicksToSave ticksToSave = chunk.getTicksForSerialization();
            final long gameTime = level.getGameTime();
            chunkTag.put("block_ticks", ticksToSave.blocks().save(gameTime, block -> BuiltInRegistries.BLOCK.getKey(block).toString()));
            chunkTag.put("fluid_ticks", ticksToSave.fluids().save(gameTime, fluid -> BuiltInRegistries.FLUID.getKey(fluid).toString()));

            final CompoundTag heightMapsTag = new CompoundTag();

            for (final Map.Entry<Heightmap.Types, Heightmap> entry : chunk.getHeightmaps()) {
                if (chunk.getPersistedStatus().heightmapsAfter().contains(entry.getKey())) {
                    heightMapsTag.put(entry.getKey().getSerializationKey(), new LongArrayTag(entry.getValue().getRawData()));
                }
            }

            chunkTag.put("heightmaps", heightMapsTag);

            SablePlotPlatform.INSTANCE.writeLightData(tag, level.registryAccess(), chunk);
            SablePlotPlatform.INSTANCE.writeChunkAttachments(tag, level.registryAccess(), chunk);

            chunks.put(String.valueOf(ChunkPos.asLong(local.x, local.z)), chunkTag);
        }

        tag.put("chunks", chunks);
        return tag;
    }

    public static void load(ServerLevelPlot destinationPlot, final CompoundTag tag, final BlockEntityRegistry.MoveInfo moveInfo) {
        LevelPlotAccessor accessor = (LevelPlotAccessor) destinationPlot;
        ServerLevelPlotAccessor accessor1 = (ServerLevelPlotAccessor) destinationPlot;
        LevelLightEngine lightEngine = accessor1.dimensionalsable$getLightEngine();
        SubLevelContainer container = accessor.dimensionalsable$getContainer();

        final int logSize = tag.getInt("log_size");
        if (logSize != accessor.dimensionalsable$getLogSize()) {
            throw new IllegalArgumentException("Log size mismatch");
        }

        final int dataVersion = tag.contains("data_version") ? tag.getInt("data_version") : 0;
        if (dataVersion < 0 || dataVersion > DATA_VERSION) {
            throw new IllegalArgumentException("Unsupported version: " + dataVersion);
        }

        final ServerSubLevel subLevel = destinationPlot.getSubLevel();
        final ServerLevel level = subLevel.getLevel();

        int minY = level.dimensionType().minY();
        final BlockPos center = destinationPlot.getCenterBlock().offset(0,minY,0); // minY accounts for the different starting y levels a dimension can have (overworld is -64, nether is 0)

        if (tag.contains("biome")) {
            final ResourceLocation location = ResourceLocation.tryParse(tag.getString("biome"));

            if (location != null) {
                destinationPlot.setBiome(ResourceKey.create(Registries.BIOME, location));
            }
        }

        final CompoundTag chunks = tag.getCompound("chunks");
        for (final String key : chunks.getAllKeys()) {
            final long chunkPos = Long.parseLong(key);

            final int x = ChunkPos.getX(chunkPos);
            final int z = ChunkPos.getZ(chunkPos);
            final ChunkPos local = new ChunkPos(x, z);
            final ChunkPos global = destinationPlot.toGlobal(local);

            final CompoundTag chunkTag = chunks.getCompound(key);
            final CompoundTag sectionsTag = chunkTag.getCompound("sections");

            accessor1.dimensionalsable$invokeNewNonLitChunk(global);
            final LevelChunk chunk = destinationPlot.getChunk(local);

            boolean hasLit = false;
            for (final String sectionKey : sectionsTag.getAllKeys()) {
                final int yIndex = Integer.parseInt(sectionKey);


                final LevelChunkSection[] sections = chunk.getSections();

                final PalettedContainer<BlockState> palettedContainer;
                final CompoundTag sectionTag = sectionsTag.getCompound(sectionKey);

                palettedContainer = BLOCK_STATE_CODEC.parse(NbtOps.INSTANCE, sectionTag.getCompound("block_states"))
                        .promotePartial(string -> ServerLevelPlotAccessor.dimensionalsable$invokeLogLoadingErrors(new ChunkPos(chunkPos), chunk.getSectionYFromSectionIndex(yIndex), string))
                        .getOrThrow(ChunkSerializer.ChunkReadException::new);

                final Registry<Biome> biomeRegistry = level.registryAccess().registryOrThrow(Registries.BIOME);
                final PalettedContainer<Holder<Biome>> biomeContainer = new PalettedContainer<>(biomeRegistry.asHolderIdMap(), biomeRegistry.getHolderOrThrow(accessor.dimensionalsable$getBiome()), PalettedContainer.Strategy.SECTION_BIOMES);

                sections[yIndex] = new LevelChunkSection(palettedContainer, biomeContainer);

                final SectionPos sectionPos = SectionPos.of(global, level.getSectionYFromSectionIndex(yIndex));

                final boolean hasBlockLight = lightEngine.blockEngine != null && sectionTag.contains("BlockLight", Tag.TAG_BYTE_ARRAY);
                final boolean hasSkyLight = lightEngine.skyEngine != null && level.dimensionType().hasSkyLight() && sectionTag.contains("SkyLight", Tag.TAG_BYTE_ARRAY);
                if (hasBlockLight || hasSkyLight) {
                    if (!hasLit) {
                        lightEngine.retainData(global, true);
                        hasLit = true;
                    }

                    if (hasBlockLight) {
                        lightEngine.queueSectionData(LightLayer.BLOCK, sectionPos, new DataLayer(sectionTag.getByteArray("BlockLight")));
                    }

                    if (hasSkyLight) {
                        lightEngine.queueSectionData(LightLayer.SKY, sectionPos, new DataLayer(sectionTag.getByteArray("SkyLight")));
                    }
                }
            }

            if (dataVersion >= 0) {
                final LevelChunkTicks<Block> blockTicks = LevelChunkTicks.load(
                        chunkTag.getList("block_ticks", Tag.TAG_COMPOUND), id -> BuiltInRegistries.BLOCK.getOptional(ResourceLocation.tryParse(id)), global
                );
                final LevelChunkTicks<Fluid> fluidTicks = LevelChunkTicks.load(
                        chunkTag.getList("fluid_ticks", Tag.TAG_COMPOUND), id -> BuiltInRegistries.FLUID.getOptional(ResourceLocation.tryParse(id)), global
                );

                //noinspection unchecked
                ((LevelChunkTicksExtension<Block>) chunk.getBlockTicks()).sable$copy(blockTicks);
                //noinspection unchecked
                ((LevelChunkTicksExtension<Fluid>) chunk.getFluidTicks()).sable$copy(fluidTicks);

                final CompoundTag heightMapsTag = chunkTag.getCompound("heightmaps");
                final EnumSet<Heightmap.Types> enumset = EnumSet.noneOf(Heightmap.Types.class);

                for (final Heightmap.Types heightMapType : chunk.getPersistedStatus().heightmapsAfter()) {
                    final String heightMapKey = heightMapType.getSerializationKey();
                    if (heightMapsTag.contains(heightMapKey, Tag.TAG_LONG_ARRAY)) {
                        chunk.setHeightmap(heightMapType, heightMapsTag.getLongArray(heightMapKey));
                    } else {
                        enumset.add(heightMapType);
                    }
                }

                Heightmap.primeHeightmaps(chunk, enumset);

                SablePlotPlatform.INSTANCE.readLightData(chunkTag, level.registryAccess(), chunk);

                chunk.setLightCorrect(chunkTag.getBoolean("isLightOn"));
            }

            // Setup lighting
            accessor1.dimensionalsable$invokeLightChunk(chunk);

            SablePlotPlatform.INSTANCE.readChunkAttachments(chunkTag, level.registryAccess(), chunk);

            final ListTag blockEntitiesTag = chunkTag.getList("block_entities", 10);

            // Add block entities
            for (int i = 0; i < blockEntitiesTag.size(); i++) {
                CompoundTag blockEntityTag = blockEntitiesTag.getCompound(i).copy();

                //this is where we modify the block entity tag
                blockEntityTag = BlockEntityRegistry.modifyNBT(new BlockEntityRegistry.BlockEntityInfo(blockEntityTag, tag.getUUID("SubLevelID"),moveInfo));

                final boolean keepBlockEntityPacked = blockEntityTag.getBoolean("keepPacked");
                BlockPos offset = BlockEntity.getPosFromTag(blockEntityTag);
                BlockPos pos = center.offset(offset);
                blockEntityTag.putInt("x", pos.getX());
                blockEntityTag.putInt("y", pos.getY());
                blockEntityTag.putInt("z", pos.getZ());

                if (keepBlockEntityPacked) {
                    chunk.setBlockEntityNbt(blockEntityTag);
                } else {
                    final BlockPos blockPos = BlockEntity.getPosFromTag(blockEntityTag);
                    final BlockEntity blockEntity = BlockEntity.loadStatic(blockPos, chunk.getBlockState(blockPos), blockEntityTag, level.registryAccess());
                    if (blockEntity != null) {
                        chunk.setBlockEntity(blockEntity);

                        //balloon fixer
                        if (ModList.get().isLoaded("aeronautics"))
                        {
                            AeronauticsCompat.tryCreateBalloon(blockEntity);
                        }
                    }
                }
            }

            chunk.registerAllBlockEntitiesAfterLevelLoad();
            level.startTickingChunk(chunk);
            SablePlotPlatform.INSTANCE.postLoad(chunkTag, chunk);
        }

        // Before we send the chunks, let's ensure our lighting data is complete
        do {
            lightEngine.runLightUpdates();
        } while (lightEngine.hasLightWork());

        final SubLevelPhysicsSystem physicsSystem = ((ServerSubLevelContainer) container).physicsSystem();

        final BlockPos.MutableBlockPos globalBlockPos = new BlockPos.MutableBlockPos();

        // go through them all again
        for (final String key : chunks.getAllKeys()) {
            final long chunkPos = Long.parseLong(key);

            final int x = ChunkPos.getX(chunkPos);
            final int z = ChunkPos.getZ(chunkPos);
            final ChunkPos local = new ChunkPos(x, z);
            final ChunkPos global = destinationPlot.toGlobal(local);

            final PlotChunkHolder chunkHolder = destinationPlot.getChunkHolder(local);
            final LevelChunk chunk = destinationPlot.getChunk(local);
            final LevelChunkSection[] levelChunkSections = chunk.getSections();

            final Iterable<ServerPlayer> players = container.getPlayersTracking(global);
            for (final ServerPlayer player : players) {
                SubLevelPlayerChunkSender.sendChunk(player.connection::send, lightEngine, chunk);
                SubLevelPlayerChunkSender.sendChunkPoiData(level, chunk);
            }

            for (int i = 0; i < chunk.getSectionsCount(); i++) {
                final LevelChunkSection section = levelChunkSections[i];
                if (!section.hasOnlyAir()) {
                    final int sectionY = chunk.getSectionYFromSectionIndex(i);
                    final int chunkMinX = global.getMinBlockX();
                    final int chunkMinY = sectionY << 4;
                    final int chunkMinZ = global.getMinBlockZ();

                    final boolean expandPlotBackup = accessor.dimensionalsable$getExpandPlotIfNecessary();

                    // We don't want to expand the plot while loading it
                    accessor.dimensionalsable$setExpandPlotIfNecessary(false);

                    final BlockState airState = Blocks.AIR.defaultBlockState();
                    for (int xOff = 0; xOff < 16; xOff++) {
                        for (int yOff = 0; yOff < 16; yOff++) {
                            for (int zOff = 0; zOff < 16; zOff++) {
                                final BlockState state = section.getBlockState(xOff, yOff, zOff);

                                if (!state.isAir()) {
                                    globalBlockPos.set(xOff + chunkMinX, yOff + chunkMinY, zOff + chunkMinZ);
                                    final BlockPos immutable = globalBlockPos.immutable();

                                    chunkHolder.handleBlockChange(xOff, chunkMinY + yOff, zOff, airState, state);
                                    subLevel.getHeatMapManager().onSolidAdded(immutable);
                                    subLevel.getFloatingBlockController().queueAddFloatingBlock(state, immutable);
                                    physicsSystem.updateMassDataFromBlockChange(subLevel, globalBlockPos, airState, state, false);
                                    destinationPlot.onBlockChange(immutable, state);
                                }
                            }
                        }
                    }

                    // upload
                    accessor.dimensionalsable$setExpandPlotIfNecessary(expandPlotBackup);
                }
            }
        }

        destinationPlot.updateBoundingBox();
        subLevel.updateMergedMassData(1.0f);
        physicsSystem.getPipeline().onStatsChanged(subLevel);

        for (final String key : chunks.getAllKeys()) {
            final long chunkPos = Long.parseLong(key);

            final int x = ChunkPos.getX(chunkPos);
            final int z = ChunkPos.getZ(chunkPos);
            final ChunkPos local = new ChunkPos(x, z);
            final ChunkPos global = destinationPlot.toGlobal(local);

            final LevelChunk chunk = destinationPlot.getChunk(local);
            final LevelChunkSection[] levelChunkSections = chunk.getSections();

            for (int i = 0; i < chunk.getSectionsCount(); i++) {
                final LevelChunkSection section = levelChunkSections[i];
                if (!section.hasOnlyAir()) {
                    final int sectionY = chunk.getSectionYFromSectionIndex(i);
                    physicsSystem.getTicketManager().addTicketForSection(level, SectionPos.of(global.x, sectionY, global.z));
                    physicsSystem.getPipeline().handleChunkSectionAddition(section, global.x, sectionY, global.z, true);
                }
            }
        }

        subLevel.updateMergedMassData(1.0f);

        physicsSystem.getPipeline().onStatsChanged(subLevel);
    }
}
