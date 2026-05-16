package dev.egg;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;

public class AeronauticsCompat {

    private static final boolean LOADED =
            ModList.get().isLoaded("aeronautics");

    private static Class<?> GAS_PROVIDER;

    static {
        if (LOADED) {
            try {
                GAS_PROVIDER = Class.forName(
                        "dev.eriksonn.aeronautics.content.blocks.hot_air.BlockEntityLiftingGasProvider"
                );
            } catch (Exception ignored) {}
        }
    }

    public static void tryCreateBalloon(BlockEntity be) {
        if (GAS_PROVIDER == null) return;

        try {
            if (GAS_PROVIDER.isInstance(be)) {
                GAS_PROVIDER.getMethod("tryCreateBalloon")
                        .invoke(be);
            }
        }
        catch (Exception ignored) {}
    }
}