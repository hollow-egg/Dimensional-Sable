package dev.egg;

import com.ibm.icu.impl.Pair;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import dev.egg.registries.BlockEntityRegistry;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.api.physics.constraint.fixed.FixedConstraintConfiguration;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3fc;

import java.util.*;

import static java.lang.Math.floor;
import static java.lang.Math.round;

public class SubLevelWarper {

    public static int WarpSubLevel(final ServerSubLevel subLevel, final Level dimension)
    {
        return WarpSubLevel(subLevel, dimension, subLevel.logicalPose().position(), 0, true);
    }
    public static int WarpSubLevel(final ServerSubLevel subLevel, final Level dimension, final Vector3d position, double lockTime)
    {
        return WarpSubLevel(subLevel, dimension, position, lockTime, true);
    }
    public static int WarpSubLevel(final ServerSubLevel subLevel, final Level dimension, final Vector3d position)
    {
        return WarpSubLevel(subLevel, dimension, position, 0, true);
    }
    public static int WarpSubLevel(final ServerSubLevel subLevel, final Level dimension, double lockTime)
    {
        return WarpSubLevel(subLevel, dimension, subLevel.logicalPose().position(), lockTime, true);
    }
    private static int WarpSubLevel(final ServerSubLevel subLevel, final Level dimension, final Vector3d position, double lockTime, boolean warpConnected)
    {
        ServerSubLevelContainer sourceContainer = ServerSubLevelContainer.getContainer(subLevel.getLevel());
        ServerSubLevelContainer destinationContainer = ServerSubLevelContainer.getContainer((ServerLevel)dimension);

        Collection<SubLevel> subLevels;
        if (warpConnected)
            subLevels = SubLevelHelper.getConnectedChain(subLevel); //gets all sublevels that sable considers "connected" (ropes, springs, swivel bearings etc)
        else
            subLevels = Set.of(subLevel);

        final Vector3d center = subLevel.logicalPose().position();
        WarpSubLevels(subLevels, sourceContainer, destinationContainer, center, position, lockTime);

        return subLevels.size();
    }

    //this function is *basically* the same as the clone command from sable
    private static void WarpSubLevels(final Collection<SubLevel> compoundSubLevel, final ServerSubLevelContainer sourceContainer, final ServerSubLevelContainer destinationContainer, final Vector3d center, final Vector3d position, final double lockTime) {

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

            List<Entity> candidates = sourceContainer.getLevel().getEntities(null, box);
            visitedEntities.put(subLevel.getUniqueId(), new HashSet<>(candidates));

            //currently, my solution for contraptions is to disassemble them. I'm looking into other options
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

        Set<Entity> visited = new HashSet<>();
        HashMap<UUID,UUID> passengerToEntityMap = new HashMap<>();
        HashMap<UUID,Vector3d> entityToPositionMap = new HashMap<>();
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
                            sourceContainer.getLevel(),
                            destinationContainer.getLevel()));

            physics.getPipeline().teleport(copy, pose.position(), pose.orientation());
            if (lockTime > 0)
                SubLevelLockManager.AddLock(copy, lockTime); //freezes sublevels for "lockTime" seconds to allow for things to catch up

            if (subLevel.getName() != null)
                copy.setName(subLevel.getName());

            //save teleport positions
            for(Entity entity : visitedEntities.get(subLevel.getUniqueId())) {
                if (visited.contains(entity)) continue;
                visited.add(entity);

                Entity ride = entity.getVehicle();
                if (ride != null)
                    passengerToEntityMap.put(entity.getUUID(), ride.getUUID());

                if (!EntitySubLevelUtil.shouldKick(entity)) { // paintings and other stationary entities
                    var pos = entity.position();
                    var offset = oldToNew.get(subLevel.getUniqueId()).second;
                    entityToPositionMap.put(entity.getUUID(), new Vector3d(pos.x+ offset.getX(),  pos.y+ offset.getY(), pos.z+ offset.getZ()));
                }
                else { // all other entities
                    var offset = new Vector3d(position).sub(center);
                    var pos = Sable.HELPER.projectOutOfSubLevel(sourceContainer.getLevel(), entity.position());

                    entityToPositionMap.put(entity.getUUID(), new Vector3d(pos.x+ offset.x,  pos.y+ offset.y, pos.z+ offset.z));
                }
            }
        }

        //delete old sublevels
        for (SubLevel subLevel : compoundSubLevel) {
            sourceContainer.removeSubLevel(subLevel, SubLevelRemovalReason.REMOVED);
        }

        //hopefully teleport entities
        for (UUID id : entityToPositionMap.keySet()) {
            Entity entity = destinationContainer.getLevel().getEntity(id);
            if (entity == null) continue;

            var pos = entityToPositionMap.get(id);
            entity.teleportTo(destinationContainer.getLevel(),
                    pos.x,
                    pos.y,
                    pos.z,
                    Set.of(),
                    entity.getYRot(),
                    entity.getXRot());
        }

        //put passengers back on their rides (this does not work atm)
        MinecraftServer server = destinationContainer.getLevel().getServer();
        server.execute(() -> { // next tick
            for (UUID id : passengerToEntityMap.keySet()) {
                Entity entity = destinationContainer.getLevel().getEntity(id);
                Entity ride = destinationContainer.getLevel().getEntity(passengerToEntityMap.get(id));

                if (entity != null && ride != null) {
                    entity.startRiding(ride);

                    DimensionalSable.LOGGER.info(entity + " RIDING -> " + ride);
                }
            }
        });
    }
}
