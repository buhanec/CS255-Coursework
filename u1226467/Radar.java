package u1226467;

import robocode.*;
import java.util.*;

public class Radar {
    protected static int    RIGHT       = 1;
    protected static int    LEFT        = -1;
    protected static int    RADAR_0     = 0;
    protected static int    RADAR_360   = 1;
    protected static int    RADAR_180   = 2;
    protected static int    RADAR_45    = 3;
    protected static int    RADAR_SWEEP = 4;
    protected static int    RADAR_GUN   = 5;
    protected static int    AGE_180     = 18;
    protected static int    NUM_180     = 2;
    protected static int    AGE_45      = 4;
    protected static double EXTRA_180   = 0.5;
    protected static double PERC_180    = 1.2;
    protected static double EXTRA_45    = 0.05;
    protected static double PERC_45     = 1.1;
    protected static int    AGE_360     = 40;
    protected static int    MODE_RADAR  = 1;
    protected static int    MODE_GUN    = 2;

    protected Robot robot;
    protected State state;
    protected Rectangle arena;
    protected double heading;
    protected double gunHeading;

    protected int mode;
    protected boolean firing;
    protected boolean justfired;
    protected boolean losttarget;

    protected boolean radar360;
    protected boolean radar180;
    protected boolean radar45;

    // We only need to think one step ahead in this system, as such we have
    // the currently scheduled move and the next scheduled move stored
    protected double angle;
    protected double angle2;
    protected double offset;
    protected int direction;
    protected int direction2;

    protected int working;
    protected int previous;
    protected int last360;
    protected Set<String> names;
    protected String target;

    protected int uniqueScanned;

    Radar(Robot robot, State state, Rectangle arena) {
        this.robot = robot;
        this.state = state;
        this.arena = arena;
        heading = getRadarHeadingRadians();
        gunHeading = getGunHeadingRadians();

        mode = MODE_RADAR;
        firing = false;
        justfired = false;
        losttarget = false;

        radar360 = false;
        radar180 = false;
        radar45 = false;

        angle = 0;
        angle2 = 0;
        direction = RIGHT;
        direction2 = RIGHT;

        working = RADAR_0;
        previous = RADAR_0;
        last360 = -AGE_360-1;
        names = new HashSet<String>();
        target = null;

        uniqueScanned = 0;
    }

    public void reset() {
        mode = MODE_RADAR;
        firing = false;
        justfired = false;
        losttarget = false;

        radar360 = false;
        radar180 = false;
        radar45 = false;

        angle = 0;
        angle2 = 0;
        direction = RIGHT;
        direction2 = RIGHT;

        working = RADAR_0;
        previous = RADAR_0;
        last360 = -AGE_360-1;
        target = null;

        uniqueScanned = 0;
    }

    /**
     * Returns whether a scan is in progress.
     */
    public boolean isActive() {
        return (working != RADAR_0);
    }

    /**
     * Returns whether a general scan was just performed.
     */
    public boolean hasScanned() {
        return (previous == RADAR_360 || previous == RADAR_180);
    }

    /**
     * Returns the current target.
     */
    public String getTarget() {
        return target;
    }

    /**
     * Sets the gun target.
     */
    public void setTarget(String name) {
        target = name;
    }

    /**
     * Returns whether the gun/radar lost the target.
     */
    public boolean lostTarget() {
        return losttarget;
    }

