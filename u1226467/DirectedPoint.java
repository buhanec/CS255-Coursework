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

    public double getHeading() {
        return heading;
    }

    public void setHeading(double heading) {
        this.heading = heading;
    }

    public void setHeading(DirectedPoint observer, double bearing) {
        this.heading = (observer.getHeading() + bearing + 2*Math.PI)%(2*Math.PI);
    }

    public double getBearingTo(DirectedPoint point) {
        return getBearingTo(point.getX(), point.getY());
    }

    public double getBearingTo(Point point) {
        return getBearingTo(point.getX(), point.getY());
    }

    public double getBearingTo(double x, double y) {
        double dx = x - this.x;
        double dy = y - this.y;
        double theta = Math.atan2(dy, dx);
        if (x == 0) {
            if (y < 0) {
                return Math.PI;
            } else {
                return 0;
            }
        } else if (x > 0) {
            theta = Math.PI/2 - theta;
        } else {
            theta = Math.PI*2.5 - theta;
        }
        return (theta+2*Math.PI)%(2*Math.PI);
    }

    public double getRelativeBearingTo(DirectedPoint point) {
        return getRelativeBearingTo(point.getX(), point.getY());
    }

    public double getRelativeBearingTo(Point point) {
        return getRelativeBearingTo(point.getX(), point.getY());
    }

    public double getRelativeBearingTo(double x, double y) {
        double dx = x - this.x;
        double dy = y - this.y;
        return Math.atan2(dy, dx);
    }

    public String toString() {
        return "("+Math.round(x)+","+Math.round(y)+") bearing "+Math.round(Math.toDegrees(heading));
    }

    public double lateralOf(VectorPoint vector) {
        return vector.getSpeed() * Math.sin(getBearingTo(vector));
    }
}
