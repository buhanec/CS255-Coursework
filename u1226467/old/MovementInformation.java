package u1226467;

public class MovementInformation {
    public final double angle;
    public final double distance;
    public final int angleDirection;
    public final int distanceDirection;

    MovementInformation(double angle, double distance, int angleDirection, int distanceDirection) {
        this.angle = angle;
        this.distance = distance;
        this.angleDirection = angleDirection;
        this.distanceDirection = distanceDirection;
    }
}
