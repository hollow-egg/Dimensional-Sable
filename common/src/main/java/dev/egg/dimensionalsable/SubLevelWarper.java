package dev.egg.dimensionalsable;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.*;

public class SubLevelWarper {

    public static int warpConnectedSubLevels(ServerSubLevel targetSubLevel, ServerLevel level, Vector3d position) {
        return warpConnectedSubLevels(targetSubLevel, getSubLevelGroup(targetSubLevel), level, position, new Quaterniond());
    }

    public static int warpConnectedSubLevels(ServerSubLevel targetSubLevel, ServerLevel level, Vector3d position, Quaterniond orientationTransform) {
        return warpConnectedSubLevels(targetSubLevel, getSubLevelGroup(targetSubLevel), level, position, orientationTransform);
    }

    public static int warpConnectedSubLevels(ServerSubLevel targetSubLevel, List<ServerSubLevel> dependents, ServerLevel level, Vector3d position, Quaterniond orientationTransform) {
        final ServerLevel sourceLevel = targetSubLevel.getLevel();
        final ServerSubLevelContainer sourceContainer = SubLevelContainer.getContainer(sourceLevel);
        final ServerSubLevelContainer destinationContainer = SubLevelContainer.getContainer(level);
        if (sourceContainer == null || destinationContainer == null) return 0;

        SubLevelPhysicsSystem systemSource = SubLevelPhysicsSystem.get(sourceLevel);

        // setup transform
        final Quaterniond R = new Quaterniond(orientationTransform).normalize();
        final Vector3d targetOldPos = new Vector3d(targetSubLevel.logicalPose().position());

        Set<UUID> handled = new HashSet<>();

        // save and load sublevels
        for (ServerSubLevel dependent : dependents) {
            RigidBodyHandle physicsHandle = systemSource.getPhysicsHandle(dependent);

            Vector3d newPos = dependent.logicalPose().position().sub(targetOldPos, new Vector3d());
            R.transform(newPos);
            newPos.add(position);
            Quaterniond newOrientation = R.mul(dependent.logicalPose().orientation(), new Quaterniond());
            Pose3d pose = new Pose3d(newPos, newOrientation, dependent.logicalPose().rotationPoint(), dependent.logicalPose().scale());

            Vector3d newVel = R.transform(physicsHandle.getLinearVelocity(new Vector3d()));
            Vector3d newAngVel = R.transform(physicsHandle.getAngularVelocity(new Vector3d()));

            BoundingBox3ic plotBounds = dependent.getPlot().getBoundingBox();
            BoundingBox3d boundingBox = new BoundingBox3d(plotBounds.minX(), plotBounds.minY(), plotBounds.minZ(), plotBounds.maxX(), plotBounds.maxY(), plotBounds.maxZ());
            List<BlockPos> blocks = BlockPos.betweenClosedStream(plotBounds.toMojang()).map(BlockPos::immutable).toList();;

            final ServerSubLevel subLevel = (ServerSubLevel) destinationContainer.allocateNewSubLevel(pose);

            final LevelPlot plot = subLevel.getPlot();
            plot.newEmptyChunk(plot.getCenterChunk());
//            if (plot.getChunkHolder(plot.getCenterChunk()) == null) {
//                destinationContainer.removeSubLevel(subLevel, SubLevelRemovalReason.REMOVED);
//                DimensionalSable.LOGGER.error("Could not create sub level plot: {}", plot.getCenterChunk());
//                return 0;
//            }
            final BoundingBox3i bounds = new BoundingBox3i(boundingBox);
            final BlockPos anchor = dependent.getPlot().getCenterBlock();
            final BlockPos plotAnchor = plot.getCenterBlock();
            final SubLevelAssemblyHelper.AssemblyTransform transform = new SubLevelAssemblyHelper.AssemblyTransform(anchor, plotAnchor, 0, Rotation.NONE, level);

            // teleport entities
            Set<Entity> entities = new HashSet<>(sourceLevel.getEntities(null, dependent.boundingBox().toMojang().inflate(1.0)));
            entities.removeIf(e ->  {
                if (e.isPassenger()) return true;
                SubLevel tracking = Sable.HELPER.getTrackingOrVehicleSubLevel(e);
                if (tracking != null && tracking != dependent) return true;
                return !handled.add(e.getUUID());
            });
            teleportAllEntities(entities, sourceLevel, level, R, targetOldPos, position, plotAnchor.getCenter().subtract(anchor.getCenter()));

            // move blocks
            SubLevelAssemblyHelper.moveBlocks(sourceLevel, transform, blocks);
            final Vector3dc centerOfMass = subLevel.getMassTracker().getCenterOfMass();
            if (centerOfMass == null) {
                subLevel.logicalPose().rotationPoint()
                        .set(plotAnchor.getX() + 0.5, plotAnchor.getY() + 0.5, plotAnchor.getZ() + 0.5);
            }

            // update position
            subLevel.logicalPose().position().set(newPos);
            if (!subLevel.isRemoved()) {
                destinationContainer.physicsSystem().getPipeline().teleport(subLevel, subLevel.logicalPose().position(), subLevel.logicalPose().orientation());
            }
            subLevel.updateLastPose();

            SubLevelAssemblyHelper.moveTrackingPoints(sourceLevel, bounds, subLevel, transform);

            subLevel.setUniqueId(dependent.getUniqueId());
            subLevel.setName(dependent.getName());
        }

        return dependents.size();
    }

