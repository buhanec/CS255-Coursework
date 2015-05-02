package u1226467;

import robocode.*;

public class VectorPoint extends DirectedPoint {
    protected double speed;

    VectorPoint(double x, double y, double heading, double speed) {
        super(x, y, heading);
        this.speed = speed;
    }

    VectorPoint(Point point, double heading, double speed) {
        super(point, heading);
        this.speed = speed;
    }

    VectorPoint(DirectedPoint point, double speed) {
        super(point);
        this.speed = speed;
    }

    VectorPoint(VectorPoint vector) {
        super(vector);
        speed = vector.speed;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public void setVector(double heading, double speed) {
        this.heading = heading;
        this.speed = speed;
    }

    public VectorPoint project() {
        return projectLinear(1);
    }

    public VectorPoint projectMax() {
        return projectLinearMax(1);
    }

    public VectorPoint projectMin() {
        return projectLinearMin(1);
    }

    public VectorPoint projectLinear(long time) {
        double x = this.x + Math.sin(heading)*speed*time;
        double y = this.y + Math.cos(heading)*speed*time;
        return new VectorPoint(x, y, heading, speed);
    }

    public VectorPoint projectLinearMax(long time) {
        double distance = 0;
        double speed = this.speed;
        for (int i = 0; i < time; i++) {
            if (speed < 0) {
                speed = Math.min(0, speed + Rules.DECELERATION);
            } else if (speed < Rules.MAX_VELOCITY) {
                speed = Math.min(Rules.MAX_VELOCITY, speed + Rules.ACCELERATION);
            }
            distance += speed;
        }
        double x = this.x + Math.sin(heading)*distance;
        double y = this.y + Math.cos(heading)*distance;
        return new VectorPoint(x, y, heading, speed);
    }

    public VectorPoint projectLinearMin(long time) {
        double distance = 0;
        double speed = this.speed;
        for (int i = 0; i < time; i++) {
            if (speed > 0) {
                speed = Math.max(0, speed - Rules.DECELERATION);
            } else if (speed <= 0) {
                speed = Math.max(-Rules.MAX_VELOCITY, speed - Rules.ACCELERATION);
            }
            distance += speed;
        }
        double x = this.x + Math.sin(heading)*distance;
        double y = this.y + Math.cos(heading)*distance;
        return new VectorPoint(x, y, heading, speed);
    }

    public VectorPoint projectLateralMax(DirectedPoint point, int direction) {
        VectorPoint retval = new VectorPoint(this);
        double bearing = point.getBearingTo(this);
        //System.out.println("---");
        //System.out.println(Math.toDegrees(point.getHeading())+" "+Math.toDegrees(bearing));
        double left = Utility.fixAngle(point.getHeading() + bearing - Math.PI/2 - getHeading());
        int ldir = 1;
        double right = Utility.fixAngle(point.getHeading() + bearing + Math.PI/2 - getHeading());
        int rdir = 1;
        //System.out.println(Math.toDegrees(left)+" "+Math.toDegrees(right));
        double max;
        double min;
        // speed
        if (speed > 0) {
            max = Math.min(Rules.MAX_VELOCITY, speed + Rules.ACCELERATION);
            min = Math.max(0, speed - Rules.DECELERATION);
        } else if (speed == 0) {
            max = Math.min(Rules.MAX_VELOCITY, Rules.ACCELERATION);
            min = Math.max(-Rules.MAX_VELOCITY, -Rules.ACCELERATION);
        } else {
            max = Math.min(0, speed + Rules.DECELERATION);
            min = Math.max(-Rules.MAX_VELOCITY, speed - Rules.ACCELERATION);
        }
        // get as close to ideal turn values as possible
        //System.out.println(ldir+" "+Math.toDegrees(left));
        if (left > Math.PI) {
            left = left - Math.PI;
            ldir = -ldir;
        }
        left = Math.min(left, Rules.MAX_TURN_RATE_RADIANS);
        //System.out.println(ldir+" "+Math.toDegrees(left));
        //System.out.println(rdir+" "+Math.toDegrees(right));
        if (right > Math.PI) {
            right = right - Math.PI;
            rdir = -rdir;
        }
        right = Math.min(right, Rules.MAX_TURN_RATE_RADIANS);
        //System.out.println(rdir+" "+Math.toDegrees(right));
        // turn turn values in headings
        left = Utility.fixAngle(getHeading()+(ldir*left));
        right = Utility.fixAngle(getHeading()+(rdir*right));
        //System.out.println(Math.toDegrees(left)+" "+Math.toDegrees(right));
        //System.out.println("---");
        // determine whether going forwards or backwards maximises lateralness
        if (direction == 1) {
            if (Utility.lateral(max, right) > Utility.lateral(min, left)) {
                retval.setHeading(right);
                retval.setSpeed(max);
            } else {
                retval.setHeading(left);
                retval.setSpeed(min);
            }
        } else {
            if (Utility.lateral(max, left) < Utility.lateral(min, right)) {
                retval.setHeading(left);
                retval.setSpeed(max);
            } else {
                retval.setHeading(right);
                retval.setSpeed(min);
            }
        }
        // project the point in case we ever need the coorinates
        retval = retval.projectLinear(1);
        return retval;
    }

    public VectorPoint projectLateralMax(DirectedPoint point, int direction, long time) {
        VectorPoint retval = this;
        for (int i = 0; i < time; i++) {
            retval = retval.projectLateralMax(point, direction);
        }
        return retval;
    }

    public String toString() {
        return "("+Math.round(x)+","+Math.round(y)+") bearing "+Math.round(Math.toDegrees(heading))+" going "+Math.round(speed);
    }
}
