package dev.egg.dimensionalsable.utils;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ryanhcode.sable.Sable;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.List;

public class BlockEntityTransformer {

    public static final Codec<Vector3d> VEC3D = Codec.DOUBLE.listOf().xmap(
            list -> new Vector3d(list.get(0), list.get(1), list.get(2)),
            vec -> List.of(vec.x, vec.y, vec.z)
    );

    public static final Codec<Quaterniond> QUATERNION = Codec.DOUBLE.listOf().xmap(
            list -> new Quaterniond(list.get(0), list.get(1), list.get(2), list.get(3)),
            q -> List.of(q.x, q.y, q.z, q.w)
    );

    public static final Codec<BlockEntityTransformer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Level.RESOURCE_KEY_CODEC.fieldOf("level").forGetter((transformer) -> transformer.getOldLevel().dimension()),
            VEC3D.fieldOf("old_position").forGetter(BlockEntityTransformer::getOldPosition),
            VEC3D.fieldOf("new_position").forGetter(BlockEntityTransformer::getNewPosition),
            QUATERNION.fieldOf("rotation").forGetter(BlockEntityTransformer::getRotation)
    ).apply(instance, BlockEntityTransformer::new));

    private static final String TRANSFORM_TAG = "dimensional_sable_transform";

    private final ResourceKey<Level> oldLevelKey;
    private Level oldLevel;
    private final Vector3d oldPosition;
    private final Vector3d newPosition;
    private final Quaterniond rotation;
    public Level getOldLevel() {
        return this.oldLevel;
    }
    public Vector3d getOldPosition() {
        return this.oldPosition;
    }
    public Vector3d getNewPosition() {
        return this.newPosition;
    }
    public Quaterniond getRotation() {
        return this.rotation;
    }

    private BlockEntityTransformer(Level oldLevel, Vector3d oldPosition, Vector3d newPosition, Quaterniond rotation) {
        this.oldLevelKey = oldLevel.dimension();
        this.oldLevel = oldLevel;
        this.oldPosition = oldPosition;
        this.newPosition = newPosition;
        this.rotation = rotation;
    }
    private BlockEntityTransformer(ResourceKey<Level> oldLevelKey, Vector3d oldPosition, Vector3d newPosition, Quaterniond rotation) {
        this.oldLevelKey = oldLevelKey;
        this.oldPosition = oldPosition;
        this.newPosition = newPosition;
        this.rotation = rotation;
    }

    public static @Nullable BlockEntityTransformer getTransform(CompoundTag tag, MinecraftServer server) {
        if (!tag.contains(TRANSFORM_TAG)) return null;

        CompoundTag transformTag = tag.getCompound(TRANSFORM_TAG);
        DataResult<Pair<BlockEntityTransformer, Tag>> result = CODEC.decode(NbtOps.INSTANCE, transformTag);
        if (result.isSuccess()) {
            BlockEntityTransformer transformer = result.getOrThrow().getFirst();
            transformer.oldLevel = server.getLevel(transformer.oldLevelKey);
            return transformer;
        }
        return null;
    }

    public static Tag createTransformTag(Level oldLevel, Vector3d oldPosition, Vector3d newPosition, Quaterniond rotation) {
        BlockEntityTransformer transformer = new BlockEntityTransformer(oldLevel, oldPosition, newPosition, rotation);
        DataResult<Tag> result = CODEC.encode(transformer, NbtOps.INSTANCE, new CompoundTag());
        return result.getOrThrow();
    }
    public static void addTransformTag(CompoundTag compoundTag, Tag tag) {
        compoundTag.put(TRANSFORM_TAG, tag);
    }

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
        }
        else if (current instanceof ListTag listTag) {
            if (listTag.size() == 3 && listTag.getElementType() == Tag.TAG_DOUBLE) {
                Vector3d pos = new Vector3d(listTag.getDouble(0), listTag.getDouble(1), listTag.getDouble(2));

                if (Sable.HELPER.isInPlotGrid(oldLevel, pos)) return; // do not offset nbt positions in sublevel space

                pos.sub(oldPosition);
                rotation.transform(pos);
                pos.add(newPosition);

                listTag.set(0, DoubleTag.valueOf(pos.x));
                listTag.set(1, DoubleTag.valueOf(pos.y));
                listTag.set(2, DoubleTag.valueOf(pos.z));
            }
            else {
                if (listTag.getElementType() == Tag.TAG_COMPOUND ||  listTag.getElementType() == Tag.TAG_LIST) {
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
