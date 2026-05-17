package dev.egg;

import com.mojang.logging.LogUtils;
import dev.egg.registries.BlockEntityRegistry;
import dev.egg.registries.blockentities.create.DisplayLinkBlockEntity;
import dev.egg.registries.blockentities.create.MechanicalPistonBlockEntity;
import dev.egg.registries.blockentities.create.BearingBlockEntity;
import dev.egg.registries.blockentities.simulated.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

import java.util.Set;

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

        if (ModList.get().isLoaded("simulated")) {

            //this one was fairly easy, so I did it first. I recommend using it as an example for making any new block entity accessors
            //(This one also doesn't use the built in connection fixer because "Goal" is a long for some reason)
            BlockEntityRegistry.PublishCustomFixer("simulated","spring", new SpringBlockEntity());
            BlockEntityRegistry.PublishCustomFixer("simulated","rope_connector", new RopeConnectorBlockEntity());
            BlockEntityRegistry.PublishCustomFixer("simulated","rope_winch", new RopeConnectorBlockEntity()); //this has the same nbt as rope connector
            BlockEntityRegistry.PublishConnectionFixer("simulated","docking_connector", "OtherConnectorSubLevelId", "OtherConnector");
            BlockEntityRegistry.PublishConnectionFixer("simulated","swivel_bearing", "SubLevelID", "SwivelPlate");
            BlockEntityRegistry.PublishConnectionFixer("simulated","swivel_bearing_link_block", "ParentSubLevelId", "ParentPos");
        }
        if (ModList.get().isLoaded("create")) {
            BlockEntityRegistry.PublishCustomFixer("create","mechanical_bearing", new BearingBlockEntity());
            BlockEntityRegistry.PublishCustomFixer("create","windmill_bearing", new BearingBlockEntity());
            BlockEntityRegistry.PublishCustomFixer("create","clockwork_bearing", new BearingBlockEntity());
            BlockEntityRegistry.PublishCustomFixer("create","mechanical_piston", new MechanicalPistonBlockEntity()); //currently places in the wrong position, gotta fix that
            //block entities with "Source", this is taken from the Create github
            //Copyright (c) The Create Team / The Creators of Create
            BlockEntityRegistry.PublishCompoundPosFixer("create", Set.of("adjustable_chain_gearshift", "backtank", "belt", "clockwork_bearing", "clutch", "cuckoo_clock",
                    "deployer", "drill", "elevator_pulley", "encased_fan", "flap_display", "fluid_valve", "flywheel",
                    "gantry_pinion", "gearbox", "gearshift", "hand_crank", "hose_pulley", "large_water_wheel", "mechanical_arm",
                    "mechanical_bearing", "mechanical_crafter", "mechanical_mixer", "mechanical_piston", "mechanical_press",
                    "mechanical_pump", "millstone", "powered_shaft", "rope_pulley", "saw", "sequenced_gearshift",
                    "simple_kinetic", "speedometer", "stressometer", "valve_handle", "water_wheel", "weighted_ejector",
                    "windmill_bearing"
            ), Set.of("Source"));

            BlockEntityRegistry.PublishPosFixer("create","belt", Set.of("Controller"));
            BlockEntityRegistry.PublishCompoundPosFixer("create", Set.of("item_vault", "fluid_tank"), Set.of("LastKnownPos", "Controller"));
            BlockEntityRegistry.PublishCustomFixer("create", "display_link", new DisplayLinkBlockEntity());
            BlockEntityRegistry.PublishCompoundPosFixer("create", Set.of("drill", "saw"), Set.of("Breaking"));
            BlockEntityRegistry.PublishCompoundPosFixer("create", Set.of("rope_pulley", "elevator_pulley"), Set.of("MirrorChildren")); //not sure if this is everything I need to do
            BlockEntityRegistry.PublishPosFixer("create", "powered_shaft", Set.of("EnginePos"));
        }
        if (ModList.get().isLoaded("aeronautics")) {
            BlockEntityRegistry.PublishCustomFixer("aeronautics","propeller_bearing", new BearingBlockEntity());
            BlockEntityRegistry.PublishCustomFixer("aeronautics","gyroscopic_propeller_bearing", new BearingBlockEntity());

            //not entirely sure how necessary it is to fix "Source", but I'm doing it anyway
            BlockEntityRegistry.PublishCompoundPosFixer("aeronautics", Set.of("propeller_bearing", "gyroscopic_propeller_bearing",
                    "smart_propeller", "wooden_propeller", "andesite_propeller"
            ), Set.of("Source"));
        }
        if (ModList.get().isLoaded("offroad")) {
            BlockEntityRegistry.PublishCustomFixer("offroad","borehead_bearing", new BearingBlockEntity());
            BlockEntityRegistry.PublishPosFixer("offroad","borehead_bearing", Set.of("Source"));
        }
    }

    public record Pair<A, B>(A first, B second) {
        public static <A, B> Pair<A, B> of(A first, B second) {
            return new Pair<>(first, second);
        }
    }
}
