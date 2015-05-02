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

    public VectorPoint projectLateralNew(VectorPoint target, int direction, long time) {
        VectorPoint retval = this;
        for (int i = 0; i < time; i++) {
            retval = retval.projectLateralNew(target, direction);
        }
        return retval;
    }

    public VectorPoint projectLateralNew(VectorPoint target, int direction) {
        return projectLateralNew(this, target, direction);
    }

    public VectorPoint projectLateralNew(VectorPoint source, VectorPoint target, int direction) {
        VectorPoint origin = new VectorPoint(source);
        origin.project();
        VectorPoint retval = new VectorPoint(source);
        // speed of target
        double max;
        double min;
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
        // bearing of target
        double bearing = target.getNorthBearingTo(origin);
        double heading = target.getHeading();
        double temp;
        double speed;
        int dir = 1;
        double distance = target.distanceTo(origin);
        // heading is "right" of bearing to origin, target rotating "left" around origin
        if (Utility.isAngleBetween(heading, bearing, Utility.fixAngle(bearing+Math.PI))) {
            if (direction == -1) {
                max = max;
                speed = max;
                heading = heading;
            } else {
                max = -min;
                speed = min;
                heading = Utility.fixAngle(heading+Math.PI);
            }
            temp = Math.acos(1-Math.pow(max, 2)/(2*Math.pow(distance, 2)));
            temp = bearing + (Math.PI - temp)/2;
            if (max >= 0) {
                // approach optimal angle
                if (Utility.isAngleBetween(heading, bearing, temp)) {
                    temp = temp-heading;
                    dir = 1;
                } else {
                    temp = heading-temp;
                    dir = -1;
                }
            } else if (max < 0) {
                if (Utility.isAngleBetween(heading, bearing, Utility.fixAngle(bearing+Math.PI/2))) {
                    dir = -1;
                    temp = Utility.fixAngle(heading-bearing);
                } else {
                    dir = 1;
                    temp = Utility.fixAngle(bearing-bearing);
                }
            }
        // heading is "left" of bearing to origin, target rotating "right" around origin
        } else {
            if (direction == 1) {
                max = max;
                speed = max;
                heading = heading;
            } else {
                max = -min;
                speed = min;
                heading = Utility.fixAngle(heading+Math.PI);
            }
            temp = Math.acos(1-Math.pow(max, 2)/(2*Math.pow(distance, 2)));
            temp = bearing - (Math.PI - temp)/2;
            if (max >= 0) {
                // approach optimal angle
                if (Utility.isAngleBetween(heading, bearing, temp)) {
                    temp = temp-heading;
                    dir = -1;
                } else {
                    temp = heading-temp;
                    dir = 1;
                }
            } else if (max < 0) {
                if (Utility.isAngleBetween(heading, bearing, Utility.fixAngle(bearing+Math.PI/2))) {
                    dir = 1;
                    temp = Utility.fixAngle(heading-bearing);
                } else {
                    dir = -1;
                    temp = Utility.fixAngle(bearing-bearing);
                }
            }
        }
        temp = Math.min(Rules.MAX_TURN_RATE_RADIANS, temp);
        if (dir == 1) {
            retval.setHeading(Utility.fixAngle(heading + temp));
        } else {
            retval.setHeading(Utility.fixAngle(heading - temp));
        }
        retval.setSpeed(speed);
        retval = retval.project();
        return retval;
    }

    public VectorPoint projectLateralMax(DirectedPoint point, int direction) {
        VectorPoint retval = new VectorPoint(this);
        double bearing = point.getBearingTo(this);
        //System.out.println("---");
        //System.out.println(Math.toDegrees(point.getHeading())+" "+Math.toDegrees(bearing));
        double left = Utility.fixAngle(bearing - Math.PI/2 - getHeading());
        int ldir = 1;
        double right = Utility.fixAngle(bearing + Math.PI/2 - getHeading());
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
