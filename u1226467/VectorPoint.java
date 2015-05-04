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

    /**
     * Returns the VectorPoint's speed
     * @return speed
     */
    public double getSpeed() {
        return speed;
    }

    /**
     * Sets the VectorPoint's speed
     * @param speed speed
     */
    public void setSpeed(double speed) {
        this.speed = speed;
    }

    /**
     * Sets the vector (heading, speed) of the VectorPoint.
     * @param heading heading of the vector in radians
     * @param speed   speed
     */
    public void setVector(double heading, double speed) {
        this.heading = heading;
        this.speed = speed;
    }

    /**
     * Performs a 1-step projection of the VectorPoint using the linear
     * projection method.
     * See {@link #projectLinear(long)}.
     * @return projected point
     */
    public VectorPoint project() {
        return projectLinear(1);
    }

    /**
     * Performs a projection of the VectorPoint using linear projection.
     * @param  time number of steps over which to project
     * @return      projected point
     */
    public VectorPoint projectLinear(long time) {
        double x = this.x + Math.sin(heading)*speed*time;
        double y = this.y + Math.cos(heading)*speed*time;
        return new VectorPoint(x, y, heading, speed);
    }

    /**
     * Performs the minimal linear projection of the VectorPoint.
     * @param  time number of steps over which to project
     * @return      projected point
     */
    public VectorPoint projectLinearMin(long time) {
        double distance = 0;
        double speed = this.speed;
        double x, y;
        for (int i = 0; i < time; i++) {
            if (speed > 0) {
                speed = Math.max(0, speed - Rules.DECELERATION);
            } else if (speed <= 0) {
                speed = Math.max(-Rules.MAX_VELOCITY,
                                 speed - Rules.ACCELERATION);
            }
            distance += speed;
        }
        x = this.x + Math.sin(heading)*distance;
        y = this.y + Math.cos(heading)*distance;
        return new VectorPoint(x, y, heading, speed);
    }

    /**
     * Performs the maximal linear projection of the VectorPoint.
     * @param  time number of steps over which to project
     * @return      projected point
     */
    public VectorPoint projectLinearMax(long time) {
        double distance = 0;
        double speed = this.speed;
        double x, y;
        for (int i = 0; i < time; i++) {
            if (speed < 0) {
                speed = Math.min(0, speed + Rules.DECELERATION);
            } else if (speed < Rules.MAX_VELOCITY) {
                speed = Math.min(Rules.MAX_VELOCITY,
                                 speed + Rules.ACCELERATION);
            }
            distance += speed;
        }
        x = this.x + Math.sin(heading)*distance;
        y = this.y + Math.cos(heading)*distance;
        return new VectorPoint(x, y, heading, speed);
    }

    /**
     * Performs a worst-case lateral projection of a given target VectorPoint
     * around the current VectorPoint. Effectively helps determine the maximum
     * angular change to face the target point in either direction.
     * See {@link #projectLateral(VectorPoint, int)}.
     * @param  target    target VectorPoint
     * @param  direction direction of simulation, clockwise being 1,
     *                   anticlockise being -1
     * @param  time      number of steps over which to project
     * @return           projectedd point
     */
    public VectorPoint projectLateral(VectorPoint target, int direction,
                                      long time) {
        VectorPoint retval = this;
        for (int i = 0; i < time; i++) {
            retval = retval.projectLateral(target, direction);
        }
        return retval;
    }

    /**
     * Performs a worst-case lateral projection of a given target VectorPoint
     * around the current VectorPoint. Effectively helps determine the maximum
     * angular change to face the target point in either direction.
     * @param  target    target VectorPoint
     * @param  direction direction of simulation, clockwise being 1,
     *                   anticlockise being -1
     * @return           projectedd point
     */
    public VectorPoint projectLateral(VectorPoint target, int direction) {
        VectorPoint retval = new VectorPoint(this);
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
        double bearing = target.getNorthBearingTo(this);
        double heading = target.getHeading();
        double temp;
        double speed;
        int dir = 1;
        double distance = target.distanceTo(this);
        // heading is "right" of the origin, target rotating "left"
        if (Utility.isAngleBetween(heading, bearing,
                                   Utility.fixAngle(bearing+Math.PI))) {
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
                if (Utility.isAngleBetween(heading, bearing,
                    Utility.fixAngle(bearing+Math.PI/2))) {
                    dir = -1;
                    temp = Utility.fixAngle(heading-bearing);
                } else {
                    dir = 1;
                    temp = Utility.fixAngle(bearing-bearing);
                }
            }
        // heading is "left" of the origin, target rotating "right"
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
                if (Utility.isAngleBetween(heading, bearing,
                    Utility.fixAngle(bearing+Math.PI/2))) {
                    dir = 1;
                    temp = Utility.fixAngle(heading-bearing);
                } else {
                    dir = -1;
                    temp = Utility.fixAngle(bearing-bearing);
                }
            }
        }
        temp = Math.min(Utility.maxTurn(Math.abs(speed)), temp);
        if (dir == 1) {
            retval.setHeading(Utility.fixAngle(heading + temp));
        } else {
            retval.setHeading(Utility.fixAngle(heading - temp));
        }
        retval.setSpeed(speed);
        retval = retval.project();
        return retval;
    }

    public String toString() {
        return "("+Math.round(x)+","+Math.round(y)+") bearing "
            +Math.round(Math.toDegrees(heading))+" going "+Math.round(speed);
    }
}
