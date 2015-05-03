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
    protected static int    AGE_180     = 8;
    protected static int    NUM_180     = 2;
    protected static int    AGE_45      = 4;
    protected static double EXTRA_180   = 0.5;
    protected static double PERC_180    = 1.2;
    protected static double EXTRA_45    = 0.25;
    protected static double PERC_45     = 1.1;
    protected static int    AGE_360     = 40;
    protected static int    MODE_RADAR  = 1;
    protected static int    MODE_GUN    = 2;

    protected Robot robot;
    protected State state;
    protected double heading;
    protected double gunHeading;

    protected int mode;

    protected boolean radar360;
    protected boolean radar180;
    protected boolean radar45;

    // We only need to think one step ahead in this system
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

    Radar(Robot robot, State state) {
        this.robot = robot;
        this.state = state;
        heading = getRadarHeadingRadians();
        gunHeading = getGunHeadingRadians();

        mode = MODE_RADAR;

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

    public boolean isActive() {
        return (working != RADAR_0);
    }

    public String getTarget() {
        if (state.getScanned(AGE_45) == 1) {
            return target;
        } else {
            return null;
        }
    }

    public void setStrategy() {
        // Adjust radar in case something rotated it
        double turn = Utility.angleBetween(getRadarHeadingRadians(), heading);
        int turndir = 1;
        if (turn > Math.PI) {
            turn = 2*Math.PI - turn;
            turndir = -1;
        }
        heading = getRadarHeadingRadians();
        gunHeading = getGunHeadingRadians();
        if (turn > 0.0001) {
            turned(turn, turndir);
        }

        // We are in a dueling environment
        if (state.getRemaining() == 1) {
            for (String t : state.getScannedNames(AGE_45)) {
                target = t;
            }
            if (state.getScanned(AGE_45) == 1) {
                System.out.println("[Radar] 45");
                mode = MODE_GUN;
                do45();
            } else {
                System.out.println("[Radar] Regressing 45->360");
                mode = MODE_RADAR;
                do360();
            }
        // We are in a melee environment
        } else if (isActive()) {
            mode = MODE_RADAR;
            System.out.println("[Radar] Strategy: active ("+working+")");
            if (working == RADAR_360) {
                if (state.getScanned(AGE_180) == state.getRemaining()) {
                    System.out.println("[Radar] Found all, attempting 360->180");
                    do180();
                } else if (previous == RADAR_360 && state.getScanned(AGE_180) >= NUM_180) {
                    System.out.println("[Radar] Found enough, attempting 360->180");
                    do180();
                }
            }
        } else {
            mode = MODE_RADAR;
            System.out.println("[Radar] Strategy: inactive");
            if (last360 < (robot.getTime() - AGE_360) && state.getScanned(AGE_180) < state.getRemaining()) {
                System.out.println("[Radar] Time-based 360");
                do360();
            } else {
                System.out.println("[Radar] Complete knowledge, 180");
                do180();
            }
        }
    }

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
        }
    }

    protected void do45() {
        doOscillate(RADAR_45, AGE_45, EXTRA_45, PERC_45);
    }

    protected void do180() {
        doOscillate(RADAR_180, AGE_180, EXTRA_180, PERC_180);
    }

    protected void do360() {
        do360(direction);
    }

    protected void do360(int direction) {
        System.out.println("[Radar] do360");
        setRadar(RADAR_360);
        angle = 2*Math.PI;
        this.direction = direction;
    }

    protected void doOscillate(int type, int age, double flat, double percent) {
        System.out.println("[Radar] doOscillate");
        setRadar(type);
        names = state.getScannedNames(age);
        double angles[] = state.getArc();
        angles[0] = Utility.fixAngle(angles[0] - heading);
        angles[1] = Utility.fixAngle(angles[1] - heading);
        System.out.println("[Radar] Arc: "+Math.toDegrees(angles[0])+" - "+Math.toDegrees(angles[1]));
        System.out.println("[Radar] Radar heading: "+robot.getRadarHeading()+" ("+heading+")");
        System.out.println("[Radar] Arc size: "+Math.toDegrees(Utility.angleBetween(angles[0], angles[1])));
        if (Utility.angleBetween(angles[0], angles[1]) > (Math.PI)) {
            System.out.println("[Radar] Unsuitable, regressing to 360");
            do360();
        } else {
            if (!Utility.isAngleBetween(0, angles[0], angles[1])) {
                if (Utility.angleBetween(0, angles[0]) < Utility.angleBetween(angles[1], 0)) {
                    angle = pad(angles[1], flat, percent);
                    direction = RIGHT;
                } else {
                    angle = pad(Utility.fixAngle(-angles[0]), flat, percent);
                    direction = LEFT;
                }
            } else {
                if (Utility.angleBetween(0, angles[1]) < Utility.angleBetween(angles[0], 0)) {
                    angle = pad(angles[1], flat, percent);
                    direction = RIGHT;
                    angle2 = pad(Utility.fixAngle(-angles[0]), flat, percent) + angle;
                    direction2 = LEFT;
                } else {
                    angle = pad(Utility.fixAngle(-angles[0]), flat, percent);
                    direction = LEFT;
                    angle2 = pad(angles[1], flat, percent) + angle;
                    direction2 = RIGHT;
                }
            }
        }
        System.out.println("[Radar] First turn: "+direction*Math.toDegrees(angle));
        System.out.println("[Radar] Second turn: "+direction2*Math.toDegrees(angle2));
    }

    protected double pad(double radians, double flat, double percent) {
        return Utility.fixAngle(Math.max(radians*percent, radians+flat));
    }

    public double turnRadar(double angle, int direction) {
        double degrees = Rules.RADAR_TURN_RATE;
        // Get angle
        if (angle == 0) {
            return 0;
        } else if (angle <= (Rules.RADAR_TURN_RATE_RADIANS + 0.0001)) {
            degrees = Math.min(Rules.RADAR_TURN_RATE, Math.toDegrees(angle));
            angle = 0;
        } else {
            angle -= Rules.RADAR_TURN_RATE_RADIANS;
        }
        // Turnt he radar
        if (direction == 1) {
            robot.turnRadarRight(degrees);
        } else {
            robot.turnRadarLeft(degrees);
        }
        // Return any remaining angle
        return angle;
    }

    public double turnGun(double angle, int direction) {
        double degrees = Rules.GUN_TURN_RATE;
        // Get angle
        if (angle == 0) {
            return 0;
        } else if (angle <= (Rules.GUN_TURN_RATE_RADIANS + 0.0001)) {
            degrees = Math.min(Rules.GUN_TURN_RATE, Math.toDegrees(angle));
            angle = 0;
        } else {
            angle -= Rules.GUN_TURN_RATE;
        }
        // Should we turn the radar too
        if (shouldTurn(Math.toRadians(degrees), 1) || mode == MODE_GUN) {
            robot.setAdjustRadarForGunTurn(false);
        } else {
            robot.setAdjustRadarForGunTurn(true);
        }
        // Turn the gun
        if (direction == 1) {
            robot.turnGunRight(degrees);
        } else {
            robot.turnGunLeft(degrees);
        }
        // Return any remaining angle
        return angle;
    }

    public void scan() {
        double remaining = turnRadar(angle, direction);
        if (remaining == 0) {
            angle = angle2;
            direction = direction2;
            angle2 = 0;
        }
    }

    // simple iterative prediction
    public void gunLinear(String name) {
        Snapshot snap = state.getSnapshot(name);
        VectorPoint proj;
        if (snap == null) {
            return;
        }
        Snapshot self = state.getSelf();
        double gunAngle = getGunHeadingRadians();
        double bulletPower = Utility.constrain(500/self.distanceTo(snap), Rules.MIN_BULLET_POWER, Rules.MAX_BULLET_POWER);
        long bulletTime = (long) (self.distanceTo(snap)/Rules.getBulletSpeed(bulletPower));
        proj = snap.projectLinear(self.getTime()-snap.getTime()+bulletTime+1); //+1 for the turn delay between moving and firing
        double targetAngle = self.getNorthBearingTo(proj);
        if (Utility.angleBetween(gunAngle, targetAngle) < Utility.angleBetween(targetAngle, gunAngle)) {
            if (Utility.angleBetween(gunAngle, targetAngle) > Rules.GUN_TURN_RATE_RADIANS) {
                turnGun(Rules.GUN_TURN_RATE_RADIANS, 1);
            } else {
                turnGun(Utility.fixAngle(targetAngle - gunAngle), 1);
            }
        } else {
            if (Utility.angleBetween(targetAngle, gunAngle) > Rules.GUN_TURN_RATE_RADIANS) {
                turnGun(Rules.GUN_TURN_RATE_RADIANS, -1);
            } else {
                turnGun(Utility.fixAngle(gunAngle - targetAngle), -1);
            }
        }
        if (robot.getGunHeat() == 0) {
            robot.fire(bulletPower);
        }
    }

    public boolean shouldTurn(double radians, int direction) {
        return (Math.abs(this.direction*angle-direction*radians) < angle);
    }

    protected void turned(double radians, int direction) {
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

    public void onScannedRobot(ScannedRobotEvent e) {
        if (names.contains(e.getName())) {
            names.remove(e.getName());
            if (names.isEmpty() && working == RADAR_180) {
                setRadar(RADAR_0);
            }
            System.out.println("[Radar] scanned a target, "+names.size()+" remain");
        }
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public double getRadarHeadingRadians() {
        return Math.toRadians(robot.getRadarHeading());
    }

    public double getGunHeadingRadians() {
        return Math.toRadians(robot.getGunHeading());
    }

    public double getHeading() {
        if (mode == MODE_RADAR) {
            return getRadarHeadingRadians();
        } else {
            return getGunHeadingRadians();
        }
    }

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
