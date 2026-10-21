package dev.egg.dimensionalsable.mixin;

import com.mojang.datafixers.DataFixer;
import dev.egg.dimensionalsable.storage.MinecraftServerAccessor;
import dev.egg.dimensionalsable.storage.SharedOccupancyDataStorage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.Proxy;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin implements MinecraftServerAccessor {

    @Unique
    protected SharedOccupancyDataStorage dimensionalsable$sharedOccupancyDataStorage;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void dimensionalsable$sharedContainerStorage(Thread serverThread, LevelStorageSource.LevelStorageAccess storageSource, PackRepository packRepository, WorldStem worldStem, Proxy proxy, DataFixer fixerUpper, Services services, ChunkProgressListenerFactory progressListenerFactory, CallbackInfo ci) {
        this.dimensionalsable$sharedOccupancyDataStorage = new SharedOccupancyDataStorage(storageSource, fixerUpper);
    }

    @Override
    public SharedOccupancyDataStorage dimensionalsable$getSharedOccupancyDataStorage() {
        return this.dimensionalsable$sharedOccupancyDataStorage;
    }
}
