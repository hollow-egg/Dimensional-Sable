package dev.egg;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import dev.egg.registries.BlockEntityRegistry;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.api.sublevel.KinematicContraption;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3d;

import dev.egg.DimensionalSable.Pair;

import java.util.*;

public class SubLevelWarper {

    public static int WarpSubLevel(final ServerSubLevel subLevel, final Level dimension)
    {
        return WarpSubLevel(subLevel, dimension, subLevel.logicalPose().position(), true);
    }
    public static int WarpSubLevel(final ServerSubLevel subLevel, final Level dimension, final Vector3d position)
    {
        return WarpSubLevel(subLevel, dimension, position, true);
    }
    private static int WarpSubLevel(final ServerSubLevel subLevel, final Level dimension, final Vector3d position, boolean warpConnected)
    {
        ServerSubLevelContainer sourceContainer = ServerSubLevelContainer.getContainer(subLevel.getLevel());
        ServerSubLevelContainer destinationContainer = ServerSubLevelContainer.getContainer((ServerLevel)dimension);

        Collection<SubLevel> subLevels;
        if (warpConnected)
            subLevels = SubLevelHelper.getConnectedChain(subLevel); //gets all sublevels that sable considers "connected" (ropes, springs, swivel bearings etc)
        else
            subLevels = Set.of(subLevel);

        final Vector3d center = subLevel.logicalPose().position();
        WarpSubLevels(subLevels, sourceContainer, destinationContainer, center, position);

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

        Set<UUID> visited = new HashSet<>();
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

            //fixes sublevel position
            physics.getPipeline().teleport(copy, pose.position(), pose.orientation());

            if (subLevel.getName() != null)
                copy.setName(subLevel.getName());

            //teleport entities
            for(Entity entity : visitedEntities.get(subLevel.getUniqueId())) {
                TeleportEntity(entity, sourceContainer, destinationContainer, center, position, subLevel, oldToNew, visited);
            }
        }

        //delete old sublevels
        for (SubLevel subLevel : compoundSubLevel) {
            sourceContainer.removeSubLevel(subLevel, SubLevelRemovalReason.REMOVED);
        }
    }

    private static void TeleportEntity(final Entity entity, final ServerSubLevelContainer sourceContainer, final ServerSubLevelContainer destinationContainer, final Vector3d center, final Vector3d position, final SubLevel subLevel, final HashMap<UUID,Pair<UUID,Vec3i>> oldToNew, final Set<UUID> visited)
    {
        if (visited.contains(entity.getUUID()))
            return;
        visited.add(entity.getUUID());

        Vector3d newPos;
        if (!EntitySubLevelUtil.shouldKick(entity) && !entity.isPassenger()) { // paintings and other stationary entities
            var pos = entity.trackingPosition();
            var offset = oldToNew.get(subLevel.getUniqueId()).second();

            newPos = new Vector3d(pos.x+ offset.getX(),  pos.y+ offset.getY(), pos.z+ offset.getZ());
        }
        else { // all other entities
            var offset = new Vector3d(position).sub(center);
            var pos = Sable.HELPER.projectOutOfSubLevel(sourceContainer.getLevel(), entity.position());

            newPos = new Vector3d(pos.x+ offset.x,  pos.y+ offset.y, pos.z+ offset.z);
        }

        entity.unRide();
        entity.teleportTo(
                destinationContainer.getLevel(),
                newPos.x,
                newPos.y,
                newPos.z,
                Set.of(),
                entity.getYRot(),
                entity.getXRot()
        );
    }
}
