package cc.barnab.core.maps;

import cc.barnab.ImageMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class MapLoader {
    static HashMap<UUID, PlayerMapList> playerMapLists = new HashMap<>();

    public static void loadPlayerMaps() {
        File[] files = new File(FabricLoader.getInstance().getConfigDir().toFile(), "/ImageMap/player_maps").listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    try {
                        PlayerMapList mapList = PlayerMapList.fromFile(file);
                        playerMapLists.put(UUID.fromString(getFileNameWithoutExtension(file)), mapList);
                    } catch (Exception e) {
                        ImageMap.LOGGER.warn("Failed parsing player map file: " + file.getName());
                    }
                }
            }
        }

        ImageMap.LOGGER.info("Loaded " + playerMapLists.size() + " player map list" + (playerMapLists.size() == 1 ? "" : "s"));
    }

    public static void savePlayerMaps(boolean suppressLogs) {
        for (UUID uuid : playerMapLists.keySet()) {
            savePlayerMaps(uuid);
        }
        if (!suppressLogs)
            ImageMap.LOGGER.info("Saved " + playerMapLists.size() + " player map lists");
    }

    public static void savePlayerMaps(UUID uuid) {
        if (!playerMapLists.containsKey(uuid)) {
            return;
        }
        try {
            PlayerMapList mapList = playerMapLists.get(uuid);
            if (!mapList.isDirty)
                return;

            if (mapList.count() > 0) {
                File file = new File(FabricLoader.getInstance().getConfigDir().toFile(), "/ImageMap/player_maps/" + uuid.toString() + ".json");
                file.getParentFile().mkdirs();
                mapList.toFile(file);
            }
        } catch (Exception e) {
            ImageMap.LOGGER.warn("Failed saving player map list file for " + uuid);
        }
    }

    public static PlayerMapList getPlayerMaps(UUID uuid) {
        if (!playerMapLists.containsKey(uuid)) {
            playerMapLists.put(uuid, new PlayerMapList());
        }
        return playerMapLists.get(uuid);
    }

    public static String getNextId(UUID uuid) {
        List<MapImage> playerMaps = getPlayerMaps(uuid).mapList;
        return "Map-" + playerMaps.size();
    }

    public static void addPlayerMap(UUID uuid, MapImage mapImage) {
        PlayerMapList playerMapList = getPlayerMaps(uuid);
        playerMapList.mapList.add(mapImage);
        playerMapList.isDirty = true;
    }

    private static final Pattern ext = Pattern.compile("(?<=.)\\.[^.]+$");
    public static String getFileNameWithoutExtension(File file) {
        return ext.matcher(file.getName()).replaceAll("");
    }
}
