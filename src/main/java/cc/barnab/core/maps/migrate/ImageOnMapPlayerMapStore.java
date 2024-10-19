package cc.barnab.core.maps.migrate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

public class ImageOnMapPlayerMapStore {
    public MapList PlayerMapStore;

    public static class MapList {
        public List<Map> mapList = new ArrayList<>();

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Map {
            // Shared
            public String name;
            public String id;
            public String type;

            // Single map
            public int mapID;

            // Poster map
            public int rows = 1;
            public int columns = 1;
            public List<Integer> mapsIDs;
        }
    }
}
