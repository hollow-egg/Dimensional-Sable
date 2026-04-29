package dev.egg;

import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;

import java.util.Collection;
import java.util.Set;

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
        }

        //delete old sublevels
        for (SubLevel subLevel : compoundSubLevel) {
            sourceContainer.removeSubLevel(subLevel, SubLevelRemovalReason.REMOVED);
        }
    }
}