    /**
     * Performs most of the reasoning behind choosing a strategy.
     */
    public void setStrategy() {
        // Adjust radar in case something rotated it
        double turn = Utility.angleBetween(getRadarHeadingRadians(), heading);
        int turndir = 1;
        if (turn > Math.PI) {
            turn = 2*Math.PI - turn;
            turndir = -1;
        }
        if (turn > 0.0001) {
            turned(turn, turndir);
        }

        // Update variables
        heading = getRadarHeadingRadians();
        gunHeading = getGunHeadingRadians();

        // We are in a dueling environment
        if (state.getRemaining() == 1 && target != null) {
            mode = MODE_GUN;
            if (Math.abs(getRadarHeadingRadians()- getGunHeadingRadians()) > 0.0001) {
                setGunToRadar();
            }
            // If all the target has been scanned recently perform an accurate
            // scan.
            if (state.getScanned(AGE_45) == 1) {
                losttarget = false;
                do45();
            // Otherwise perform a 360 degree scan for the target
            } else {
                losttarget = true;
                do360();
            }
        // We are in a melee environment
        } else if (isActive()) {
            //mode = MODE_RADAR;
            mode = MODE_GUN;
            if (working == RADAR_360) {
                // If we have scanned all the targets recently attempt to
                // perform a more accurate oscillation scan instead of the
                // current 360 degree scan.
                if (state.getScanned(AGE_180) == state.getRemaining()) {
                    do180();
                // If we have already performed a 360 degree scan prior to the
                // current one and there is a certain threshold of recently
                // scanned targets, switch to an oscillation scan.
                } else if (previous == RADAR_360
                           && state.getScanned(AGE_180) >= NUM_180) {
                    do180();
                }
            } else if (working == RADAR_GUN) {
                // Potentially include timing out the radar gun in the strategy
                // if the design of the robot changes. Currently the robot
                // decides whether to fire or not rather than the radar
                // strategy.
            }
        } else {
            //mode = MODE_RADAR;
            mode = MODE_GUN;
            // Prevent inactivity of the radar by scheduling either a 360 scan
            // or an oscillation scan, depending on the knowledge available
            if (last360 < (robot.getTime() - AGE_360)
                && state.getScanned(AGE_180) < state.getRemaining()) {
                do360();
            } else {
                do180();
            }
        }
    }

    /**
     * Helper method to help transition between strategies.
     */
    protected void setRadar(int type) {
        previous = working;
        angle = 0;
        angle2 = 0;
        direction2 = direction;
        if (type == RADAR_360) {
            working = RADAR_360;
            radar360 = false;
        } else if (type == RADAR_180) {
            working = RADAR_180;
            radar180 = false;
        } else if (type == RADAR_45) {
            working = RADAR_45;
            radar45 = false;
        } else if (type == RADAR_0) {
            working = RADAR_0;
        } else if (type == RADAR_GUN) {
            working = RADAR_GUN;
        }
    }

    /**
     * Sets the strategy to perform a tighter, more accurate oscillation.
     */
    protected void do45() {
        doOscillate(RADAR_45, AGE_45, EXTRA_45, PERC_45);
    }

    /**
     * Sets the strategy to perform a safer, more padded oscillation.
     */
    protected void do180() {
        doOscillate(RADAR_180, AGE_180, EXTRA_180, PERC_180);
    }

    /**
     * Sets the strategy to perform a 360 degree scan
     */
    protected void do360() {
        do360(direction);
    }

    /**
     * Sets the strategy to perform a 360 degree scan
     */
    protected void do360(int direction) {
        setRadar(RADAR_360);
        angle = 2*Math.PI;
        this.direction = direction;
    }

    /**
     * Sets the strategy to perform oscillation within an arc calculated by the
     * state.
     */
    protected void doOscillate(int type, int age, double flat,
                               double percent) {
        setRadar(type);
        // get all the names to scan, if these are found before the arc is
        // complete the oscillation ends and can rebound faster.
        names = state.getScannedNames(age);

        // get the absolute angles of the arc in which to scan and adjust
        // accordingly.
        double angles[] = state.getArc();
        if (mode == MODE_RADAR) {
            angles[0] = Utility.fixAngle(angles[0] - getRadarHeadingRadians());
            angles[1] = Utility.fixAngle(angles[1] - getRadarHeadingRadians());
        } else {
            angles[0] = Utility.fixAngle(angles[0] - getGunHeadingRadians());
            angles[1] = Utility.fixAngle(angles[1] - getGunHeadingRadians());
        }

        // If the angle is too great, resort to a 360 degree scan
        if (Utility.angleBetween(angles[0], angles[1]) > (Math.PI)) {
            do360();
        // Otherwise determine the optimal way to scan the arc and queue it up
        } else {
            if (!Utility.isAngleBetween(0, angles[0], angles[1])) {
                if (Utility.angleBetween(0, angles[0])
                    < Utility.angleBetween(angles[1], 0)) {
                    angle = pad(angles[1], flat, percent);
                    direction = RIGHT;
                } else {
                    angle = pad(Utility.fixAngle(-angles[0]), flat, percent);
                    direction = LEFT;
                }
            } else {
                if (Utility.angleBetween(0, angles[1])
                    < Utility.angleBetween(angles[0], 0)) {
                    angle = pad(angles[1], flat, percent);
                    direction = RIGHT;
                    angle2 = pad(Utility.fixAngle(-angles[0]), flat, percent)
                                 + angle;
                    direction2 = LEFT;
                } else {
                    angle = pad(Utility.fixAngle(-angles[0]), flat, percent);
                    direction = LEFT;
                    angle2 = pad(angles[1], flat, percent) + angle;
                    direction2 = RIGHT;
                }
            }
        }
    }

