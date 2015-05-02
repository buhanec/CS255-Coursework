package u1226467;

import robocode.*;
import java.util.*;

public class SurfPilot {
    protected static int BINS = 47;
    protected static double stats[] = new double[BINS];

    protected State state;
    protected String target;

    protected List<Double> waves;
    protected List<Double> directions;
    protected List<Double> bearings;

    SurfPilot() {
        waves = new ArrayList<Double>();
        directions = new ArrayList<Double>();
        bearings = new ArrayList<Double>();
    }

    public void onScannedRobot(ScannedRobotEvent e) {

    }
}
