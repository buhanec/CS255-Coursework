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
    protected static double EXTRA_45    = 0.05;
    protected static double PERC_45     = 1.1;
    protected static int    AGE_360     = 40;
    protected static int    MODE_RADAR  = 1;
    protected static int    MODE_GUN    = 2;

    protected Robot robot;
    protected State state;
    protected double heading;
    protected double gunHeading;

    protected int mode;
    protected boolean firing;

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

    public void reset() {
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
        target = null;

        uniqueScanned = 0;
    }

    public boolean isActive() {
        return (working != RADAR_0);
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String name) {
        target = name;
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
        if (state.getRemaining() == 1 && target != null) {
            System.out.println("[Radar] target: "+target);
            mode = MODE_GUN;
            setGunToRadar();
            for (String t : state.getScannedNames(AGE_45)) {
                target = t;
            }
            if (state.getScanned(AGE_45) == 1) {
                System.out.println("[Radar] 45");
                do45();
            } else {
                System.out.println("[Radar] Regressing 45->360");
                do360();
            }
        // We are in a melee environment
        } else if (isActive()) {
            //mode = MODE_RADAR;
            mode = MODE_GUN;
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
            //mode = MODE_RADAR;
            mode = MODE_GUN;
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
        System.out.println("  [Radar] Raw angles: "+Math.round(Math.toDegrees(angles[0]))+"-"+Math.round(Math.toDegrees(angles[1])));
        if (mode == MODE_RADAR) {
            angles[0] = Utility.fixAngle(angles[0] - getRadarHeadingRadians());
            angles[1] = Utility.fixAngle(angles[1] - getRadarHeadingRadians());
        } else {
            angles[0] = Utility.fixAngle(angles[0] - getGunHeadingRadians());
            angles[1] = Utility.fixAngle(angles[1] - getGunHeadingRadians());
        }
        System.out.println("  [Radar] Adjusted angles: "+Math.round(Math.toDegrees(angles[0]))+"-"+Math.round(Math.toDegrees(angles[1])));

        System.out.println("  [Radar] Arc size: "+Math.toDegrees(Utility.angleBetween(angles[0], angles[1])));

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
        System.out.println("[Gun] turning gun "+Math.toDegrees(angle)*direction);
        double degrees = Rules.GUN_TURN_RATE;
        // Get angle
        if (angle == 0) {
            return 0;
        } else if (angle <= (Rules.GUN_TURN_RATE_RADIANS + 0.0001)) {
            degrees = Math.min(Rules.GUN_TURN_RATE, Math.toDegrees(angle));
            angle = 0;
        } else {
            angle -= Rules.GUN_TURN_RATE_RADIANS;
        }
        // Should we turn the radar too
        if (shouldTurn(Math.toRadians(degrees), 1) || mode == MODE_GUN) {
            robot.setAdjustRadarForGunTurn(false);
        } else {
            robot.setAdjustRadarForGunTurn(true);
        }
        // Turn the gun
        if (direction == 1) {
            //System.out.println("  [Gun] turning right "+degrees);
            robot.turnGunRight(degrees);
        } else {
            //System.out.println("  [Gun] turning left "+degrees);
            robot.turnGunLeft(degrees);
        }
        //System.out.println("  [Gun] done, left "+Math.toDegrees(angle));
        // Return any remaining angle
        return angle;
    }

    public void scan() {
        double remaining;
        if (mode == MODE_RADAR) {
            System.out.println("[Radar] Scanning with radar"+Math.toDegrees(angle));
            angle = turnRadar(angle, direction);
        } else {
            System.out.println("[Radar] Scanning with gun: "+Math.toDegrees(angle));
            angle = turnGun(angle, direction);
        }
        if (angle == 0) {
            angle = angle2;
            direction = direction2;
            angle2 = 0;
        }
    }

    public boolean isFiring() {
        return firing;
    }

    public Bullet gunSimple(String name) {
        System.out.println("[SIMPLEGUN]");
        Snapshot target = state.getSnapshot(name);
        Snapshot self = state.getSelf();

        if (self == null) {
            System.out.println("  [Gun] Self is null");
        }
        if (target == self || target == null) {
            System.out.println("  [Gun] Null target");
            return null;
        }
        System.out.println("  [Gun] Going for "+name);
        firing = true;

        double currentAngle = getGunHeadingRadians();
        System.out.println("  [Gun] Gun currently at "+Math.round(Math.toDegrees(currentAngle)));

        double power = Utility.constrain(500/self.distanceTo(target), Rules.MIN_BULLET_POWER, Rules.MAX_BULLET_POWER);
        long time = (long) (self.distanceTo(target)/Rules.getBulletSpeed(power));

        VectorPoint projection = target.projectLinear(self.getTime()-target.getTime()+time);
        System.out.println("  [Gun] Projected target at "+projection);

        double targetAngle = self.getNorthBearingTo(projection);
        System.out.println("  [Gun] Gun projected at "+Math.round(Math.toDegrees(targetAngle)));

        firing = false;
        return null;
    }

    // simple iterative prediction
    public Bullet gunLinear(String name) {
        System.out.println("[GUNGUNGUGNUNGUNGUN]");
        Snapshot snap = state.getSnapshot(name);
        Snapshot self = state.getSelf();
        if (snap == self || name == null) {
            System.out.println("[Gun] no target, shutting down");
            return null;
        }
        System.out.println("[Gun] going for target "+name);
        firing = true;
        VectorPoint proj;
        double gunAngle = getGunHeadingRadians();
        System.out.println("[Gun] gun heading: "+Math.toDegrees(gunAngle));
        double bulletPower = Utility.constrain(500/self.distanceTo(snap), Rules.MIN_BULLET_POWER, Rules.MAX_BULLET_POWER);
        long bulletTime = (long) (self.distanceTo(snap)/Rules.getBulletSpeed(bulletPower));
        proj = snap.projectLinear(self.getTime()-snap.getTime()+bulletTime);
        double targetAngle = self.getNorthBearingTo(proj);
        System.out.println("[Gun] target angle: "+Math.toDegrees(targetAngle));
        System.out.println("[Gun] target angle: "+Math.toDegrees(self.getRelativeBearingTo(snap)));
        //VectorPoint test1 = new VectorPoint(self);
        //VectorPoint test2 = new VectorPoint(snap);
        //VectorPoint test3 = new VectorPoint(proj);
        //System.out.println(test1.getX()+" "+test1.getY());
        //System.out.println(test2.getX()+" "+test2.getY());
        if (Utility.angleBetween(gunAngle, targetAngle) < Utility.angleBetween(targetAngle, gunAngle)) {
            System.out.println("[Gun] Angle to right "+Math.toDegrees(Utility.angleBetween(gunAngle, targetAngle)));
            if (Utility.angleBetween(gunAngle, targetAngle) > Rules.GUN_TURN_RATE_RADIANS) {
                turnGun(Rules.GUN_TURN_RATE_RADIANS, 1);
            } else {
                turnGun(Utility.fixAngle(targetAngle - gunAngle), 1);
                if (robot.getGunHeat() == 0) {
                    firing = false;
                    return robot.fireBullet(bulletPower);
                }
            }
        } else {
            System.out.println("[Gun] Angle to left "+Math.toDegrees(Utility.angleBetween(targetAngle, gunAngle)));
            if (Utility.angleBetween(targetAngle, gunAngle) > Rules.GUN_TURN_RATE_RADIANS) {
                turnGun(Rules.GUN_TURN_RATE_RADIANS, -1);
            } else {
                turnGun(Utility.fixAngle(gunAngle - targetAngle), -1);
                if (robot.getGunHeat() == 0) {
                    firing = false;
                    return robot.fireBullet(bulletPower);
                }
            }
        }
        firing = false;
        return null;
    }

    public boolean shouldTurn(double radians, int direction) {
        return (Math.abs(this.direction*angle-direction*radians) < angle);
    }

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
        System.out.println("[Radar] setting gun to radar");
        double radar = getRadarHeadingRadians();
        double gun = getGunHeadingRadians();
        System.out.println("[Radar] "+Math.round(Math.toDegrees(gun))+"->"+Math.round(Math.toDegrees(radar)));
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