    public static List<ServerSubLevel> getSubLevelGroup(ServerSubLevel subLevel) {
        final List<ServerSubLevel> visited = new ArrayList<>();
        final ObjectOpenHashSet<ServerSubLevel> frontier = new ObjectOpenHashSet<>();

        frontier.add(subLevel);

        while (!frontier.isEmpty()) {
            final ServerSubLevel current = frontier.iterator().next();

            frontier.remove(current);
            visited.add(current);

            final Iterable<SubLevel> intersecting = Sable.HELPER.getAllIntersecting(current.getLevel(), new BoundingBox3d(current.boundingBox()));

            // Intersecting dependencies
            for (final SubLevel neighbor : intersecting) {
                final ServerSubLevel serverNeighbor = (ServerSubLevel) neighbor;

                if (!visited.contains(serverNeighbor)) {
                    frontier.add(serverNeighbor);
                }
            }

            // Actor dependencies
            for (final BlockEntitySubLevelActor actor : current.getPlot().getBlockEntityActors()) {
                final Iterable<SubLevel> dependencies = actor.sable$getConnectionDependencies();

                if (dependencies == null) continue;

                for (final SubLevel dependency : dependencies) {

                    if (!visited.contains((ServerSubLevel) dependency)) {
                        frontier.add((ServerSubLevel) dependency);
                    }
                }
            }
        }

        return visited;
    }

    private static void teleportAllEntities(Set<Entity> entities, ServerLevel sourceLevel, ServerLevel destinationLevel, Quaterniond rotation, Vector3d targetOldPos, Vector3d targetNewPos, Vec3 blockOffset) {
        // hold vehicles for teleport
        Map<UUID,List<UUID>> vehicles = new HashMap<>();
        ObjectOpenHashSet<Entity> frontier = new ObjectOpenHashSet<>();
        frontier.addAll(entities);

        while (!frontier.isEmpty()) {
            Entity vehicle = frontier.iterator().next();
            frontier.remove(vehicle);
            if (vehicle.getPassengers().isEmpty()) continue;
            List<Entity> passengers = vehicle.getPassengers();

            List<UUID> passengerIDs = new ArrayList<>();
            for (final Entity passenger : passengers) {
                passengerIDs.add(passenger.getUUID());
                entities.add(passenger);
                frontier.add(passenger);
            }
            vehicles.put(vehicle.getUUID(), passengerIDs);
        }
        for (Entity entity : entities) {
            teleportEntity(entity, sourceLevel, destinationLevel, rotation, targetOldPos, targetNewPos, blockOffset);
        }
        // attempt to re-ride entities
        vehicles.forEach((entityID, passengerIDs) -> {
            Entity vehicle = destinationLevel.getEntity(entityID);
            if (vehicle == null) return;
            passengerIDs.forEach((UUID passengerID) -> {
                Entity passenger = destinationLevel.getEntity(passengerID);
                if (passenger == null) return;

                passenger.startRiding(vehicle, true);
            });
        });
    }

    private static void teleportEntity(Entity entity, ServerLevel sourceLevel, ServerLevel destinationLevel, Quaterniond rotation, Vector3d targetOldPos, Vector3d targetNewPos, Vec3 blockOffset)
    {
        Vec3 newPos;
        if (!EntitySubLevelUtil.shouldKick(entity) && !entity.isPassenger()) { // paintings and other stationary entities
            newPos = entity.trackingPosition().add(blockOffset);
        }
        else { // all other entities
            Vec3 pos = Sable.HELPER.projectOutOfSubLevel(sourceLevel, entity.position());
            Vector3d p = new Vector3d(pos.x, pos.y, pos.z).sub(targetOldPos);
            rotation.transform(p);
            p.add(targetNewPos);
            newPos = new Vec3(p.x, p.y, p.z);
        }

        entity.teleportTo(
                destinationLevel,
                newPos.x,
                newPos.y,
                newPos.z,
                Set.of(),
                entity.getYRot(),
                entity.getXRot()
        );
    }
}
