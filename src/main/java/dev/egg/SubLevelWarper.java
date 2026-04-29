package dev.egg;

import dev.egg.mixin.ServerLevelPlotAccessor;
import dev.egg.registries.BlockEntityRegistry;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.platform.SablePlotPlatform;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.joml.Vector3d;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

public class SubLevelWarper {

    public static int WarpSubLevel(ServerSubLevel subLevel, Level dimension, BlockPos position)
    {
        return WarpSubLevel(subLevel, dimension, position, true);
    }
    public static int WarpSubLevel(ServerSubLevel subLevel, Level dimension, BlockPos position, boolean warpConnected)
    {
        ServerSubLevelContainer sourceContainer = ServerSubLevelContainer.getContainer(subLevel.getLevel());
        ServerSubLevelContainer destinationContainer = ServerSubLevelContainer.getContainer((ServerLevel)dimension);

        Collection<SubLevel> subLevels;
        if (warpConnected)
            subLevels = SubLevelHelper.getConnectedChain(subLevel); //gets all sublevels that sable considers "connected" (ropes, springs, swivel bearings etc)
        else
            subLevels = Set.of(subLevel);

        final Vector3d center = subLevel.logicalPose().position();
        final Vector3d pos = new Vector3d(position.getX(), position.getY() + 0.5f, position.getZ());

        WarpSubLevels(subLevels, sourceContainer, destinationContainer, center, pos);

        return subLevels.size();
    }

    //this function is *basically* the same as the clone command from sable
    private static void WarpSubLevels(Collection<SubLevel> compoundSubLevel, ServerSubLevelContainer sourceContainer, ServerSubLevelContainer destinationContainer, Vector3d center, Vector3d position) {

        HashMap<UUID,UUID> oldToNew = new HashMap<>();
        HashMap<UUID,CompoundTag> subLevelTags = new HashMap<>();
        HashMap<UUID, Vec3i> subLevelToOffset = new HashMap<>();
        HashMap<UUID,ServerLevelPlot> subLevelPlots = new HashMap<>();

        for (SubLevel subLevel : compoundSubLevel) {

            ServerSubLevel serverSubLevel = (ServerSubLevel) subLevel;

            //save sublevel data
            CompoundTag tag = SubLevelTemplate.save(serverSubLevel.getPlot());
            //allocate
            Pose3d pose = new Pose3d();
            pose.position().set(subLevel.logicalPose().position().sub(center).add(position)); //keeps relative positions
            pose.orientation().set(subLevel.logicalPose().orientation());
            ServerSubLevel copy = (ServerSubLevel) destinationContainer.allocateNewSubLevel(pose);
            copy.updateLastPose(); //to avoid massive jumps
            //copy data to plot in other dimension
            SubLevelTemplate.load(copy.getPlot(), tag);

            if (serverSubLevel.getName() != null)
                copy.setName(serverSubLevel.getName());

            oldToNew.put(subLevel.getUniqueId(), copy.getUniqueId());
            subLevelTags.put(copy.getUniqueId(), tag);

            Vec3i start = serverSubLevel.getPlot().getCenterBlock().offset(0, -sourceContainer.getLevel().dimensionType().minY(), 0);
            Vec3i end = copy.getPlot().getCenterBlock().offset(0, destinationContainer.getLevel().dimensionType().minY(), 0);
            subLevelToOffset.put(copy.getUniqueId(), end.subtract(start));
            subLevelPlots.put(copy.getUniqueId(), copy.getPlot());
        }

        //modify nbt
        for(UUID subLevelid : subLevelTags.keySet())
        {
            final Vec3i offset = subLevelToOffset.get(subLevelid);
            CompoundTag tag = subLevelTags.get(subLevelid);

            ServerLevel level = destinationContainer.getLevel();

            ServerLevelPlot plot = subLevelPlots.get(subLevelid);
            BlockPos plotCenter = plot.getCenterBlock().offset(0, level.dimensionType().minY(), 0); // minY accounts for the different starting y levels a dimension can have (overworld is -64, nether is 0)

            final CompoundTag chunks = tag.getCompound("chunks");
            for (final String key : chunks.getAllKeys()) {
                final CompoundTag chunkTag = chunks.getCompound(key);
                final long chunkPos = Long.parseLong(key);

                final int x = ChunkPos.getX(chunkPos);
                final int z = ChunkPos.getZ(chunkPos);
                final ChunkPos local = new ChunkPos(x, z);

                final LevelChunk chunk = plot.getChunk(local);

                final ListTag blockEntitiesTag = chunkTag.getList("block_entities", 10);
                for (int i = 0; i < blockEntitiesTag.size(); i++) {
                    CompoundTag blockEntityTag = blockEntitiesTag.getCompound(i).copy();

                    blockEntityTag = BlockEntityRegistry.modifyNBT(blockEntityTag, oldToNew, offset);

                    final boolean keepBlockEntityPacked = blockEntityTag.getBoolean("keepPacked");

                    BlockPos relativePos = BlockEntity.getPosFromTag(blockEntityTag);
                    BlockPos pos = plotCenter.offset(relativePos);
                    blockEntityTag.putInt("x", pos.getX());
                    blockEntityTag.putInt("y", pos.getY());
                    blockEntityTag.putInt("z", pos.getZ());

                    if (keepBlockEntityPacked)
                        chunk.setBlockEntityNbt(blockEntityTag);
                    else
                    {
                        final BlockPos blockPos = BlockEntity.getPosFromTag(blockEntityTag);
                        final BlockEntity blockEntity = BlockEntity.loadStatic(blockPos, chunk.getBlockState(blockPos), blockEntityTag, level.registryAccess());
                        if (blockEntity != null)
                            chunk.setBlockEntity(blockEntity);
                    }
                }

                chunk.registerAllBlockEntitiesAfterLevelLoad();
                level.startTickingChunk(chunk);
                SablePlotPlatform.INSTANCE.postLoad(chunkTag, chunk);
            }
        }

        //delete old sublevels
        for (SubLevel subLevel : compoundSubLevel) {
            sourceContainer.removeSubLevel(subLevel, SubLevelRemovalReason.REMOVED);
        }
    }
}
