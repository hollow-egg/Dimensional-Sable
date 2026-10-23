package dev.egg.dimensionalsable;

import com.mojang.logging.LogUtils;
import dev.egg.dimensionalsable.storage.MinecraftServerAccessor;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;


public class DimensionalSable {
    public static final String MODID = "dimensionalsable";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static void init() {
    }

    public static void loadSharedSubLevelContainer(MinecraftServer server) {
        ((MinecraftServerAccessor) server).dimensionalsable$getSharedOccupancyDataStorage().load(SharedOccupancyContainer.INSTANCE);
    }

    public static void saveSharedSubLevelContainer(MinecraftServer server) {
        ((MinecraftServerAccessor) server).dimensionalsable$getSharedOccupancyDataStorage().save(SharedOccupancyContainer.INSTANCE);
    }
}
