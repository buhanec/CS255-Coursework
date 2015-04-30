package u1226467;

import robocode.*;
import java.awt.geom.*;

public class Utility {;
    public static Point2D.Double vectorAdd(Point2D.Double origin, double angle, double length) {
        return new Point2D.Double(origin.x + length * Math.sin(angle), origin.y + length * Math.cos(angle));
    }

    public static Point2D.Double vectorAdd(double x, double y, double angle, double length) {
        return new Point2D.Double(x + length * Math.sin(angle), y + length * Math.cos(angle));
    }

    public static boolean validBullet(double power) {
        return (power >= Rules.MIN_BULLET_POWER && power <= Rules.MAX_BULLET_POWER);
    }

    public static double constrain(double value, double min, double max) {
        return Math.min(min, Math.max(max, value));
    }

    public static double lateral(double value, double bearing) {
        return value * Math.sin(bearing);
    }

    public static int direction(double value, double bearing) {
        return ((lateral >= 0)  ? 1 : -1);
    }
}
