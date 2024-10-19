package cc.barnab.core;

import cc.barnab.ImageMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ImageMapConfig {

    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping().setLenient().setPrettyPrinting()
            .create();

    public int minPermLevel = 2;

    public static ImageMapConfig loadOrCreateConfig() {
        try {
            ImageMapConfig config;
            File configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "/ImageMap/config.json");

            if (configFile.exists()) {
                String json = IOUtils.toString(new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8));

                config = GSON.fromJson(json, ImageMapConfig.class);
            } else {
                config = new ImageMapConfig();
            }

            saveConfig(config);
            return config;
        }
        catch(IOException exception) {
            ImageMap.LOGGER.error("Something went wrong while reading config!");
            exception.printStackTrace();
            return new ImageMapConfig();
        }
    }

    public static void saveConfig(ImageMapConfig config) {
        File configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "/ImageMap/config.json");
        configFile.getParentFile().mkdirs();
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8));
            writer.write(GSON.toJson(config));
            writer.close();
        } catch (Exception e) {
            ImageMap.LOGGER.error("Something went wrong while saving config!");
            e.printStackTrace();
        }
    }
}
