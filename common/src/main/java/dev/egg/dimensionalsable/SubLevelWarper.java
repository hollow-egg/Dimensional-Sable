package dev.egg.dimensionalsable;

import dev.egg.dimensionalsable.utils.BlockEntityTransformer;
import dev.egg.dimensionalsable.utils.ServerSubLevelOccupancy;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelData;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelSerializer;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.ryanhcode.sable.util.SableNBTUtils;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.*;

public class SubLevelWarper {

    public static int warpConnectedSubLevels(ServerSubLevel targetSubLevel, ServerLevel level, Vector3d position) {
        return warpConnectedSubLevels(targetSubLevel, getSubLevelGroup(targetSubLevel), level, position, new Quaterniond());
    }

    public static int warpConnectedSubLevels(ServerSubLevel targetSubLevel, ServerLevel level, Vector3d position, Quaterniond orientationTransform) {
        return warpConnectedSubLevels(targetSubLevel, getSubLevelGroup(targetSubLevel), level, position, orientationTransform);
    }

    public static int warpConnectedSubLevels(ServerSubLevel targetSubLevel, List<ServerSubLevel> dependents, ServerLevel level, Vector3d position, Quaterniond orientationTransform) {

        // in the same dimension
        if (targetSubLevel.getLevel() == level) return 0;

        final ServerLevel sourceLevel = targetSubLevel.getLevel();
        final ServerSubLevelContainer sourceContainer = SubLevelContainer.getContainer(sourceLevel);
        final ServerSubLevelContainer destinationContainer = SubLevelContainer.getContainer(level);
        if (sourceContainer == null || destinationContainer == null) return 0;

        SubLevelPhysicsSystem systemSource = SubLevelPhysicsSystem.get(sourceLevel);

        // setup transform
        final Quaterniond R = new Quaterniond(orientationTransform).normalize();
        final Vector3d targetOldPos = new Vector3d(targetSubLevel.logicalPose().position());
        Tag blockEntityTransformer = BlockEntityTransformer.createTransformTag(sourceLevel, targetOldPos, position, R);

        // save and load sublevels
        List<ServerSubLevel> newSubLevels = new ArrayList<>(); // saved in case of failure
        for (ServerSubLevel subLevel : dependents) {
            RigidBodyHandle physicsHandle = systemSource.getPhysicsHandle(subLevel);

            Vector3d newPos = subLevel.logicalPose().position().sub(targetOldPos, new Vector3d());
            R.transform(newPos);
            newPos.add(position);
            Quaterniond newOrientation = R.mul(subLevel.logicalPose().orientation(), new Quaterniond());
            Pose3d pose = new Pose3d(newPos, newOrientation, subLevel.logicalPose().rotationPoint(), subLevel.logicalPose().scale());

            Vector3d newVel = R.transform(physicsHandle.getLinearVelocity(new Vector3d()));
            Vector3d newAngVel = R.transform(physicsHandle.getAngularVelocity(new Vector3d()));

            SubLevelData data = SubLevelSerializer.toData(subLevel, List.of());

            final CompoundTag fullTag = data.fullTag();

            fullTag.put("pose", SableNBTUtils.writePose3d(pose));
            fullTag.put("linear_velocity", SableNBTUtils.writeVector3d(newVel));
            fullTag.put("angular_velocity", SableNBTUtils.writeVector3d(newAngVel));
            CompoundTag plotTag = fullTag.getCompound("plot");
            BlockEntityTransformer.addTransformTag(plotTag, blockEntityTransformer);
            fullTag.put("plot", plotTag);

            final SubLevelData original = SubLevelSerializer.fromData(fullTag);
            try {
                ServerSubLevel newSubLevel = SubLevelSerializer.fullyLoad(level, original);
                newSubLevels.add(newSubLevel);
            } catch (final IndexOutOfBoundsException e) {
                //remove previous sublevels
                SubLevel halfLoaded = destinationContainer.getSubLevel(original.uuid());
                if (halfLoaded != null) {
                    destinationContainer.removeSubLevel(halfLoaded, SubLevelRemovalReason.REMOVED);
                }
                for (ServerSubLevel s : newSubLevels) {
                    destinationContainer.removeSubLevel(s, SubLevelRemovalReason.REMOVED);
                }
                throw new RuntimeException("Could not teleport sublevel: " + subLevel.getUniqueId() + "! Does not fit world bounds of target level.");
            }
        }

        // remove old sublevels
        Set<UUID> handled = new HashSet<>();
        for (ServerSubLevel subLevel : dependents) {
            // teleport entities
            Set<Entity> entities = new HashSet<>(sourceLevel.getEntities(null, subLevel.boundingBox().toMojang().inflate(1.0)));
            entities.removeIf(e -> e.isPassenger() || !handled.add(e.getUUID()));
            teleportAllEntities(entities, sourceLevel, level, R, targetOldPos, position);

            ((ServerSubLevelOccupancy) subLevel).dimensionalsable$markDimensionalRemove();
            sourceContainer.removeSubLevel(subLevel, SubLevelRemovalReason.REMOVED);
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

    private static void teleportAllEntities(Set<Entity> entities, ServerLevel sourceLevel, ServerLevel destinationLevel, Quaterniond rotation, Vector3d targetOldPos, Vector3d targetNewPos) {
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
            teleportEntity(entity, sourceLevel, destinationLevel, rotation, targetOldPos, targetNewPos);
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

    private static void teleportEntity(Entity entity, ServerLevel sourceLevel, ServerLevel destinationLevel, Quaterniond rotation, Vector3d targetOldPos, Vector3d targetNewPos)
    {
        Vec3 newPos;
        if (!EntitySubLevelUtil.shouldKick(entity) && !entity.isPassenger()) { // paintings and other stationary entities
            newPos = entity.trackingPosition();
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
