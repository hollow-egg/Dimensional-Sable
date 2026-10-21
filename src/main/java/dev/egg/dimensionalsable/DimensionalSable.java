package dev.egg.dimensionalsable;

import com.mojang.logging.LogUtils;
import dev.egg.dimensionalsable.storage.MinecraftServerAccessor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

import java.util.Objects;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(DimensionalSable.MODID)
public class DimensionalSable {
    public static final String MODID = "dimensional_sable";
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public DimensionalSable(IEventBus modEventBus, ModContainer modContainer) {
        final IEventBus neoBus = NeoForge.EVENT_BUS;
        neoBus.addListener(Commands::registerCommands);
    }

    @EventBusSubscriber(modid = MODID)
    public static class CommonModEvents {
        @SubscribeEvent
        public static void serverStart(ServerStartingEvent event) {
            LOGGER.info("Loading Shared Occupancy Data");
            ((MinecraftServerAccessor) event.getServer()).dimensionalsable$getSharedOccupancyDataStorage().load(SharedOccupancyContainer.INSTANCE);
        }

        @SubscribeEvent
        public static void levelSave(LevelEvent.Save event) {
            LOGGER.info("Saving Shared Occupancy Data");
            ((MinecraftServerAccessor) Objects.requireNonNull(event.getLevel().getServer())).dimensionalsable$getSharedOccupancyDataStorage().save(SharedOccupancyContainer.INSTANCE);
        }
    }
}
