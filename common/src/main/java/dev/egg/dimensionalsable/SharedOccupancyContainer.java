package dev.egg.dimensionalsable;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import net.minecraft.nbt.CompoundTag;

import java.util.BitSet;

public class SharedOccupancyContainer {

    public static final SharedOccupancyContainer INSTANCE = new SharedOccupancyContainer("shared_sublevel_container");
    public static final String SHARED_OCCUPANCY_DIR = "shared_containers";
    private final BitSet occupancy;

    private final String name;

    private SharedOccupancyContainer(String name) {
        this.name = name;
        this.occupancy = new BitSet((1 << SubLevelContainer.DEFAULT_LOG_SIZE_LENGTH) * (1 << SubLevelContainer.DEFAULT_LOG_SIZE_LENGTH));
    }

    public BitSet getOccupancy() { return occupancy; }
    public String getName() { return name; }

    public CompoundTag save() {
        CompoundTag compoundtag = new CompoundTag();
        compoundtag.putLongArray("occupancy", occupancy.toLongArray());
        return compoundtag;
    }

    public void load(CompoundTag compoundtag) {
        long[] occupancies = compoundtag.getLongArray("occupancy");
        occupancy.clear();
        occupancy.or(BitSet.valueOf(occupancies));
    }

    public static void bootstrap() {}
}
