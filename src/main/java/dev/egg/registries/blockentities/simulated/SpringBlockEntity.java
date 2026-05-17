package dev.egg.registries.blockentities.simulated;

import dev.egg.registries.BlockEntityAccessor;
import dev.egg.registries.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import java.util.UUID;

public class SpringBlockEntity extends BlockEntityAccessor{
    @Override
    public CompoundTag modifyNBT(final BlockEntityRegistry.BlockEntityInfo info) {

        //this one was fairly easy, so I did it first. I recommend using it as an example for making any new block entity accessors
        //(This one also doesn't use the built in connection fixer because goal is a long for some reason)
        if (info.tag().hasUUID("GoalSubLevel")) {
            var newGoal = info.moveInfo().oldToNewSubLevelMap().get(info.tag().getUUID("GoalSubLevel"));
            if (newGoal != null) {
                UUID goalSubLevel = newGoal.first();
                Vec3i offset = newGoal.second();
                info.tag().putUUID("GoalSubLevel", goalSubLevel);

                if (info.tag().contains("Goal")) {
                    BlockPos pos = BlockPos.of(info.tag().getLong("Goal"));
                    pos = pos.offset(offset);
                    info.tag().putLong("Goal", pos.asLong());
                }
            }
        }

        return info.tag();
    }
}
