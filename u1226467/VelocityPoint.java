package u1226467;

import java.awt.geom.*;

public class VectorPoint extends DirectedPoint {
    private double value;
    private double min;
    private double max;

    VectorPoint(double x, double y, double heading, double value, double min, double max) {
        super(x, y, heading);
        this.value = value;
        this.min = min;
        this.max = max;
    }

    VectorPoint(Point2D.Double point, double heading, double value, double min, double max) {
        super(point, heading);
        this.value = value;
        this.min = min;
        this.max = max;
    }

    VectorPoint(double x, double y, double heading, double value) {
        this(x, y, heading, value, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    VectorPoint(Point2D.Double point, double heading, double value) {
        this(point, heading, value, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    VectorPoint(VectorPoint vector) {
        super(vector);
        value = vector.value;
        value = vector.min;
        value = vector.max;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public void setConstraints(double min, double max) {
        this.min = min;
        this.max = max;
    }

    public void setMin(double min) {
        this.min = min;
    }

    public void setMax(double max) {
        this.max = max;
    }

    public double setVector(double heading, double value) {
        this.heading = heading;
        this.speed = speed;
    }

    private VectorPoint project(long scale, double value) {
        VectorPoint vector = new VectorPoint(this);
        vector.setPoint(x + Math.sin(heading)*value*scale,
                        y + Math.cos(heading)*value*scale);
        return vector;
    }

    public VectorPoint project() {
        return project(1);
    }

    public VectorPoint projectMin() {
        return projectMin(1);
    }

    public VectorPoint projectMax() {
        return projectMax(1);
    }

    public VectorPoint project(long scale) {
        return project(scale, value);
    }

    public VectorPoint projectMin(long scale) {
        return project(scale, min);
    }

    public VectorPoint projectMax(long scale) {
        return project(scale, max);
    }

    public VectorPoint projectMaxLateral(VectorPoint vector, int direction) {
        double maxturn = 10;
        double bearing = getBearingTo(vector);
        double right = Math.min(Math.abs(Math.PI/2 - bearing), maxturn);
        double left = Math.min(Math.abs(3*Math.PI/2 - bearing), maxturn);
        VectorPoint retval = new VectorPoint(this);
        if (bearing > 3*Math.PI/2 || bearing < Math.PI/2) {
            right = bearing + right;
            left = bearing - left;
        } else {
            right = bearing - right;
            left = bearing + left;
        }
        // Clockwise
        if (direction == 1) {
            if (Utility.lateral(max, right) > Utility.lateral(min, left)) {
                retval.setHeading(this, right);
                retval.setSpeed(max);
            } else {
                retval.setHeading(this, left);
                retval.setSpeed(min);
            }
        // Anticlockwise
        } else {
            if (Utility.lateral(max, left) > Utility.lateral(min, right)) {
                retval.setHeading(this, left);
                retval.setSpeed(max);
            } else {
                retval.setHeading(this, right);
                retval.setSpeed(min);
            }
        }
        return retval;
    }

    public double lateralOf(VectorPoint vector) {
        return vector.getValue() * Math.sin(getBearingTo(vector));
    }
}
