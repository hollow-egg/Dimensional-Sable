package dev.egg.registries.blockentities.simulated;

import dev.egg.registries.BlockEntityAccessor;
import dev.egg.registries.BlockEntityRegistry;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.*;

import dev.egg.DimensionalSable.Pair;
import java.util.UUID;

public class RopeConnectorBlockEntity extends BlockEntityAccessor {

    @Override
    public CompoundTag modifyNBT(final BlockEntityRegistry.BlockEntityInfo info) {

        if (info.tag().contains("Strand")) {
            CompoundTag strandTag = info.tag().getCompound("Strand");

            // attachments
            if (strandTag.contains("attachments", Tag.TAG_LIST)) {
                ListTag attachments = strandTag.getList("attachments", Tag.TAG_COMPOUND);

                for (int i = 0; i < attachments.size(); i++) {
                    CompoundTag attachment = attachments.getCompound(i);

                    // sublevelID
                    if (attachment.contains("subLevelID", Tag.TAG_STRING)) {
                        UUID old = UUID.fromString(attachment.getString("subLevelID"));
                        Pair<UUID,Vec3i> mapped = info.moveInfo().oldToNewSubLevelMap().get(old);

                        if (mapped != null) {
                            if (attachment.contains("blockAttachment")) {
                                var posArray = attachment.getIntArray("blockAttachment");
                                posArray[0] += mapped.second().getX();
                                posArray[1] += mapped.second().getY();
                                posArray[2] += mapped.second().getZ();
                                attachment.putIntArray("blockAttachment", posArray);
                            }

                            attachment.putString("subLevelID", mapped.first().toString());
                        }
                    }

                    attachments.set(i, attachment);
                }

                strandTag.put("blockAttachment", attachments);
            }
            if (strandTag.contains("points", Tag.TAG_LIST)) {
                ListTag points = strandTag.getList("points", Tag.TAG_LIST);

                for (int i = 0; i < points.size(); i++) {
                    ListTag point = points.getList(i);

                    double x = point.getDouble(0) + info.moveInfo().translationOffset().x;
                    double y = point.getDouble(1) + info.moveInfo().translationOffset().y;
                    double z = point.getDouble(2) + info.moveInfo().translationOffset().z;

                    point.setTag(0, DoubleTag.valueOf(x));
                    point.setTag(1, DoubleTag.valueOf(y));
                    point.setTag(2, DoubleTag.valueOf(z));

                    points.setTag(i, point);
                }
                strandTag.put("points", points);
            }

            info.tag().put("Strand", strandTag);
        }

        return info.tag();
    }
}