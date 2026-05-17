package dev.egg.registries;

import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

import dev.egg.DimensionalSable.Pair;

public class BlockEntityRegistry {
    private final static HashMap<String, Vector<BlockEntityAccessor>> blockEntityFixerMap = new HashMap<>();

    public record BlockEntityInfo(CompoundTag tag,
                                  UUID oldSubLevelID,
                                  MoveInfo moveInfo){}

    public record MoveInfo(HashMap<UUID,Pair<UUID,Vec3i>> oldToNewSubLevelMap,
                           Vector3d translationOffset,
                           ServerLevel oldDimension,
                           ServerLevel newDimension){}

    public static CompoundTag modifyNBT(final BlockEntityInfo info)
    {
        var id = info.tag.getString("id");
        Vector<BlockEntityAccessor> accessors = blockEntityFixerMap.get(id);
        if (accessors != null) {
            for (BlockEntityAccessor accessor : accessors)
                accessor.modifyNBT(info);
        }

        return info.tag();
    }

    public static void PublishCompoundPosFixer(final String namespace, final Set<String> blockEntities, final Set<String> tags)
    {
        for (String blockEntity : blockEntities)
            PublishPosFixer(namespace, blockEntity, tags);
    }
    public static void PublishCompoundUUIDFixer(final String namespace, final Set<String> blockEntities, final Set<String> tags)
    {
        for (String blockEntity : blockEntities)
            PublishUUIDFixer(namespace, blockEntity, tags);
    }

    //for adding more complex conversion than a position
    public static void PublishCustomFixer(final String namespace, final String BlockEntityLocation, final BlockEntityAccessor blockEntityAccessor)
    {
        final String location = namespace + ":" + BlockEntityLocation;
        blockEntityFixerMap.putIfAbsent(location,new Vector<>());
        blockEntityFixerMap.get(location).add(blockEntityAccessor);
    }
    //for adding a simple position fix in block entity nbt
    public static void PublishPosFixer(final String namespace, final String BlockEntityLocation, final Set<String> tags)
    {
        PublishCustomFixer(namespace, BlockEntityLocation, new PosBlockEntityAccessor(tags));
    }
    public static void PublishConnectionFixer(final String namespace, final String BlockEntityLocation, final String subLevelTag, final String positionTag)
    {
        PublishCustomFixer(namespace, BlockEntityLocation, new ConnectionBlockEntityAccessor(subLevelTag, positionTag));
    }
    //for adding a simple UUID fix in block entity nbt (old subLevelID -> new subLevelID)
    public static void PublishUUIDFixer(final String namespace, final String BlockEntityLocation, final Set<String> tags)
    {
        PublishCustomFixer(namespace, BlockEntityLocation, new UUIDBlockEntityAccessor(tags));
    }
}
