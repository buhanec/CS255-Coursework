package u1226467;

public class Wave extends DirectedPoint {
    protected double r;
    protected double speed;
    protected double direction;
    protected long time;

    Wave(Point origin, double speed, double direction, double heading, long time) {
        super(origin, heading);
        this.r = speed;
        this.speed = speed;
        this.direction = direction;
        this.time = time;
    }

    public void update(long time) {
        r = (time-this.time)*speed;
        this.time = time;
    }

    public double getRadius() {
        return r;
    }

    public double getSpeed() {
        return speed;
    }

    public double getDirection() {
        return direction;
    }
}
