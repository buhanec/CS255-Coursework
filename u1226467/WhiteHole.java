package u1226467;

import robocode.*;
import java.util.*;

public class WhiteHole implements Pilot {
    private State state;
    private Rectangle arena;
    private Robot robot;

    private long time;

    private Map<Point, Double> extra;

    private DirectedPoint hit;

    WhiteHole(Robot robot, State state, Rectangle arena) {
        this.robot = robot;
        this.state = state;
        this.arena = arena;

        hit = null;
        time = 0;

        extra = new HashMap<Point, Double>();
    }

    // reset the hit on the start of a new round
    public void update(long time) {
        if (time < this.time) {
            hit = null;
        }
        this.time = time;
    }

    // calculates the gravity at a specific point
    private VectorPoint gravity(Point p) {
        double fx = 0;
        double fy = 0;
        VectorPoint self = new VectorPoint(p, 0, 0);

        // we don't really want perfect precision, errors result in randomisation
        for (Snapshot snapshot : state.getLatestSnapshots()) {
            Point point = snapshot.projectLinear(time-snapshot.getTime());
            double power = state.threat(snapshot.getName());
            double force = 100000*power/Math.pow(self.distanceTo(point), 2);
            double angle = point.getNorthBearingTo(self);

            fx += Math.sin(angle)*force;
            fy += Math.cos(angle)*force;
        }

        // very quickly reducing strength of wall anti-gravity
        fx += 5/Math.pow(Math.min(1, self.getX()-arena.getX()), 3);
        fx += 5/Math.pow(Math.min(1, self.getX()-arena.getWidth()), 3);
        fy += 5/Math.pow(Math.min(1, self.getY()-arena.getY()), 3);
        fy += 5/Math.pow(Math.min(1, self.getY()-arena.getWidth()), 3);

        // create the point and return it
        self.setHeading(self.getNorthBearingTo(new Point(fx, fy)));
        self.setSpeed(Math.sqrt(Math.pow(fx, 2) + Math.pow(fy, 2)));
        return self;
    }

    // iteratively tries to find the point of least gravity in a circular area around the robot
    public void reactive(boolean wall) {
        Snapshot self = state.getSelf();
        double heading = self.getHeading();
        Set<Snapshot> snapshots = state.getLatestSnapshots();
        VectorPoint temp, proj;

        // area parameters
        double min = 2*arena.getX();
        double max = Math.min(Math.min(arena.getWidth(), arena.getHeight())*0.5, Rules.RADAR_SCAN_RADIUS/2);
        double step = (max-min)/5;
        max = Math.max(max, min+1);

        // variables used to store the minimum gravity point
        double tempgrav = 0;
        double mingrav = Double.POSITIVE_INFINITY;
        int turn = 0;
        double distance = 0;

        // iterates through all the points defined by the area parameters
        for (int i = -2; i < 3; i++) {
            for (double j = min; j <= max; j += step) {
                double angle = Utility.fixAngle(heading+i*Rules.MAX_TURN_RATE_RADIANS);

                // if this is a wall-hit movement or if there is no registered hit we can move in any direction,
                // otherwise avoid having the same heading as the bullet
                if (hit != null && wall == false) {
                    double between = Math.toDegrees(Utility.angleBetween(angle, hit.getHeading()));
                    if (between < 15 || between > 345 || (between > 165 && between < 195)) {
                        continue;
                    }
                }

                // check the gravity moving both forwards and backwards
                temp = new VectorPoint(self);
                // forwards
                temp.setHeading(angle);
                temp.setSpeed(j);
                proj = temp.project();
                if (arena.contains(proj)) {
                    proj = gravity(proj);
                    if (proj.getSpeed() < mingrav) {
                        mingrav = proj.getSpeed();
                        turn = i;
                        distance = j;
                    }
                }
                // backwards
                temp.setHeading(angle);
                temp.setSpeed(-j);
                proj = temp.project();
                if (arena.contains(proj)) {
                    proj = gravity(proj);
                    if (proj.getSpeed() < mingrav) {
                        mingrav = proj.getSpeed();
                        turn = i;
                        distance = -j;
                    }
                }
            }
        }

        // move
        //System.out.println("[Reactive] "+turn+" "+distance);
        robot.turnRight(turn*Rules.MAX_TURN_RATE);
        robot.ahead(distance);
    }

    public void move() {
        reactive(true);
    }

    // registers the latest hit in order to avoid its orientation
    public void onHitByBullet(HitByBulletEvent e) {
        hit = new DirectedPoint(e.getBullet().getX(), e.getBullet().getY(), e.getHeadingRadians());
    }
}
