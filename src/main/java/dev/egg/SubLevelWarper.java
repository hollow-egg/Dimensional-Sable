package dev.egg;

import com.ibm.icu.impl.Pair;
import dev.ryanhcode.sable.api.SubLevelHelper;
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
import net.minecraft.world.level.Level;
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

            ServerSubLevel copy = (ServerSubLevel) destinationContainer.allocateNewSubLevel(pose);
            subLevelTags.put(subLevel.getUniqueId(), tag);

            Vec3i start = serverSubLevel.getPlot().getCenterBlock().offset(0, sourceContainer.getLevel().dimensionType().minY(), 0);
            Vec3i end = copy.getPlot().getCenterBlock().offset(0, destinationContainer.getLevel().dimensionType().minY(), 0);
            Vec3i offset = end.subtract(start);

            oldToNew.put(subLevel.getUniqueId(), Pair.of(copy.getUniqueId(), offset));
            subLevelPlots.put(subLevel.getUniqueId(), copy.getPlot());
        }

        var physics = SubLevelPhysicsSystem.get(destinationContainer.getLevel());
        //load tags now that we have new plots and offsets
        for (SubLevel subLevel : compoundSubLevel) {
            ServerLevelPlot plot = subLevelPlots.get(subLevel.getUniqueId());
            //copy data to plot in other dimension
            Pose3d pose = new Pose3d(plot.getSubLevel().logicalPose());
            SubLevelTemplate.load(plot, subLevelTags.get(subLevel.getUniqueId()), oldToNew, Pair.of(new Vector3d(center),new Vector3d(position))); //modifies nbt data with custom block entity accessors
            physics.getPipeline().teleport(plot.getSubLevel(), pose.position(), pose.orientation());

            if (subLevel.getName() != null)
                plot.getSubLevel().setName(subLevel.getName());
        }

        //delete old sublevels
        for (SubLevel subLevel : compoundSubLevel) {
            sourceContainer.removeSubLevel(subLevel, SubLevelRemovalReason.REMOVED);
        }
    }
}
