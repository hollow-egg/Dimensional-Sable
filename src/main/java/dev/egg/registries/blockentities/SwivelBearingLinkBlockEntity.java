package dev.egg.registries.blockentities;

import com.ibm.icu.impl.Pair;
import dev.egg.registries.BlockEntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.UUID;

public class SwivelBearingLinkBlockEntity extends BlockEntityAccessor {
    @Override
    public CompoundTag modifyNBT(final CompoundTag tag, final HashMap<UUID, Pair<UUID, Vec3i>> oldToNewSubLevelIDMap, final Pair<Vector3d,Vector3d> translation) {

        if (tag.hasUUID("ParentSubLevelId")) {
            var newGoal = oldToNewSubLevelIDMap.get(tag.getUUID("ParentSubLevelId"));
            if (newGoal != null) {
                UUID goalSubLevel = newGoal.first;
                Vec3i offset = newGoal.second;
                tag.putUUID("ParentSubLevelId", goalSubLevel);

                if (tag.contains("ParentPos")) {
                    var list = tag.getIntArray("ParentPos");
                    BlockPos pos = new BlockPos(list[0],list[1],list[2]);
                    pos = pos.offset(offset);
                    tag.putIntArray("ParentPos", new int[]{pos.getX(),pos.getY(),pos.getZ()});
                }
            }
        }

        return tag;
    }
}
