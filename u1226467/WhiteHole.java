package u1226467;

import robocode.*;
import java.util.*;

public class WhiteHole implements Pilot {
    private State state;
    private Rectangle arena;
    private Robot robot;
    private Map<String, Double> forces;
    private Map<String, Integer> attempts;
    private Map<String, Integer> success;

    private int forwards = 1;
    private Map<Point, Double> extraForces;

    WhiteHole(Robot robot, State state, Rectangle arena) {
        this.robot = robot;
        this.state = state;
        this.arena = arena;
    }

    public void update(long time) {

    }

    public void move() {

    }
}
