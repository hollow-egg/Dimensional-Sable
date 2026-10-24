package dev.egg.dimensionalsable.utils;

import dev.ryanhcode.sable.Sable;
import net.minecraft.nbt.*;
import net.minecraft.world.level.Level;
import org.joml.Quaterniond;
import org.joml.Vector3d;

public record BlockEntityTransformer(Level oldLevel, Vector3d oldPosition, Vector3d newPosition, Quaterniond rotation) {

    public void transformBlockEntityTag(CompoundTag tag) {
        transformBlockEntityRecursive(tag);
    }

    private void transformBlockEntityRecursive(Object current) {
        if (current instanceof CompoundTag compoundTag) {
            for (String key : compoundTag.getAllKeys()) {
                Tag subTag = compoundTag.get(key);

                if (subTag instanceof CompoundTag || subTag instanceof ListTag) {
                    transformBlockEntityRecursive(subTag);
                    compoundTag.put(key, subTag);
                }
            }
        } else if (current instanceof ListTag listTag) {
            if (listTag.size() == 3 && listTag.getElementType() == Tag.TAG_DOUBLE) {
                Vector3d pos = new Vector3d(listTag.getDouble(0), listTag.getDouble(1), listTag.getDouble(2));

                if (Sable.HELPER.isInPlotGrid(oldLevel, pos)) return; // do not offset nbt positions in sublevel space

                pos.sub(oldPosition);
                rotation.transform(pos);
                pos.add(newPosition);

                listTag.set(0, DoubleTag.valueOf(pos.x));
                listTag.set(1, DoubleTag.valueOf(pos.y));
                listTag.set(2, DoubleTag.valueOf(pos.z));
            } else {
                if (listTag.getElementType() == Tag.TAG_COMPOUND || listTag.getElementType() == Tag.TAG_LIST) {
                    for (int i = 0; i < listTag.size(); ++i) {
                        Tag subTag = listTag.get(i);
                        transformBlockEntityRecursive(subTag);
                        listTag.set(i, subTag);
                    }
                }
            }
        }
    }
}
