package cc.barnab.core.maps;

import java.util.ArrayList;
import java.util.List;

public class MapImage {
    private String name;
    private String id;
    private MapImageType type;
    private int width = 1;
    private int height = 1;

    private List<Integer> mapIds;

    private MapImage() { }

    public static MapImage single(String name, String id, int mapId) {
        MapImage self = new MapImage();
        self.name = name;
        self.id = id;
        self.type = MapImageType.SINGLE;
        self.width = 1;
        self.height = 1;
        self.mapIds = List.of(mapId);

        return self;
    }
    public static MapImage poster(String name, String id, int width, int height, List<Integer> mapIds) {
        MapImage self = new MapImage();
        self.name = name;
        self.id = id;
        self.type = MapImageType.POSTER;
        self.width = width;
        self.height = height;
        self.mapIds = mapIds;

        return self;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public MapImageType getType() {
        return type;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public List<Integer> getMapIds() {
        return mapIds;
    }
}