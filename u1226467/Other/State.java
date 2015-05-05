package u1226467;

import robocode.*;
import java.util.*;

public class State {
    private static int HISTORY = 16;
    private static int THRESHOLD_180 = 3;

    private Snapshot self;
    private Snapshot[][] history;
    private Map<String, Integer> ids;
    private Set<String> alive;
    private Set<String> dead;
    private int id;
    private int remaining;
    private int total;
    private long time;

    State(String name, int robots) {
        this(name, robots, 0);
    }

    State(String name, int robots, long time) {
        history = new Snapshot[HISTORY][robots];
        ids = new HashMap<String, Integer>(robots);
        alive = new HashSet<String>();
        dead = new HashSet<String>();
        remaining = robots;
        total = robots;
        this.time = time;
        id = 0;
    }

    public void reset() {
        history = new Snapshot[HISTORY][total];
        alive.addAll(dead);
        dead.clear();
        remaining = total;
        time = 0;
    }

    public boolean isAlive(String name) {
        return alive.contains(name);
    }

    //todo:scope
    public Set<String> getAlive() {
        return alive;
    }

    public boolean isDead(String name) {
        return dead.contains(name);
    }

    //todo:scope
    public Set<String> getDead() {
        return dead;
    }

    public void update() {
        update(time+1);
    }

    public void update(long time) {
        int timedelta = (int) (time - this.time);
        Snapshot[][] temp = new Snapshot[HISTORY][total];
        if (timedelta >= HISTORY) {
            history = temp;
        } else {
            for (int i = HISTORY-1; i >= timedelta; i--) {
                history[i] = history[i-1];
            }
            for (int i = 0; i < timedelta; i++) {
                history[i] = new Snapshot[total];
            }
        }
        this.time = time;
    }

    public int getId(String name) {
        if (name == null) {
            return 0;
        }
        if (ids.get(name) == null) {
            ids.put(name, id++);
            return id-1;
        } else {
            return ids.get(name);
        }
    }

    public int getScanned() {
        return getScanned(2);
    }

    public int getScanned(int threshold) {
        int retval = 0;
        int max = Math.min(HISTORY, threshold);
        for (int i = 1; i < id; i++) {
            for (int j = 0; j < max; j++) {
                if (history[j][i] != null) {
                    retval++;
                    break;
                }
            }
        }
        return retval;
    }

    public void onRobotDeath(RobotDeathEvent e) {
        if (alive.contains(e.getName())) {
            alive.remove(e.getName());
            dead.add(e.getName());
        }
        int id = getId(e.getName());
        remaining--;
        for (int i = 0; i < HISTORY; i++) {
            history[i][id] = null;
        }
    }

    public int getRemaining() {
        return remaining-1;
    }

    public Set<String> getScannedNames() {
        return getScannedNames(2);
    }

    public Set<String> getScannedNames(int threshold) {
        Set<String> names = new HashSet<String>();
        int max = Math.min(HISTORY, threshold);
        for (int i = 1; i < id; i++) {
            for (int j = 0; j < max; j++) {
                if (history[j][i] != null) {
                    names.add(history[j][i].getName());
                }
            }
        }
        return names;
    }

    public Set<Snapshot> getLatestSnapshots() {
        return getLatestSnapshots(HISTORY);
    }

    public Set<Snapshot> getLatestSnapshots(int threshold) {
        Set<Snapshot> snapshots = new HashSet<Snapshot>();
        int max = Math.min(HISTORY, threshold);
        for (int i = 1; i < id; i++) {
            for (int j = 0; j < max; j++) {
                if (history[j][i] != null) {
                    snapshots.add(history[j][i]);
                }
            }
        }
        return snapshots;
    }

    public void addSelf(Snapshot snapshot) {
        self = snapshot;
    }

