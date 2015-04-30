package u1226467;

import robocode.*;
import java.util.*;
import java.awt.geom.*;
import u1226467.Snapshot;

public class RadarSnapshot {
    public final double lateralSpeed;

    Snapshot(ScannedRobotEvent event, Robot robot) {
        super(event, robot);
        double lateralSpeed = event.getVelocity() * Math.sin(e.robot.getHeadingRadians() - heading);
    }
}
