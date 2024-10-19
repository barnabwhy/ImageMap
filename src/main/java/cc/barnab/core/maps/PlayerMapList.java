package cc.barnab.core.maps;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class PlayerMapList {
    public List<MapImage> mapList = new ArrayList<>();
    public transient boolean isDirty = false;

    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping().setLenient().setPrettyPrinting()
            .create();

    public static PlayerMapList fromFile(File file) throws IOException {
        PlayerMapList self;
        if (file.exists()) {
            String json = IOUtils.toString(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));

            self = GSON.fromJson(json, PlayerMapList.class);
        } else {
            self = new PlayerMapList();
        }
        return self;
    }

    public void toFile(File file) throws IOException {
        try (Writer writer = new FileWriter(file)) {
            Gson gson = new GsonBuilder().create();
            gson.toJson(this, writer);
        }
    }

    public int count() {
        return mapList.size();
    }
}
