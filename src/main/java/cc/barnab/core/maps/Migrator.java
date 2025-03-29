package cc.barnab.core.maps;

import cc.barnab.ImageMap;
import cc.barnab.core.maps.migrate.ImageOnMapPlayerMapStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtInt;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.IdCountsState;
import net.minecraft.world.PersistentState;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

public class Migrator {
    private static final int MAP_SAVE_INTERVAL = 100;

    private static boolean isMigrating = false;

    private static String password = generatePassword();

    private static String generatePassword() {
        Random r = new Random();

        StringBuilder pass = new StringBuilder();

        String alphabet = "abcdefghijklmnopqrstuvwkyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        for (int i = 0; i < 16; i++) {
            pass.append(alphabet.charAt(r.nextInt(alphabet.length())));
        }

        return pass.toString();
    }

    public static String getPassword() {
        return password;
    }

//    public static int migrateImageOnMap(ServerCommandSource source, String pass) {
//        if (isMigrating) {
//            return -1;
//        }
//        if (!pass.equals(password)) {
//            return -2;
//        }
//
//        ServerWorld world = source.getWorld();
//        boolean wasSavingDisabled = world.savingDisabled;
//        world.savingDisabled = true;
//
//        // Roll pass
//        password = generatePassword();
//
//        source.sendFeedback(() -> Text.literal("Migrating maps from ImageOnMap...").formatted(Formatting.GOLD), false);
//
//        isMigrating = true;
//
//        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
//
//        File[] playerMapFiles = new File(FabricLoader.getInstance().getConfigDir().getParent().toFile(), "/plugins/ImageOnMap/maps").listFiles();
//        if (playerMapFiles == null)
//            return 0;
//
//        File[] imageFiles = new File(FabricLoader.getInstance().getConfigDir().getParent().toFile(), "/plugins/ImageOnMap/images").listFiles();
//        if (imageFiles == null)
//            return 0;
//
//        int migratedCount = 0;
//
//        int prevHighestIdSeen = 0;
//        int highestIdSeen = 0;
//
//        for (File playerMapFile : playerMapFiles) {
//            try {
//                source.sendFeedback(() -> Text.literal("Migrating maps from file: " + playerMapFile.getName()).formatted(Formatting.AQUA), true);
//                int migratedFromThisFile = 0;
//                UUID uuid = UUID.fromString(MapLoader.getFileNameWithoutExtension(playerMapFile));
//
//                ImageOnMapPlayerMapStore.MapList playerMapStore = mapper.readValue(playerMapFile, ImageOnMapPlayerMapStore.class).PlayerMapStore;
//
//                for (ImageOnMapPlayerMapStore.MapList.Map map : playerMapStore.mapList) {
//                    if (map.type.equals("SINGLE")) {
//                        long startTime = System.currentTimeMillis();
//                        source.sendFeedback(() ->
//                                Text.literal(String.format("Migrating single map %s", map.id)).formatted(Formatting.AQUA), true);
//
//                        Optional<File> imageFile = Arrays.stream(imageFiles).filter(f -> f.getName().equals("map" + map.mapID + ".png")).findFirst();
//                        if (imageFile.isEmpty()) {
//                            source.sendFeedback(() ->
//                                    Text.literal("Couldn't migrate single map " + map.id + ": Missing image").formatted(Formatting.YELLOW), true);
//                            continue;
//                        }
//
//                        BufferedImage image = ImageIO.read(imageFile.get());
//
//                        MapIdComponent mapId = new MapIdComponent(map.mapID);
//                        MapRenderer.drawMap(source.getWorld(), mapId, image, 0, 0, 0, 0);
//
//                        if (map.mapID > highestIdSeen)
//                            highestIdSeen = map.mapID;
//
//                        MapImage mapImage = MapImage.single(map.name, MapLoader.getNextId(uuid), map.mapID);
//                        MapLoader.addPlayerMap(uuid, mapImage);
//                        migratedCount++;
//                        migratedFromThisFile++;
//
//                        long timeElapsed = System.currentTimeMillis() - startTime;
//                        source.sendFeedback(() ->
//                                Text.literal(String.format("Migrated single map %s → %s in %.2fs", map.id, mapImage.getId(), (double)timeElapsed / 1000.0)).formatted(Formatting.GREEN), true);
//                    } else if (map.type.equals("POSTER")) {
//                        long startTime = System.currentTimeMillis();
//                        source.sendFeedback(() ->
//                                Text.literal(String.format("Migrating %dx%d poster map %s",
//                                        map.columns, map.rows, map.id)).formatted(Formatting.AQUA), true);
//
//                        boolean hadAllImages = true;
//                        for (int id : map.mapsIDs) {
//                            Optional<File> imageFile = Arrays.stream(imageFiles).filter(f -> f.getName().equals("map" + id + ".png")).findFirst();
//                            if (imageFile.isEmpty()) {
//                                hadAllImages = false;
//                                break;
//                            }
//                        }
//                        if (!hadAllImages) {
//                            source.sendFeedback(() ->
//                                    Text.literal(String.format("Couldn't migrate %dx%d poster map %s: Missing image(s)",
//                                            map.columns, map.rows, map.id)).formatted(Formatting.GREEN), true);
//                            continue;
//                        }
//
//                        ArrayList<Integer> mapIds = new ArrayList<>();
//                        for (int y = map.rows - 1; y >= 0; y--) {
//                            for (int x = 0; x < map.columns; x++) {
//                                int id = map.mapsIDs.get(y * map.columns + x);
//
//                                Optional<File> imageFile = Arrays.stream(imageFiles).filter(f -> f.getName().equals("map" + id + ".png")).findFirst();
//                                assert imageFile.isPresent();
//
//                                BufferedImage image = ImageIO.read(imageFile.get());
//
//                                MapIdComponent mapId = new MapIdComponent(id);
//                                MapRenderer.drawMap(source.getWorld(), mapId, image, 0, 0, 0, 0);
//                                mapIds.add(id);
//
//                                if (id > highestIdSeen)
//                                    highestIdSeen = id;
//
//                                if (mapIds.size() % 100 == 0 && map.columns * map.rows >= 120) {
//                                    source.sendFeedback(() ->
//                                            Text.literal(String.format("Map render progress: %d/%d", mapIds.size(), map.columns * map.rows))
//                                                    .formatted(Formatting.LIGHT_PURPLE), true);
//                                }
//                            }
//                        }
//
//                        MapImage mapImage = MapImage.poster(map.name, MapLoader.getNextId(uuid), map.columns, map.rows, mapIds);
//                        MapLoader.addPlayerMap(uuid, mapImage);
//                        migratedCount++;
//                        migratedFromThisFile++;
//                        long timeElapsed = System.currentTimeMillis() - startTime;
//                        source.sendFeedback(() ->
//                                Text.literal(String.format("Migrated %dx%d poster map %s → %s in %.2fs",
//                                        map.columns, map.rows, map.id, mapImage.getId(), (double)timeElapsed / 1000.0)).formatted(Formatting.GREEN), true);
//                    }
//
//                    if (prevHighestIdSeen < highestIdSeen) {
//                        prevHighestIdSeen = highestIdSeen;
//
//                        IdCountsState idCounts = world.getServer().getOverworld().getPersistentStateManager()
//                                .getOrCreate(IdCountsState.getPersistentStateType(), "idcounts");
//                        NbtCompound idCountsNbt = idCounts.writeNbt(new NbtCompound(), world.getRegistryManager());
//                        int mapCount = idCountsNbt.getInt("map");
//                        if (mapCount <= highestIdSeen) {
//                            idCountsNbt.putInt("map", highestIdSeen + 1);
//                            idCounts = IdCountsState.fromNbt(idCountsNbt, world.getRegistryManager());
//                            idCounts.markDirty();
//                            world.getServer().getOverworld().getPersistentStateManager()
//                                    .set("idcounts", idCounts);
//                        }
//                    }
//                }
//
//                // Save maps
//                world.getPersistentStateManager().save();
//                MapLoader.savePlayerMaps(uuid);
//
//                int finalMigratedFromThisFile = migratedFromThisFile;
//                source.sendFeedback(() ->
//                        Text.literal("Migrated " + finalMigratedFromThisFile + " maps from file: " + playerMapFile.getName()).formatted(Formatting.GREEN), true);
//            } catch (Exception e) {
//                e.printStackTrace();
//                source.sendFeedback(() ->
//                        Text.literal("Failed to migrate maps from file: " + playerMapFile.getName()).formatted(Formatting.RED), true);
//            }
//        }
//
//        // Save map data
//        source.sendFeedback(() -> Text.literal("Saving map data...").formatted(Formatting.AQUA), true);
//        source.getWorld().getPersistentStateManager().save();
//        MapLoader.savePlayerMaps(false);
//        source.sendFeedback(() -> Text.literal("Saved map data.").formatted(Formatting.GREEN), true);
//
//        world.savingDisabled = wasSavingDisabled;
//        world.getServer().saveAll(true, false, false);
//
//        isMigrating = false;
//        return migratedCount;
//    }
}
