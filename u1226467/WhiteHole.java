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

    public void update(long time) {
        if (time == 0) {
            hit = null;
        }
        this.time = time;
    }

    private VectorPoint gravity(Point p) {
        double fx = 0;
        double fy = 0;
        VectorPoint self = new VectorPoint(p, 0, 0);

        // we don't really want perfect precision, errors result in randomisation
        for (Snapshot snapshot : state.getLatestSnapshots()) {
            //System.out.println(snapshot);
            Point point = snapshot.projectLinear(time-snapshot.getTime());
            //System.out.println(point);
            double power = state.threat(snapshot.getName());
            //System.out.println(power);
            double force = 100000*power/Math.pow(self.distanceTo(point), 2);
            double angle = point.getNorthBearingTo(self);

            //System.out.println(force);

            fx += Math.sin(angle)*force;
            fy += Math.cos(angle)*force;

            //System.out.println("pre-wall: "+fx+","+fy);
        }

        // very quickly reducing strength of wall anti-gravity
        fx += 5/Math.pow(Math.min(1, self.getX()-arena.getX()), 3);
        fx += 5/Math.pow(Math.min(1, self.getX()-arena.getWidth()), 3);
        fy += 5/Math.pow(Math.min(1, self.getY()-arena.getY()), 3);
        fy += 5/Math.pow(Math.min(1, self.getY()-arena.getWidth()), 3);
        //System.out.println("post-wall: "+fx+","+fy);

        // create the point
        self.setHeading(self.getNorthBearingTo(new Point(fx, fy)));
        self.setSpeed(Math.sqrt(Math.pow(fx, 2) + Math.pow(fy, 2)));
        return self;
    }

    public void reactive(boolean wall) {
        Snapshot self = state.getSelf();
        double heading = self.getHeading();
        Set<Snapshot> snapshots = state.getLatestSnapshots();
        VectorPoint temp, proj;
        double min = 2*arena.getX();
        double max = Math.min(Math.min(arena.getWidth(), arena.getHeight())*0.5, Rules.RADAR_SCAN_RADIUS/2);
        double step = (max-min)/5;
        max = Math.max(max, min+1);
        double tempgrav = 0;
        double mingrav = Double.POSITIVE_INFINITY;
        int turn = 0;
        double distance = 0;

        for (int i = -2; i < 3; i++) {
            //System.out.println("[Reactive] Outer: "+i);
            for (double j = min; j <= max; j += step) {
                //System.out.println("  [Reactive] Inner: "+j);
                double angle = Utility.fixAngle(heading+i*Rules.MAX_TURN_RATE_RADIANS);
                if (hit != null && wall == false) {
                    double between = Math.toDegrees(Utility.angleBetween(angle, hit.getHeading()));
                    if (between < 15 || between > 345 || (between > 165 && between < 195)) {
                        continue;
                    }
                }
                temp = new VectorPoint(self);
                // forwards
                temp.setHeading(angle);
                temp.setSpeed(j);
                proj = temp.project();
                if (arena.contains(proj)) {
                    proj = gravity(proj);
                    //System.out.println("    [Reactive] Projection: "+proj.getSpeed());
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
                    //System.out.println("    [Reactive] Projection: "+proj.getSpeed());
                    if (proj.getSpeed() < mingrav) {
                        mingrav = proj.getSpeed();
                        turn = i;
                        distance = -j;
                    }
                }
            }
        }

        System.out.println("[Reactive] "+turn+" "+distance);
        robot.turnRight(turn*Rules.MAX_TURN_RATE);
        robot.ahead(distance);
    }

    public void move() {
        reactive(true);
    }

    public void onHitByBullet(HitByBulletEvent e) {
        hit = new DirectedPoint(e.getBullet().getX(), e.getBullet().getY(), e.getHeadingRadians());
    }
}
