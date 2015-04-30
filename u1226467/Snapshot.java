package u1226467;

import robocode.*;
import java.awt.geom.*;

public class Snapshot {
    public final String name;
    public final double heading;
    public final double energy;
    public final double speed;
    public final Point2D.Double location;

    Snapshot(ScannedRobotEvent e, Robot r) {
        name = e.getName();
        heading = e.getHeadingRadians();
        energy = e.getEnergy();
        speed = e.getVelocity();
        location = Utility.vectorAdd(r.getX(), r.getY(), e.getBearingRadians(), e.getDistance());
    }

    Snapshot(String name, RobotStatus s) {
        this.name = name;
        heading = s.getHeading();
        energy = s.getEnergy();
        speed = s.getVelocity();
        location = new Point2D.Double(s.getX(), s.getY());
    }

    Snapshot(RobotStatus s) {
        this(null, s);
    }

    Snapshot(Robot r) {
        name = r.getName();
        heading = r.getHeading();
        energy = r.getEnergy();
        speed = r.getVelocity();
        location = new Point2D.Double(r.getX(), r.getY());
    }
}
