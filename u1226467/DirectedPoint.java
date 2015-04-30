package u1226467;

import java.awt.geom.*;

public class DirectedPoint {
    private double x;
    private double y;
    private double heading;

    DirectedPoint(double x, double y, double heading) {
        this.x = x;
        this.y = y;
        this.heading = heading;
    }

    DirectedPoint(Point2D.Double point, double heading) {
        this.point = point;
        this.angle = new Angle2D.Double();
        this(point.getX(), point.getY(), heading, speed);
    }

    DirectedPoint(DirectedPoint point) {
        x = point.x;
        y = point.y;
        heading = point.heading;
    }

    public Point2D.Double getPoint() {
        return new Point2D.Double(x, y);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getHeading() {
        return heading;
    }

    public void setPoint(Point2D.Double point) {
        this.x = point.getX();
        this.y = point.getY();
    }

    public void setPoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void setHeading(double heading) {
        this.heading = heading;
        this.speed = speed;
    }

    public void setHeading(DirectedPoint observer, double bearing) {
        this.heading = (observer.getHeading() + bearing)%(Math.PI*2);
    }

    public double getBearingTo(DirectedPoint point) {
        return getBearingTo(point.getX(), point.getY());
    }

    public double getBearingTo(Point2D.Double point) {
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
        return theta%(Math.PI*2);
    }

    public double getBearingToAlt(DirectedPoint point) {
        return getBearingToAlt(point.getX(), point.getY());
    }

    public double getBearingToAlt(Point2D.Double point) {
        return getBearingToAlt(point.getX(), point.getY());
    }

    public double getBearingToAlt(double x, double y) {
        double dx = x - this.x;
        double dy = y - this.y;
        return theta = Math.atan2(dy, dx);
    }
}
