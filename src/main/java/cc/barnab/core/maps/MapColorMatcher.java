package cc.barnab.core.maps;

import cc.barnab.ImageMap;
import net.minecraft.block.MapColor;

import java.awt.*;

public class MapColorMatcher {
    public static final RGBAColor[] mapColors = new RGBAColor[256];
    private static boolean hasLoaded = false;

    public static byte findClosestColor(int color) {
        if (!hasLoaded) {
            loadColors();
        }

        RGBAColor c = new RGBAColor(color, false);

        // Short-circuit transparent pixels
        if (c.a < 64)
            return 0;

        int closest = 0;
        int dist = getDistance(c, mapColors[0]);

        for (int i = 1; i < 256; i++) {
            RGBAColor mapColor = mapColors[i];
            if (c.equals(mapColor))
                return (byte) i;

            int newDist = getDistance(c, mapColor);
            if (newDist < dist) {
                closest = i;
                dist = newDist;
            }
        }

        return (byte) closest;
    }

    private static int getDistance(RGBAColor c1, RGBAColor c2) {
        // Optimize color distance calculation by removing floating point math
        int rSum = c1.r + c2.r; // Use sum instead of mean for no division
        int r = c1.r - c2.r;
        int g = c1.g - c2.g;
        int b = c1.b - c2.b;
        int a = c1.a - c2.a;
        // All weights are 512x their original to avoid floating point division
        int weightR = 1024 + rSum;
        int weightG = 2048;
        int weightB = 1024 + (255*2 - rSum);
        int weightA = 2048;

        // Division by 256 here is unnecessary as this won't change the result of the sort
        return weightR * r * r + weightG * g * g + weightB * b * b + weightA * a * a;
    }

    private static void loadColors() {
        for (int i = 0; i < 256; i++) {
            int color = MapColor.getRenderColor(i);
            mapColors[i] = new RGBAColor(color, true);
        }

        hasLoaded = true;
    }
}
