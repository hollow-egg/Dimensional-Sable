package dev.egg;

import com.ibm.icu.impl.Pair;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.util.LevelAccelerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.joml.Vector3d;

import java.util.*;

public class SubLevelWarper {

    public static int WarpSubLevel(final ServerSubLevel subLevel, final Level dimension, final BlockPos position)
    {
        return WarpSubLevel(subLevel, dimension, position, true);
    }
    public static int WarpSubLevel(final ServerSubLevel subLevel, final Level dimension, final BlockPos position, boolean warpConnected)
    {
        ServerSubLevelContainer sourceContainer = ServerSubLevelContainer.getContainer(subLevel.getLevel());
        ServerSubLevelContainer destinationContainer = ServerSubLevelContainer.getContainer((ServerLevel)dimension);

        Collection<SubLevel> subLevels;
        if (warpConnected)
            subLevels = SubLevelHelper.getConnectedChain(subLevel); //gets all sublevels that sable considers "connected" (ropes, springs, swivel bearings etc)
        else
            subLevels = Set.of(subLevel);

        final Vector3d center = (Vector3d) subLevel.logicalPose().position();
        final Vector3d pos = new Vector3d(position.getX(), position.getY() + 0.5, position.getZ());

        WarpSubLevels(subLevels, sourceContainer, destinationContainer, center, pos);

        return subLevels.size();
    }

    //this function is *basically* the same as the clone command from sable
    private static void WarpSubLevels(final Collection<SubLevel> compoundSubLevel, final ServerSubLevelContainer sourceContainer, final ServerSubLevelContainer destinationContainer, final Vector3d center, final Vector3d position) {

        HashMap<UUID,Pair<UUID,Vec3i>> oldToNew = new HashMap<>();
        HashMap<UUID,CompoundTag> subLevelTags = new HashMap<>();
        HashMap<UUID,ServerLevelPlot> subLevelPlots = new HashMap<>();

        for (SubLevel subLevel : compoundSubLevel) {

            ServerSubLevel serverSubLevel = (ServerSubLevel) subLevel;

            //save sublevel data
            CompoundTag tag = SubLevelTemplate.save(serverSubLevel.getPlot());
            //allocate
            Pose3d pose = new Pose3d();
            pose.position().set(new Vector3d(subLevel.logicalPose().position()).sub(new Vector3d(center)).add(position)); //keeps relative positions
            pose.orientation().set(subLevel.logicalPose().orientation());

            //block fix (should account for sublevel expansion)
            var pos = GetFirstBlock((ServerSubLevel) subLevel, tag);
            DimensionalSable.LOGGER.info("FIRST: " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
            var boundingBox = subLevel.getPlot().getBoundingBox();
            var cornerBlock = new Vector3d(boundingBox.minX()%16, boundingBox.minY()%16, boundingBox.minZ()%16);
            DimensionalSable.LOGGER.info("CORNERBLOCK: " + cornerBlock.x + " " + cornerBlock.y + " " + cornerBlock.z);
            var blockFixOffset = pos.subtract(new Vec3i((int) cornerBlock.x, (int) cornerBlock.y, (int) cornerBlock.z));
            DimensionalSable.LOGGER.info("FIX: " + blockFixOffset.getX() + " " + blockFixOffset.getY() + " " + blockFixOffset.getZ());

            pose.position().add(pose.orientation().transform(new Vector3d(blockFixOffset.getX() + 0.5, blockFixOffset.getY() + 0.5, blockFixOffset.getZ() + 0.5)));
            pose.position().sub(pose.orientation().transform(new Vector3d((boundingBox.width()%16)/2.0, (boundingBox.height()%16)/2.0, (boundingBox.length()%16)/2.0)));
            pose.position().sub(pose.orientation().transform(new Vector3d((boundingBox.width()/16)*8.0, (boundingBox.height()/16)*8.0, (boundingBox.length()/16)*8.0)));

            ServerSubLevel copy = (ServerSubLevel) destinationContainer.allocateNewSubLevel(pose);
            subLevelTags.put(subLevel.getUniqueId(), tag);

            Vec3i start = serverSubLevel.getPlot().getCenterBlock().offset(0, sourceContainer.getLevel().dimensionType().minY(), 0);
            Vec3i end = copy.getPlot().getCenterBlock().offset(0, destinationContainer.getLevel().dimensionType().minY(), 0);
            Vec3i offset = end.subtract(start);

            oldToNew.put(subLevel.getUniqueId(), Pair.of(copy.getUniqueId(), offset));
            subLevelPlots.put(subLevel.getUniqueId(), copy.getPlot());
        }

        //load tags now that we have new plots and offsets
        for (SubLevel subLevel : compoundSubLevel) {
            ServerLevelPlot plot = subLevelPlots.get(subLevel.getUniqueId());
            //copy data to plot in other dimension
            SubLevelTemplate.load(plot, subLevelTags.get(subLevel.getUniqueId()), oldToNew, Pair.of(new Vector3d(center),new Vector3d(position))); //modifies nbt data with custom block entity accessors

            if (subLevel.getName() != null)
                plot.getSubLevel().setName(subLevel.getName());

            DimensionalSable.LOGGER.info("POS: " + plot.getSubLevel().logicalPose().position().x + " " + plot.getSubLevel().logicalPose().position().y + " " + plot.getSubLevel().logicalPose().position().z);
        }

        //delete old sublevels
        for (SubLevel subLevel : compoundSubLevel) {
            sourceContainer.removeSubLevel(subLevel, SubLevelRemovalReason.REMOVED);
        }
    }

    private static BlockPos GetFirstBlock(final ServerSubLevel subLevel, final CompoundTag tag)
    {
        final CompoundTag chunks = tag.getCompound("chunks");
        for (final String key : chunks.getAllKeys()) {
            final long chunkPos = Long.parseLong(key);

            final int x = ChunkPos.getX(chunkPos);
            final int z = ChunkPos.getZ(chunkPos);
            final ChunkPos local = new ChunkPos(x, z);

            final LevelChunk chunk = subLevel.getPlot().getChunk(local);
            final LevelChunkSection[] levelChunkSections = chunk.getSections();

            for (int i = 0; i < chunk.getSectionsCount(); i++) {
                final LevelChunkSection section = levelChunkSections[i];
                if (!section.hasOnlyAir()) {
                    for (int xOff = 0; xOff < 16; xOff++) {
                        for (int yOff = 0; yOff < 16; yOff++) {
                            for (int zOff = 0; zOff < 16; zOff++) {
                                final BlockState state = section.getBlockState(xOff, yOff, zOff);

                                if (!state.isAir()) {
                                    return new BlockPos(xOff, yOff, zOff);
                                }
                            }
                        }
                    }
                }
            }
        }
        return new BlockPos(0, 0, 0);
    }
}
