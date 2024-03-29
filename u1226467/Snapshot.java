package u1226467;

import robocode.*;

public class Snapshot extends VectorPoint {
    protected final String name;
    protected final double energy;
    protected final long time;

    Snapshot(ScannedRobotEvent e, Robot r) {
        super(Utility.vectorAdd(r.getX(), r.getY(),
              Utility.fixAngle(Math.toRadians(r.getHeading())
                               + e.getBearingRadians()), e.getDistance()),
              e.getHeadingRadians(),
              e.getVelocity());
        name = e.getName();
        energy = e.getEnergy();
        time = e.getTime();
    }

    Snapshot(Snapshot snap) {
        super(snap);
        name = snap.getName();
        energy = snap.getEnergy();
        time = snap.getTime();
    }

    Snapshot(String name, RobotStatus s) {
        super(s.getX(), s.getY(), s.getHeadingRadians(), s.getVelocity());
        this.name = name;
        energy = s.getEnergy();
        time = s.getTime();
    }

    Snapshot(RobotStatus s) {
        this(null, s);
    }

    Snapshot(Robot r) {
        super(r.getX(), r.getY(), Math.toRadians(r.getHeading()),
              r.getVelocity());
        name = r.getName();
        energy = r.getEnergy();
        time = r.getTime();
    }

    public long getTime() {
        return time;
    }

    public double getEnergy() {
        return energy;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return name+" " +"("+Math.round(x)+","+Math.round(y)+") heading "
            +Math.round(Math.toDegrees(heading))+" going "+Math.round(speed)
            +" at "+time;
    }
}
