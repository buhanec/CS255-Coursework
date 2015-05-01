package u1226467;

import robocode.*;

public class Utility {;
    public static Point vectorAdd(Point origin, double angle, double length) {
        return new Point(origin.x + length * Math.sin(angle), origin.y + length * Math.cos(angle));
    }

    public static Point vectorAdd(double x, double y, double angle, double length) {
        return new Point(x + length * Math.sin(angle), y + length * Math.cos(angle));
    }

    public static boolean validBullet(double power) {
        return (power >= Rules.MIN_BULLET_POWER && power <= Rules.MAX_BULLET_POWER);
    }

    public static double constrain(double value, double min, double max) {
        return Math.min(min, Math.max(max, value));
    }

    public static boolean containedii(double value, double min, double max) {
        return ((value >= min) && (value <= max));
    }

    public static boolean containedin(double value, double min, double max) {
        return ((value >= min) && (value < max));
    }

    public static boolean containedni(double value, double min, double max) {
        return ((value > min) && (value <= max));
    }

    public static boolean containednn(double value, double min, double max) {
        return ((value > min) && (value < max));
    }

    public static double lateral(double value, double bearing) {
        return value * Math.sin(bearing);
    }

    public static int direction(double value, double bearing) {
        return ((lateral(value, bearing) >= 0)  ? 1 : -1);
    }
}