    public void addSnapshot(Snapshot snapshot) {
        if (snapshot.getTime() == time) {
            history[0][getId(snapshot.getName())] = snapshot;
            if (!alive.contains(snapshot.getName())) {
                alive.add(snapshot.getName());
            }
        } else {
            //System.out.println("[State] my time: " + time + ", snapshot time: " + snapshot.getTime());
        }
    }

    public Snapshot getSnapshot(String name) {
        return getSnapshot(getId(name));
    }

    public Snapshot getPreviousSnapshot(String name) {
        return getPreviousSnapshot(getId(name));
    }

    public Snapshot getSnapshot(int id) {
        if (id >= this.id) {
            return null;
        }
        for (int i = 0; i < HISTORY; i++) {
            if (history[i][id] != null) {
                return history[i][id];
            }
        }
        return null;
    }

    public Snapshot getPreviousSnapshot(int id) {
        boolean previous = false;
        if (id >= this.id) {
            return null;
        }
        for (int i = 0; i < HISTORY; i++) {
            if (history[i][id] != null) {
                if (previous) {
                    return history[i][id];
                } else {
                    previous = true;
                }
            }
        }
        return null;
    }

    public Snapshot getSelf() {
        if (history[0][0] == null) {
            return history[0][1];
        }
        return history[0][0];
    }

    public double[] getArc() {
        return getArc(0);
    }

    public double[] getArc(double heading) {
        DirectedPoint self = new DirectedPoint(history[0][0]);
        self.setHeading(heading);
        return getArc(self);
    }

    public double[] getArc(DirectedPoint radar) {
        long time = 1;
        VectorPoint self = history[0][0];
        Snapshot enemy;
        VectorPoint point;
        double temp;
        double left;
        double right;
        double[] retval = {Double.NaN, Double.NaN};

        // initial population, very crude estimates with only two ticks of projection
        for (int i = 1; i < id; i++) {
            enemy = getSnapshot(i);
            if (enemy != null) {
                //left = radar.getBearingTo(enemy.projectLateralMax(self, -1, time)) + 0;
                //right = radar.getBearingTo(enemy.projectLateralMax(self, 1, time)) + 0;
                //System.out.println("[State] Arc member: " + enemy.name+" ("+Math.toDegrees(radar.getBearingTo(enemy))+"): "+Math.toDegrees(left)+" - "+Math.toDegrees(right));
                left = radar.getBearingTo(enemy.projectLateral(self, -1, time)) + 0;
                right = radar.getBearingTo(enemy.projectLateral(self, 1, time)) + 0;
                //System.out.println("[State] Arc member: " + enemy.name+" ("+Math.toDegrees(radar.getBearingTo(enemy))+"): "+Math.toDegrees(left)+" - "+Math.toDegrees(right));
                if (Utility.angleBetween(left, right) > Utility.angleBetween(right, left)) {
                    temp = left;
                    left = right;
                    right = temp;
                    //System.out.println("[State] getArc switch");
                }
                //System.out.println("[State] Arc member: " + enemy.name+" ("+Math.toDegrees(radar.getBearingTo(enemy))+"): "+Math.toDegrees(left)+" - "+Math.toDegrees(right));
                if (Double.isNaN(retval[0])) {
                    retval[0] = left;
                    retval[1] = right;
                } else {
                    if (Utility.isAngleBetween(left, retval[0], retval[1])) {
                        if (Utility.isAngleBetween(right, retval[0], retval[1])) {
                            continue;
                        } else {
                            retval[1] = right;
                        }
                    } else if (Utility.isAngleBetween(right, retval[0], retval[1])) {
                        retval[0] = left;
                    } else if (Utility.angleBetween(retval[1], left) <
                               Utility.angleBetween(right, retval[0])) {
                        retval[1] = right;
                    } else {
                        retval[0] = left;
                    }
                }
            }
        }
        return retval;
    }

    public String toString() {
        String retval = "State:";
        for (int i = 0; i < id; i++) {
            retval += "\r\n  " + i + " -> " + getSnapshot(i);
        }
        return retval;
    }
}