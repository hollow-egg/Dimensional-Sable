package dev.egg;

import com.ibm.icu.impl.Pair;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import dev.egg.registries.BlockEntityRegistry;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.sublevel.KinematicContraption;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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
        HashMap<UUID,Set<Entity>> visitedEntities = new HashMap<>();

        for (SubLevel subLevel : compoundSubLevel) {

            ServerSubLevel serverSubLevel = (ServerSubLevel) subLevel;

            //grab entities
            var boxX = subLevel.boundingBox().width();
            var boxY = subLevel.boundingBox().height();
            var boxZ = subLevel.boundingBox().length();
            AABB box = new AABB(-boxX/2 + center.x, -boxY/2 + center.y, -boxZ/2 + center.z, boxX/2 + center.x, boxY/2 + center.y, boxZ/2 + center.z);
            box.inflate(1.0);

            DimensionalSable.LOGGER.info(box.toString());

            List<Entity> candidates = sourceContainer.getLevel().getEntities(null, box);
            visitedEntities.put(subLevel.getUniqueId(), new HashSet<>(candidates));

            for(KinematicContraption contraption : serverSubLevel.getPlot().getContraptions())
                ((AbstractContraptionEntity) contraption).disassemble();

            //save sublevel data
            CompoundTag tag = SubLevelTemplate.save(serverSubLevel.getPlot());
            //allocate
            Pose3d pose = new Pose3d();
            pose.position().set(new Vector3d(subLevel.logicalPose().position()).sub(new Vector3d(center)).add(position)); //keeps relative positions
            pose.orientation().set(subLevel.logicalPose().orientation());

            ServerSubLevel copy = (ServerSubLevel) destinationContainer.allocateNewSubLevel(pose);
            subLevelTags.put(subLevel.getUniqueId(), tag);

            Vec3i start = serverSubLevel.getPlot().getCenterBlock().offset(0, sourceContainer.getLevel().dimensionType().minY(), 0);
            Vec3i end = copy.getPlot().getCenterBlock().offset(0, destinationContainer.getLevel().dimensionType().minY(), 0);
            Vec3i offset = end.subtract(start);

            oldToNew.put(subLevel.getUniqueId(), Pair.of(copy.getUniqueId(),offset));
            subLevelPlots.put(subLevel.getUniqueId(), copy.getPlot());
        }

        Set<Entity> movedEntities = new HashSet<>();
        var physics = SubLevelPhysicsSystem.get(destinationContainer.getLevel());
        //load tags now that we have new plots and offsets
        for (SubLevel subLevel : compoundSubLevel) {
            ServerLevelPlot plot = subLevelPlots.get(subLevel.getUniqueId());
            ServerSubLevel copy = plot.getSubLevel();
            //copy data to plot in other dimension
            Pose3d pose = new Pose3d(copy.logicalPose());
            //modifies nbt data with custom block entity accessors
            SubLevelTemplate.load(plot, subLevelTags.get(subLevel.getUniqueId()),
                    new BlockEntityRegistry.MoveInfo(
                            oldToNew,
                            new Vector3d(position).sub(center),
                            sourceContainer.getLevel().dimension().location().getPath(),
                            destinationContainer.getLevel().dimension().location().getPath()));

            physics.getPipeline().teleport(copy, pose.position(), pose.orientation());

            if (subLevel.getName() != null)
                copy.setName(subLevel.getName());

            //hopefully teleport entities
            for(Entity entity : visitedEntities.get(subLevel.getUniqueId())) {
                if (movedEntities.contains(entity))continue;
                movedEntities.add(entity);

                SubLevel tracking = Sable.HELPER.getTrackingOrVehicleSubLevel(entity);
                DimensionalSable.LOGGER.info(entity.toString());

//                if (tracking == subLevel) {
//                    var centerBlock = ((ServerSubLevel)subLevel).getPlot().getCenterBlock();
//                    var relativePos = entity.position().subtract(Vec3.atLowerCornerOf(centerBlock));
//                    var newSubLevelPos = Vec3.atLowerCornerOf(copy.getPlot().getCenterBlock());
//                    entity.teleportTo(copy.getLevel(),
//                            newSubLevelPos.x + relativePos.x,
//                            newSubLevelPos.y + relativePos.y,
//                            newSubLevelPos.z + relativePos.z,
//                            Set.of(),
//                            entity.getYRot(),
//                            entity.getXRot());
//                }
//                else {
                    var offset = new Vector3d(position).sub(center);
                    entity.teleportTo(plot.getSubLevel().getLevel(),
                            entity.position().x + offset.x,
                            entity.position().y + offset.y,
                            entity.position().z + offset.z,
                            Set.of(),
                            entity.getYRot(),
                            entity.getXRot());
                //}
                DimensionalSable.LOGGER.info(entity.toString());
            }
        }

        //delete old sublevels
        for (SubLevel subLevel : compoundSubLevel) {
            sourceContainer.removeSubLevel(subLevel, SubLevelRemovalReason.REMOVED);
        }
    }
}
