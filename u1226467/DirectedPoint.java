package u1226467;

public class DirectedPoint extends Point {
    protected double heading;

    DirectedPoint(double x, double y, double heading) {
        super(x, y);
        this.heading = heading;
    }

    DirectedPoint(Point point, double heading) {
        super(point);
        this.heading = heading;
    }

    DirectedPoint(DirectedPoint point) {
        super(point);
        heading = point.getHeading();
    }

    /**
     * Returns the DirectedPoint's heading.
     * @return heading in radians
     */
    public double getHeading() {
        return heading;
    }

    /**
     * Sets the DirectPoint's heading.
     * @param heading heading in radians
     */
    public void setHeading(double heading) {
        this.heading = heading;
    }

    /**
     * Sets the DirectPoint's heading given an observer and relative bearing of
     * the heading to that observer.
     * @param observer observer point
     * @param bearing  observed relative heading of this point
     */
    public void setHeading(DirectedPoint observer, double bearing) {
        this.heading = Utility.fixAngle(observer.getHeading() + bearing);
    }

    /**
     * Gets the bearing to a given Point.
     * See {@link #getBearingTo(double, double)}.
     * @param  point point to measure to
     * @return       bearing in radians
     */
    public double getBearingTo(Point point) {
        return getBearingTo(point.getX(), point.getY());
    }

    /**
     * Gets the bearing to a given point defined by its coordinates.
     * @param  x x-coordinate of the point to measure to
     * @param  y y-coordinate of the point to measure to
     * @return   bearing in radians
     */
    public double getBearingTo(double x, double y) {
        return Utility.fixAngle(getNorthBearingTo(x, y)-getHeading());
    }

    public String toString() {
        return "("+Math.round(x)+","+Math.round(y)+") bearing "+
            Math.round(Math.toDegrees(heading));
    }
}