    /**
     * "Pads" an angle with either a flat or relative amount of padding,
     * depending on which is larger.
     */
    protected double pad(double radians, double flat, double percent) {
        return Utility.fixAngle(Math.max(radians*percent, radians+flat));
    }

    /**
     * Performs the rotation of the radar to a desired angle in a specific
     * direction. If it cannot complete the operation in one tick it returns
     * the remaining angle in order to allocate the remaining turning for the
     * next tick.
     */
    public double turnRadar(double angle, int direction) {
        double degrees = Rules.RADAR_TURN_RATE;
        if (angle == 0) {
            return 0;
        } else if (angle <= (Rules.RADAR_TURN_RATE_RADIANS + 0.0001)) {
            degrees = Math.min(Rules.RADAR_TURN_RATE, Math.toDegrees(angle));
            angle = 0;
        } else {
            angle -= Rules.RADAR_TURN_RATE_RADIANS;
        }
        if (direction == 1) {
            robot.turnRadarRight(degrees);
        } else {
            robot.turnRadarLeft(degrees);
        }
        return angle;
    }

    /**
     * Performs the rotation of the gun to a desired angle in a specific
     * direction. If it cannot complete the operation in one tick it returns
     * the remaining angle in order to allocate the remaining turning for the
     * next tick.
     */
    public double turnGun(double angle, int direction) {
        double degrees = Rules.GUN_TURN_RATE;
        if (angle == 0) {
            return 0;
        } else if (angle <= (Rules.GUN_TURN_RATE_RADIANS + 0.0001)) {
            degrees = Math.min(Rules.GUN_TURN_RATE, Math.toDegrees(angle));
            angle = 0;
        } else {
            angle -= Rules.GUN_TURN_RATE_RADIANS;
        }
        if (shouldTurn(Math.toRadians(degrees), 1) || mode == MODE_GUN) {
            robot.setAdjustRadarForGunTurn(false);
        } else {
            robot.setAdjustRadarForGunTurn(true);
        }
        if (direction == 1) {
            robot.turnGunRight(degrees);
        } else {
            robot.turnGunLeft(degrees);
        }
        return angle;
    }

    /**
     * Performs scanning after a strategy has been established by moving the
     * appropriate part (gun/radar) and adjusting remaining angles. By scanning
     * in such steps it prevents execution times of over one tick.
     */
    public void scan() {
        double remaining;
        if (mode == MODE_RADAR) {
            angle = turnRadar(angle, direction);
        } else {
            angle = turnGun(angle, direction);
        }
        if (angle == 0) {
            angle = angle2;
            direction = direction2;
            angle2 = 0;
        }
        if (angle == 0) {
            setRadar(RADAR_0);
        }
        justfired = false;
    }

    /**
     * If we have spent too long scanning accurately, urge the robot to shoot
     * something.
     */
    public boolean shootAlready() {
        return (mode == RADAR_180 && previous == RADAR_180);
    }

    /**
     * Simple check if the gun is firing.
     */
    public boolean isFiring() {
        return firing;
    }

