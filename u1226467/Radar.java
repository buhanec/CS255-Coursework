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
    protected static double PERC_180  = 0.2;
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
        heading = robot.getRadarHeading();

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

        uniqueScanned = 0;
    }

    public boolean isActive() {
        return (working != RADAR_0);
    }

    public void doStrategy() {
        heading = robot.getRadarHeading();
        if (state.getRemaining() == 1) {
            if (state.getScanned() == 1) {
                System.out.println("D:1");
                do45();
            } else {
                System.out.println("D:2");
                do360();
            }
        } else if (isActive()) {
            System.out.println("D:Active");
            if (state.getScanned(AGE_180) == state.getRemaining()) {
                System.out.println("D:3");
                do180();
            } else if (working == RADAR_360 && previous == RADAR_360 && state.getScanned(AGE_180) >= NUM_180) {
                System.out.println("D:4");
                do180();
            }
        } else {
            System.out.println("D:Inactive");
            if (last360 < (robot.getTime() - AGE_360) && state.getScanned(AGE_180) < state.getRemaining()) {
                System.out.println("D:5");
                do360();
            } else {
                System.out.println("D:6");
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
        System.out.println("TODO: DUEL SCAN");
    }

    protected void do360() {
        do360(direction);
    }

    protected void do360(int direction) {
        setRadar(RADAR_360);
        angle = 2*Math.PI;
        this.direction = direction;
    }

    protected void do180() {
        setRadar(RADAR_180);
        names = state.getScannedNames(AGE_180);
        double angles[] = state.getArc(heading);
        System.out.println("do180");
        System.out.println(Math.toDegrees(angles[0])+" - "+Math.toDegrees(angles[1]));
        System.out.println(Math.toDegrees(Utility.angleBetween(angles[0], angles[1])));
        if (Utility.angleBetween(angles[0], angles[1]) > (Math.PI)) {
            System.out.println("[Radar] Unsuitable 180, regressing to 360");
            do360();
        } else {
            if (!Utility.isAngleBetween(0, angles[0], angles[1])) {
                if (Utility.angleBetween(0, angles[0]) < Utility.angleBetween(angles[1], 0)) {
                    angle = Utility.fixAngle(angles[1]+EXTRA_180);
                    direction = RIGHT;
                } else {
                    angle = Utility.fixAngle(angles[0]-EXTRA_180);
                    direction = LEFT;
                }
            } else {
                if (Utility.angleBetween(0, angles[1]) < Utility.angleBetween(angles[0], 0)) {
                    angle = angles[1]*PERC_180;
                    direction = RIGHT;
                    angle2 = (2*Math.PI - angles[0])*PERC_180 + angle;
                    direction2 = LEFT;
                } else {
                    angle = (2*Math.PI-angles[0])*PERC_180;
                    direction = LEFT;
                    angle2 = angles[1]*PERC_180 + angle;
                    direction2 = RIGHT;
                }
            }
        }
        System.out.println(direction+" "+Math.toDegrees(angle));
        System.out.println(direction2+" "+Math.toDegrees(angle2));
    }

    public void turn() {
        double degrees;
        if (angle <= (Rules.RADAR_TURN_RATE_RADIANS + 0.0001)) {
            degrees = Math.min(Rules.RADAR_TURN_RATE, Math.toDegrees(angle));
            angle = angle2;
            angle2 = 0;
            direction = direction2;
            if (angle == 0) {
                setRadar(RADAR_0);
            }
        } else {
            degrees = Rules.RADAR_TURN_RATE;
            angle -= Rules.RADAR_TURN_RATE_RADIANS;
        }
        if (direction == RIGHT) {
            robot.turnRadarRight(degrees);
        } else {
            robot.turnRadarLeft(degrees);
        }
    }
}
