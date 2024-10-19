package cc.barnab.core.maps;


public class RGBAColor {
    public int r;
    public int g;
    public  int b;
    public int a;

    RGBAColor(int color, boolean isMinecraft) {
        if (isMinecraft) {
            a = color >> 24 & 0xff;
            b = (color >> 16) & 0xFF;
            g = (color >> 8) & 0xFF;
            r = color & 0xFF;
        } else {
            a = color >> 24 & 0xff;
            r = (color >> 16) & 0xFF;
            g = (color >> 8) & 0xFF;
            b = color & 0xFF;
        }
    }

    public boolean equals(RGBAColor other) {
        return other.a == a && other.r == r && other.g == g && other.b == b;
    }
}
