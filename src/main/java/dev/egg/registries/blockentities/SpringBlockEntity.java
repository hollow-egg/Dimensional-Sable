package dev.egg.registries.blockentities;

import com.ibm.icu.impl.Pair;
import dev.egg.registries.BlockEntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.UUID;

public class SpringBlockEntity extends BlockEntityAccessor{
    @Override
    public CompoundTag modifyNBT(final CompoundTag tag, final HashMap<UUID,Pair<UUID,Vec3i>> oldToNewSubLevelIDMap, final Pair<Vector3d,Vector3d> translation) {

        //this one was fairly easy, so I did it first. I recommend using it as an example for making any new block entity accessors
        if (tag.hasUUID("GoalSubLevel")) {
            var newGoal = oldToNewSubLevelIDMap.get(tag.getUUID("GoalSubLevel"));
            if (newGoal != null) {
                UUID goalSubLevel = newGoal.first;
                Vec3i offset = newGoal.second;
                tag.putUUID("GoalSubLevel", goalSubLevel);

                if (tag.contains("Goal")) {
                    BlockPos pos = BlockPos.of(tag.getLong("Goal"));
                    pos = pos.offset(offset);
                    tag.putLong("Goal", pos.asLong());
                }
            }
        }

        return tag;
    }
}
