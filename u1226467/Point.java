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

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setPoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void setPoint(Point point) {
        x = point.getX();
        y = point.getY();
    }

    public String toString() {
        return "("+Math.round(x)+","+Math.round(y)+")";
    }

    public double distanceTo(Point point) {
        return Math.sqrt(Math.pow((point.getX()-getX()), 2) + Math.pow((point.getY() - getY()), 2));
    }
}