    /**
     * Simple linear prediction gun. An approach using statistics and min/max
     * linear predictions was attempted but resulted in worse performance and
     * much less maintainable code. Given more time a potential implementation
     * of more advanced guns could be done.
     */
    public void gunSimple(String name) {
        Snapshot target = state.getSnapshot(name);
        Snapshot self = new Snapshot(robot);

        // make sure we are not firing at ourselves or nobody
        if (name.equals(self.getName()) || target == null) {
            return;
        }

        // signal we are attempting to fire
        firing = true;
        if (working != RADAR_GUN) {
            setRadar(RADAR_GUN);
        }

        // calculate power and angle to projected target
        double currentAngle = getGunHeadingRadians();
        double power = Utility.constrain(500/self.distanceTo(target),
                                         Rules.MIN_BULLET_POWER,
                                         Rules.MAX_BULLET_POWER);
        if (robot.getEnergy() > 30 && power < 1) {
            power = 1;
        }
        long time = (long) (self.distanceTo(target)
                            /Rules.getBulletSpeed(power));
        VectorPoint projection = target.projectLinear(self.getTime()
                                                      - target.getTime()
                                                      + time + 1);
        double targetAngle = self.getNorthBearingTo(projection);

        // determine angle and direction of rotation
        if (Utility.angleBetween(currentAngle, targetAngle)
            < Utility.angleBetween(targetAngle, currentAngle)) {
            angle = Utility.fixAngle(targetAngle-currentAngle);
            direction = 1;
        } else {
            angle = Utility.fixAngle(currentAngle-targetAngle);
            direction = -1;
        }

        // if the target is within the range of a single turn we perform the
        // rotation and fire the weapon, storing the bullet information for
        // statistics
        if (angle < Rules.GUN_TURN_RATE_RADIANS+0.0001) {
            double degrees = Math.min(Rules.GUN_TURN_RATE,
                                      Math.toDegrees(angle));
            if (direction == 1) {
                robot.turnGunRight(degrees);
            } else {
                robot.turnGunLeft(degrees);
            }
            setRadar(RADAR_0);
            justfired = true;
            firing = false;
            state.addBullet(robot.fireBullet(power), name);
        // if the target it outside the range of a single turn we perform the
        // rotation tick by tick
        } else {
            angle = angle - Rules.GUN_TURN_RATE_RADIANS;
            if (direction == 1) {
                robot.turnGunRight(Rules.GUN_TURN_RATE);
            } else {
                robot.turnGunLeft(Rules.GUN_TURN_RATE);
            }
        }
    }

    /**
     * Used to decide whether the radar should be allowed to rotate with the
     * gun or not. In AdvancedRobots this would allow for larger scanning arcs.
     */
    public boolean shouldTurn(double radians, int direction) {
        return (Math.abs(this.direction*angle-direction*radians) < angle);
    }

    /**
     * Called when choosing a strategy in order to adjust the radar after any
     * external influences.
     */
    protected void turned(double radians, int direction) {
        if (mode == MODE_GUN) {
            return;
        }
        if (this.direction == direction) {
            angle -= radians;
            if (angle < 0) {
                if (direction2 == direction) {
                    angle2 += angle;
                    if (angle2 < 0) {
                        angle2 = -angle2;
                        direction2 = -direction2;
                    }
                } else {
                    angle2 -= angle;
                }
                angle = angle2;
                angle2 = 0;
            }
        } else {
            angle += radians;
        }
        if (angle > 0 && angle <= 0.0001) {
            angle = 0;
        }
    }

    /**
     * Used to track completed oscillation scans.
     */
    public void onScannedRobot(ScannedRobotEvent e) {
        if (justfired && e.getName().equals(target)) {
            // With AdvancedRobots potentially trigger sticky-radar behaviour
            // at this point
        }
        if (names.contains(e.getName())) {
            names.remove(e.getName());
            if (names.isEmpty() && working == RADAR_180) {
                setRadar(RADAR_0);
            }
        }
    }

    /**
     * Returns the heading of the radar.
     * @return heading in radians
     */
    public double getRadarHeadingRadians() {
        return Math.toRadians(robot.getRadarHeading());
    }

    /**
     * Returns the heading of the gun.
     * @return heading in radians
     */
    public double getGunHeadingRadians() {
        return Math.toRadians(robot.getGunHeading());
    }

    /**
     * Returns the heading of the scanning mode (gun or radar)
     * @return heading in radians
     */
    public double getHeading() {
        if (mode == MODE_RADAR) {
            return getRadarHeadingRadians();
        } else {
            return getGunHeadingRadians();
        }
    }

    /**
     * Turns the gun to match the radar's heading.
     */
    public void setGunToRadar() {
        double radar = getRadarHeadingRadians();
        double gun = getGunHeadingRadians();
        double angle;
        int direction;
        if (Utility.angleBetween(gun, radar) < Utility.angleBetween(radar, gun)) {
            angle = Utility.fixAngle(radar-gun);
            direction = 1;
        } else {
            angle = Utility.fixAngle(gun-radar);
            direction = -1;
        }
        while (angle > 0) {
            angle = turnGun(angle, direction);
        }
    }

    /**
     * Turns to radar to match the gun's heading.
     */
    public void setRadarToGun() {
        double radar = getRadarHeadingRadians();
        double gun = getGunHeadingRadians();
        double angle;
        int direction;
        if (Utility.angleBetween(radar, gun) < Utility.angleBetween(gun, radar)) {
            angle = Utility.fixAngle(gun-radar);
            direction = 1;
        } else {
            angle = Utility.fixAngle(radar-gun);
            direction = -1;
        }
        while (angle > 0) {
            angle = turnRadar(angle, direction);
        }
    }
}
