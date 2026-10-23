package dev.egg.dimensionalsable;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(DimensionalSable.MODID)
public class DimensionalSableNeoForge {
    public DimensionalSableNeoForge(IEventBus modEventBus, ModContainer modContainer) {

        DimensionalSable.init();

        final IEventBus neoBus = NeoForge.EVENT_BUS;
        neoBus.register(Events.class);
    }

    public static class Events {
        @SubscribeEvent
        public static void serverStart(ServerStartingEvent event) {
            DimensionalSable.loadSharedSubLevelContainer(event.getServer());
        }

        @SubscribeEvent
        public static void levelSave(LevelEvent.Save event) {
            DimensionalSable.saveSharedSubLevelContainer(event.getLevel().getServer());
        }

        @SubscribeEvent
        public static void registerCommands(RegisterCommandsEvent event){
            Commands.register(event.getDispatcher());
        }
    }
}
