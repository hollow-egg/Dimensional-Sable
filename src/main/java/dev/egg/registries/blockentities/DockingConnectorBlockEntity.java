package dev.egg.registries.blockentities;

import com.ibm.icu.impl.Pair;
import dev.egg.registries.BlockEntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.UUID;

public class DockingConnectorBlockEntity extends BlockEntityAccessor {
    @Override
    public CompoundTag modifyNBT(final CompoundTag tag, final HashMap<UUID, Pair<UUID, Vec3i>> oldToNewSubLevelIDMap, final Pair<Vector3d,Vector3d> translation) {

        if (tag.hasUUID("OtherConnectorSubLevelId")) {
            var newGoal = oldToNewSubLevelIDMap.get(tag.getUUID("OtherConnectorSubLevelId"));
            if (newGoal != null) {
                UUID goalSubLevel = newGoal.first;
                Vec3i offset = newGoal.second;
                tag.putUUID("OtherConnectorSubLevelId", goalSubLevel);

                if (tag.contains("OtherConnector")) {
                    var list = tag.getIntArray("OtherConnector");
                    BlockPos pos = new BlockPos(list[0],list[1],list[2]);
                    pos = pos.offset(offset);
                    tag.putIntArray("OtherConnector", new int[]{pos.getX(),pos.getY(),pos.getZ()});
                }
            }
        }

        return tag;
    }
}
