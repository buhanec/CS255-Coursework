package u1226467;

import robocode.*;
import java.util.*;

public class State {
    private static int HISTORY = 8;

    private Snapshot[][] history;
    private boolean[] active;
    private Map<String, Integer> ids;
    private int id;
    private int remaining;
    private int total;
    private long time;

    State(int robots) {
        this(robots, 0);
    }

    State(int robots, long time) {
        history = new Snapshot[robots][HISTORY];
        active = new boolean[robots];
        for (int i = 0; i < robots; i++) {
            active[i] = true;
        }
        ids = new HashMap<String, Integer>(robots);
        remaining = robots;
        total = robots;
        this.time = time;
        id = 0;
    }

    public void update() {
        previous = current;
        current = new Snapshot[previous.length];
        time++;
    }

    public int getId(String name) {
        if (ids.get(name) == null) {
            ids.put(name, id++);
            return id-1;
        } else {
            return ids.get(name);
        }
    }

    public int getScanned() {
        int retval = 0;
        for (int i = 1; i < current.length; i++) {
            if (current[i] != null) {
                retval++;
            }
        }
        return retval;
    }

    public int getRemaining() {
        return remaining-1;
    }

    public void addSnapshot(Snapshot snapshot) {
        if (snapshot.getTime() == time) {
            current[getId(snapshot.getName())] = snapshot;
        }
    }

    public Snapshot getSnapshot(String name) {
        return current[getId(name)];
    }

    public Snapshot getPreviousSnapshot(String name) {
        return previous[getId(name)];
    }

    public double[] radar180() {
        long time = 2;
        VectorPoint point;
        VectorPoint enemy;
        double angle;
        SortedMap<Double, VectorPoint> right = new TreeMap<Double, VectorPoint>();
        SortedMap<Double, VectorPoint> left = new TreeMap<Double, VectorPoint>();
        double[] retval = new double[2];

        // initial population, very crude estimates with only two ticks of projection
        for (int i = 1; i == current.length; i++) {
            enemy = null;
            if (current[i] != null) {
                enemy = current[i];
            } else if (previous[i] != null) {
                enemy = previous[i].project();
            }
            if (enemy != null) {
                point = enemy.projectLateralMax(current[0], 1, time);
                angle = current[0].getBearingTo(point);
                if (Utility.containedii(angle, 0, Math.PI)) {
                    right.put(angle, point);
                } else {
                    left.put(angle, point);
                }
                point = enemy.projectLateralMax(current[0], 1, time);
                angle = current[0].getBearingTo(point);
                if (Utility.containedii(angle, 0, Math.PI)) {
                    right.put(angle, point);
                } else {
                    left.put(angle, point);
                }
            }
        }
        retval[0] = left.firstKey();
        retval[1] = right.lastKey();
        return retval;
    }

    public String toString() {
        String retval = "Current: \r\n";
        for (int i = 0; i < current.length; i++) {
            if (current[i] != null) {
                retval += "    " + i + "-> " + current[i].getName() + "\r\n";
            } else {
                retval += "    " + i + "-> null\r\n";
            }
        }
        retval += "Previous: \r\n";
        for (int i = 0; i < previous.length; i++) {
            if (previous[i] != null) {
                retval += "    " + i + "-> " + previous[i].getName() + "\r\n";
            } else {
                retval += "    " + i + "-> null\r\n";
            }
        }
        retval += "id: " + id;
        return retval;
    }
}
