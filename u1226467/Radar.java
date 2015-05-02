package u1226467;

import robocode.*;
import java.util.*;

public class Radar {
    protected static int    RIGHT     = 1;
    protected static int    LEFT      = -1;
    protected static int    RADAR_0   = 0;
    protected static int    RADAR_360 = 1;
    protected static int    RADAR_180 = 2;
    protected static int    RADAR_45  = 3;
    protected static int    AGE_180   = 8;
    protected static int    NUM_180   = 2;
    protected static double EXTRA_180 = 0.5;
    protected static double PERC_180  = 1.2;
    protected static int    AGE_360   = 40;

    protected Robot robot;
    protected State state;
    protected double heading;

    protected boolean radar360;
    protected boolean radar180;
    protected boolean radar45;

    //TODO: implement queue
    protected double angle;
    protected double angle2;
    protected double offset;
    protected int direction;
    protected int direction2;
    protected int offsetDirection;

    protected int working;
    protected int previous;
    protected int last360;
    protected Set<String> names;

    protected int uniqueScanned;

    Radar(Robot robot, State state) {
        this.robot = robot;
        this.state = state;
        heading = getRadarHeadingRadians();

        radar360 = false;
        radar180 = false;
        radar45 = false;

        angle = 0;
        angle2 = 0;
        direction = RIGHT;
        direction2 = RIGHT;
        offset = 0;
        offsetDirection = RIGHT;

        working = RADAR_0;
        previous = RADAR_0;
        last360 = -AGE_360-1;
        names = new HashSet<String>();

        uniqueScanned = 0;
    }

    public boolean isActive() {
        return (working != RADAR_0);
    }

    public void doStrategy() {
        double turn = Utility.angleBetween(getRadarHeadingRadians(), heading);
        int turndir = 1;
        if (turn > Math.PI) {
            turn = 2*Math.PI - turn;
            turndir = -1;
        }
        heading = getRadarHeadingRadians();
        if (turn > 0.0001) {
            turned(turn, turndir);
        }

        if (state.getRemaining() == 1) {
            if (state.getScanned() == 1) {
                System.out.println("[Radar] Forced duel");
                do45();
            } else {
                System.out.println("[Radar] Forced search for duel");
                do360();
            }
        } else if (isActive()) {
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
        offset = 0;
        offsetDirection = RIGHT;
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
        System.out.println("[Radar] do45");
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

    private double pad180(double radians) {
        return Utility.fixAngle(Math.max(radians*PERC_180, radians+EXTRA_180));
    }

    protected void do180() {
        System.out.println("[Radar] do180");
        setRadar(RADAR_180);
        names = state.getScannedNames(AGE_180);
        double angles[] = state.getArc();
        angles[0] = Utility.fixAngle(angles[0] - heading);
        angles[1] = Utility.fixAngle(angles[1] - heading);
        System.out.println("[Radar] Arc: "+Math.toDegrees(angles[0])+" - "+Math.toDegrees(angles[1]));
        System.out.println("[Radar] Radar heading: "+robot.getRadarHeading()+" ("+heading+")");
        System.out.println("[Radar] Arc size: "+Math.toDegrees(Utility.angleBetween(angles[0], angles[1])));
        if (Utility.angleBetween(angles[0], angles[1]) > (Math.PI)) {
            System.out.println("[Radar] Unsuitable 180, regressing to 360");
            do360();
        } else {
            if (!Utility.isAngleBetween(0, angles[0], angles[1])) {
                if (Utility.angleBetween(0, angles[0]) < Utility.angleBetween(angles[1], 0)) {
                    angle = pad180(angles[1]);
                    direction = RIGHT;
                } else {
                    angle = pad180(Utility.fixAngle(-angles[0]));
                    direction = LEFT;
                }
            } else {
                if (Utility.angleBetween(0, angles[1]) < Utility.angleBetween(angles[0], 0)) {
                    angle = pad180(angles[1]);
                    direction = RIGHT;
                    angle2 = pad180(Utility.fixAngle(-angles[0])) + angle;
                    direction2 = LEFT;
                } else {
                    angle = pad180(Utility.fixAngle(-angles[0]));
                    direction = LEFT;
                    angle2 = pad180(angles[1]) + angle;
                    direction2 = RIGHT;
                }
            }
        }
        System.out.println("[Radar] First turn: "+direction*Math.toDegrees(angle));
        System.out.println("[Radar] Second turn: "+direction2*Math.toDegrees(angle2));
    }

    public void turn() {
        double degrees = Rules.RADAR_TURN_RATE;
        int function = direction;
        if (angle <= (Rules.RADAR_TURN_RATE_RADIANS + 0.0001)) {
            degrees = Math.min(Rules.RADAR_TURN_RATE, Math.toDegrees(angle));
            angle = angle2;
            angle2 = 0;
            function = direction;
            direction = direction2;
            if (angle == 0) {
                setRadar(RADAR_0);
            }
        } else {
            angle -= Rules.RADAR_TURN_RATE_RADIANS;
        }
        if (function == RIGHT) {
            System.out.println("[Radar] Turning right "+degrees);
            robot.turnRadarRight(degrees);
            System.out.println("[Radar] Turned right "+degrees);
        } else {
            System.out.println("[Radar] Turning left "+degrees);
            robot.turnRadarLeft(degrees);
            System.out.println("[Radar] Turned left "+degrees);
        }
        heading = getRadarHeadingRadians();
    }

    public boolean shouldTurn(double radians, int direction) {
        return (Math.abs(this.direction*angle-direction*radians) < angle);
    }

    public void turned(double radians, int direction) {
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
        System.out.println(Arrays.toString(names.toArray()));
        if (names.contains(e.getName())) {
            names.remove(e.getName());
            if (names.isEmpty() && working == RADAR_180) {
                setRadar(RADAR_0);
            }
            System.out.println("[Radar] scanned a target, "+names.size()+" remain");
        }
    }

    public double getRadarHeadingRadians() {
        return Math.toRadians(robot.getRadarHeading());
    }
}
