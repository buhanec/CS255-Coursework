package u1226467;

public class Rectangle extends Point {
    protected double x;
    protected double y;
    protected double w;
    protected double h;

    Rectangle(double x, double y, double width, double height) {
        super(x, y);
        w = width;
        h = height;
    }

    public boolean contains(Point point) {
        return (Utility.containedii(point.getX(), x, x+w) &&
                Utility.containedii(point.getY(), y, y+h));
    }

    public double getWidth() {
        return w;
    }

    public double getHeight() {
        return h;
    }
}
