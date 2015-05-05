package u1226467;

public class Point {
    protected double x;
    protected double y;

    Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    Point(Point point) {
        x = point.getX();
        y = point.getY();
    }

    /**
     * Returns a Point's x-coordinate.
     * @return x-coorindate
     */
    public double getX() {
        return x;
    }

    /**
     * Returns a Point's y-coordinate.
     * @return y-coordinate
     */
    public double getY() {
        return y;
    }

    /**
     * Sets the Point's x- and y-coordinates.
     * @param x x-coordinate
     * @param y y-coordinate
     */
    public void setPoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Sets the Point's x- and y-coordinates.
     * @param point point from which to copy the x- and y-coordinates
     */
    public void setPoint(Point point) {
        x = point.getX();
        y = point.getY();
    }

    /**
     * Returns the angle between the y-axis and a given point, with the current
     * point serving as the vertex of the angle. The angle is measured
     * clockwise.
     * See {@link #getNorthBearingTo(double, double)}.
     * @param  point point to which the angle is measured
     * @return       angle in radians
     */
    public double getNorthBearingTo(Point point) {
        return getNorthBearingTo(point.getX(), point.getY());
    }

    /**
     * Returns the angle between the y-axis and a point give by its coorinates,
     * with the current point serving as the vertex of the angle. The angle is
     * measured clockwise.
     * @param  x x-coordinate of point to which to measure
     * @param  y y-coordinate of point to which to measure
     * @return   angle in radians
     */
    public double getNorthBearingTo(double x, double y) {
        double dx = x - this.x;
        double dy = y - this.y;
        if (dx == 0) {
            if (dy < 0) {
                return Math.PI;
            } else {
                return 0;
            }
        }
        return Utility.fixAngle(-Utility.fixAngle(Math.atan2(dy, dx)-Math.PI/2));
        //} else if (x > 0) {
        //    return Utility.fixAngle(Math.PI/2 - Math.atan2(dy, dx));
        //} else {
        //    return Utility.fixAngle(Math.PI*2.5 - Math.atan2(dy, dx));
        //}
    }

    /**
     * Returns the distance between the current point and the target point.
     * See {@link #distanceTo(double, double)}.
     * @param  point point to which to measure
     * @return       distance to point
     */
    public double distanceTo(Point point) {
        return Math.sqrt(Math.pow((point.getX()-getX()), 2) +
                         Math.pow((point.getY() - getY()), 2));
    }

    /**
     * Returns the distance between the current point and the target point
     * defined by the given coordinates.
     * @param  x x-coordinate of the point to which to measure
     * @param  y y-coordinate of the point to which to measure
     * @return   distance to point
     */
    public double distanceTo(double x, double y) {
        return Math.sqrt(Math.pow((x - getX()), 2) +
                         Math.pow((y - getY()), 2));
    }

    public String toString() {
        return "("+Math.round(x)+","+Math.round(y)+")";
    }

}
