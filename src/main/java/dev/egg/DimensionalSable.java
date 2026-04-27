package dev.egg;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(DimensionalSable.MODID)
public class DimensionalSable {
    public static final String MODID = "dimensional_sable";
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public DimensionalSable(IEventBus modEventBus, ModContainer modContainer) {
        final IEventBus neoBus = NeoForge.EVENT_BUS;
        neoBus.addListener(dev.egg.Commands::registerCommands);
    }
}
