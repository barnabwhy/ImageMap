package cc.barnab.core.maps;

import cc.barnab.ImageMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class MapRenderer {
    public static CompletableFuture<BufferedImage> downloadImage(String imageUrl) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL(imageUrl);
                return ImageIO.read(url);
            } catch(Exception ignored) {
                return null;
            }
        });
    }

    public static CompletableFuture<MapImage> renderMapImage(ServerPlayerEntity player, ServerWorld world, BufferedImage image, int width, int height, MapFillMode mode) {
        boolean wasSavingDisabled = world.savingDisabled;
        world.savingDisabled = true;

        int pixelWidth = width * 128;
        int pixelHeight = height * 128;

        int imageOffsetX = 0;
        int imageOffsetY = 0;

        int mapOffsetX = 0;
        int mapOffsetY = 0;

        int w = image.getWidth();
        int h = image.getHeight();
        double imageAspect = (double) (w) / (double) (h);
        double mapAspect = (double) (pixelWidth) / (double) (pixelHeight);

        switch (mode) {
            case COVER -> {
                if (mapAspect >= imageAspect) {
                    pixelHeight = (int) (pixelWidth / imageAspect);
                    imageOffsetY = (pixelHeight - height * 128) / 2;
                } else {
                    pixelWidth = (int) (pixelHeight * imageAspect);
                    imageOffsetX = (pixelWidth - width * 128) / 2;
                }
            }
            case CONTAIN -> {
                if (mapAspect < imageAspect) {
                    pixelHeight = (int) (pixelWidth / imageAspect);
                    mapOffsetY = (height * 128 - pixelHeight) / 2;
                } else {
                    pixelWidth = (int) (pixelHeight * imageAspect);
                    mapOffsetX = (width * 128 - pixelWidth) / 2;
                }
            }
        };

        int finalPixelWidth = pixelWidth;
        int finalPixelHeight = pixelHeight;
        int finalImageOffsetX = imageOffsetX;
        int finalImageOffsetY = imageOffsetY;
        int finalMapOffsetX = mapOffsetX;
        int finalMapOffsetY = mapOffsetY;
        return CompletableFuture.supplyAsync(() -> {
            BufferedImage scaled = new BufferedImage(finalPixelWidth, finalPixelHeight, image.getType());

            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

            g.drawImage(image, 0, 0, finalPixelWidth, finalPixelHeight, 0, 0, image.getWidth(), image.getHeight(), null);
            g.dispose();

            return scaled;
        })
        .thenApplyAsync(scaled -> {
            // Draw maps from bottom left to top right
            ArrayList<Integer> mapIds = new ArrayList<>();

            for (int y = height - 1; y >= 0; y--) {
                for (int x = 0; x < width; x++) {
                    int imageX = x * 128 + finalImageOffsetX;
                    int imageY = y * 128 + finalImageOffsetY;
                    int fullMapX = finalMapOffsetX - (x * 128);
                    int fullMapY = finalMapOffsetY - (y * 128);

                    if (fullMapX > 128) fullMapX = 128;
                    if (fullMapY > 128) fullMapY = 128;

                    if (fullMapX < 0) {
                        fullMapX = 0;
                        imageX -= finalMapOffsetX % 128;
                    }
                    if (fullMapY < 0) {
                        fullMapY = 0;
                        imageY -= finalMapOffsetY % 128;
                    }

                    MapIdComponent mapId = world.increaseAndGetMapId();
                    drawMap(world, mapId, scaled, imageX, imageY, fullMapX, fullMapY);
                    mapIds.add(mapId.id());
                }
            }

            String id = MapLoader.getNextId(player.getUuid());

            MapImage mapImage;
            if (mapIds.size() == 1) {
                mapImage = MapImage.single("Single Map", id, mapIds.getFirst());
            } else {
                mapImage = MapImage.poster("Poster Map", id, width, height, mapIds);
            }

            // Save map data
            world.getPersistentStateManager().save();
            world.savingDisabled = wasSavingDisabled;

            return mapImage;
        });
    }

    public static void drawMap(ServerWorld world, MapIdComponent mapId, BufferedImage image, int imageX, int imageY, int mapX, int mapY) {
        int endX = mapX + (image.getWidth() - imageX);
        int endY = mapY + (image.getHeight() - imageY);

        if (endX > 128)
            endX = 128;
        if (endY > 128)
            endY = 128;

        MapState state = MapState.of((byte)0, true, world.getRegistryKey());

        for (int y = mapY; y < endY; y++) {
            for (int x = mapX; x < endX; x++) {
                int pixelColor = image.getRGB(x + imageX - mapX, y + imageY - mapY);

                byte color = MapColorMatcher.findClosestColor(pixelColor);
                state.setColor(x, y, color);
            }
        }

        world.putMapState(mapId, state);

        PersistentStateManager stateManager = world.getPersistentStateManager();
        PersistentStateType<MapState> mapStateType = MapState.createStateType(mapId);

        File mapDatBackup = new File(FabricLoader.getInstance().getConfigDir().toFile(), "/ImageMap/map_backup/map_"+ mapId.id() + ".dat");
        mapDatBackup.getParentFile().mkdirs();
        NbtCompound mapNbt = stateManager.encode(mapStateType, state, stateManager.registries.getOps(NbtOps.INSTANCE));
        try {
            NbtIo.writeCompressed(mapNbt, mapDatBackup.toPath());
        } catch (IOException ignored) {
            // Mark as dirty if write fails, so it will still get saved
            state.markDirty();
            return;
        }

        // Cheat saving so we can do it faster
        Path mapDatPath = stateManager.getFile(mapStateType.id());
        try {
            Files.copy(mapDatBackup.toPath(), mapDatPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // Mark as dirty if copy fails, so it will still get saved
            state.markDirty();
        }
    }

    private static <T> CompletableFuture<List<T>> coalesceFutures(List<CompletableFuture<T>> futures) {
        CompletableFuture[] cfs = futures.toArray(new CompletableFuture[futures.size()]);

        return CompletableFuture.allOf(cfs)
                .thenApply(ignored -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList())
                );
    }
}
