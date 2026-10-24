package dev.egg.dimensionalsable;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class DimensionalSableFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        DimensionalSable.init();

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, selection) -> Commands.register(dispatcher));
    }
}