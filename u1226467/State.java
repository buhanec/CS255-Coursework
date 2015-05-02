package u1226467;

import robocode.*;
import java.util.*;

public class State {
    private static int HISTORY = 8;
    private static int THRESHOLD_180 = 3;

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
        history = new Snapshot[HISTORY][robots];
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
        if (ids.get(name) == null) {
            ids.put(name, id++);
            System.out.println("[State] Assigned "+name+" id "+(id-1));
            return id-1;
        } else {
            System.out.println("[State] Fetched "+name+" id "+(ids.get(name)));
            return ids.get(name);
        }
    }

    public int getScanned() {
        return getScanned(2);
    }

    public int getScanned(int threshold) {
        int retval = 0;
        int max = Math.min(HISTORY, threshold);
        for (int i = 0; i < id; i++) {
            for (int j = 0; j < max; j++) {
                if (history[j][i] != null) {
                    retval++;
                    break;
                }
            }
        }
        return retval;
    }

    public int getRemaining() {
        return remaining-1;
    }

    public void addSnapshot(Snapshot snapshot) {
        if (snapshot.getTime() == time) {
            history[0][getId(snapshot.getName())] = snapshot;
        } else {
            System.out.println("[State] my time: " + time + ", snapshot time: " + snapshot.getTime());
        }
    }

    public Snapshot getSnapshot(String name) {
        return getSnapshot(getId(name));
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

    public double[] radar180() {
        long time = 1;
        VectorPoint self = history[0][0];
        VectorPoint enemy;
        VectorPoint point;
        double left;
        double right;
        double[] retval = {Double.NaN, Double.NaN};

        // initial population, very crude estimates with only two ticks of projection
        // assume robot position cannot vary more than Math.PI
        for (int i = 1; i < id; i++) {
            enemy = getSnapshot(i);
            if (enemy != null) {
                System.out.println("DEBUG: left");
                left = self.getBearingTo(enemy.projectLateralMax(self, -1, time));
                System.out.println("DEBUG: right");
                right = self.getBearingTo(enemy.projectLateralMax(self, 1, time));
                System.out.println("DEBUG1: "+Math.toDegrees(self.getBearingTo(enemy.project())));
                System.out.println("DEBUG2: "+Math.toDegrees(left));
                System.out.println("DEBUG3: "+Math.toDegrees(right));
                if (retval[0] == Double.NaN) {
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
                System.out.println("DEBUG4: "+Math.toDegrees(retval[0])+" - "+Math.toDegrees(retval[1]));
            }
        }
        return retval;
    }

    public String toString() {
        String retval = "-----------------------\r\n";
        for (int i = 0; i < id; i++) {
            retval += i + " -> " + getSnapshot(i) + "\r\n";
        }
        retval += "id: " + id + "\r\n";
        retval += "-----------------------";
        return retval;
    }
}
