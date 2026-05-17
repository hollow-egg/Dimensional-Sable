package dev.egg.registries.blockentities.create;

import dev.egg.registries.BlockEntityAccessor;
import dev.egg.registries.BlockEntityRegistry;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;

public class DisplayLinkBlockEntity extends BlockEntityAccessor {

    @Override
    public CompoundTag modifyNBT(final BlockEntityRegistry.BlockEntityInfo info) {
        if (info.tag().contains("components")) {
            CompoundTag comp = info.tag().getCompound("components");
            if (comp.contains("create:click_to_link_data")) {
                CompoundTag data = comp.getCompound("create:click_to_link_data");

                if (data.contains("selected_pos"))
                {
                    var list = data.getIntArray("selected_pos");
                    BlockPos pos = new BlockPos(list[0], list[1], list[2]);

                    ServerSubLevel sublevel = (ServerSubLevel) Sable.HELPER.getContaining(info.moveInfo().oldDimension(), pos);
                    if (sublevel != null) //if pos is in a sublevel
                    {
                        var offset = info.moveInfo().oldToNewSubLevelMap().get(sublevel.getUniqueId());
                        if (offset != null) //avoid accessing possibly null pair (sublevel might not be connected)
                            pos = pos.offset(offset.second());
                    }
                    else // target is in world position
                    {
                        if (info.tag().contains("TargetOffset")) {
                            var offsetList = info.tag().getIntArray("TargetOffset");
                            BlockPos offsetPos = new BlockPos(offsetList[0],offsetList[1],offsetList[2]);

                            var offset = info.moveInfo().oldToNewSubLevelMap().get(info.oldSubLevelID());
                            offsetPos = offsetPos.subtract(offset.second());

                            info.tag().putIntArray("TargetOffset", new int[]{offsetPos.getX(),offsetPos.getY(),offsetPos.getZ()});
                        }
                    }

                    data.putIntArray("selected_pos", new int[]{pos.getX(), pos.getY(), pos.getZ()});
                    data.putString("selected_dim", info.moveInfo().newDimension().dimension().location().getPath());
                }
                comp.put("create:click_to_link_data", data);
            }
            info.tag().put("components", comp);
        }

        return info.tag();
    }
}
