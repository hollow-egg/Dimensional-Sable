package dev.egg.dimensionalsable.storage;

import com.mojang.datafixers.DataFixer;
import dev.egg.dimensionalsable.DimensionalSable;
import dev.egg.dimensionalsable.SharedOccupancyContainer;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.storage.FileNameDateFormatter;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class SharedOccupancyDataStorage {
    private final File occupancyDir;
    protected final DataFixer fixerUpper;
    private static final DateTimeFormatter FORMATTER = FileNameDateFormatter.create();

    public SharedOccupancyDataStorage(LevelStorageSource.LevelStorageAccess levelStorageAccess, DataFixer fixerUpper) {
        this.fixerUpper = fixerUpper;
        this.occupancyDir = levelStorageAccess
                .getLevelPath(LevelResource.ROOT)
                .resolve(SharedOccupancyContainer.SHARED_OCCUPANCY_DIR)
                .toFile();
        this.occupancyDir.mkdirs();
    }

    public void save(SharedOccupancyContainer container) {
        try {
            CompoundTag compoundtag = container.save();
            Path path = this.occupancyDir.toPath();
            Path path1 = Files.createTempFile(path, container.getName() + "-", ".dat");
            NbtIo.writeCompressed(compoundtag, path1);
            Path path2 = path.resolve(container.getName() + ".dat");
            Path path3 = path.resolve(container.getName() + ".dat_old");
            Util.safeReplaceFile(path2, path1, path3);
        } catch (Exception exception) {
            DimensionalSable.LOGGER.warn("Failed to save shared container data for {}", container.getName());
        }
    }

    private void backup(SharedOccupancyContainer container) {
        Path path = this.occupancyDir.toPath();
        Path path1 = path.resolve(container.getName() + ".dat");
        Path path2 = path.resolve(container.getName() + "_corrupted_" + LocalDateTime.now().format(FORMATTER) + ".dat");
        if (Files.isRegularFile(path1)) {
            try {
                Files.copy(path1, path2, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            } catch (Exception exception) {
                DimensionalSable.LOGGER.warn("Failed to copy the shared container .dat file for {}", container.getName(), exception);
            }
        }
    }

    private Optional<CompoundTag> load(SharedOccupancyContainer container, String suffix) {
        File file = new File(this.occupancyDir, container.getName() + suffix);
        if (file.exists() && file.isFile()) {
            try {
                return Optional.of(NbtIo.readCompressed(file.toPath(), NbtAccounter.unlimitedHeap()));
            } catch (Exception exception) {
                DimensionalSable.LOGGER.warn("Failed to load shared container data for {}", container.getName());
            }
        }

        return Optional.empty();
    }

    public Optional<CompoundTag> load(SharedOccupancyContainer container) {
        Optional<CompoundTag> optional = this.load(container, ".dat");
        if (optional.isEmpty()) {
            this.backup(container);
        }

        return optional.or(() -> this.load(container, ".dat_old")).map(compoundTag -> {
            container.load(compoundTag);
            return compoundTag;
        });
    }
}
