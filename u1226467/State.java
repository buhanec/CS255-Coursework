package u1226467;

import robocode.*;
import java.util.*;

public class State {
    private Map<String, List<Snapshot>> history;
    private Map<Bullet, String> bullets;
    private Set<String> alive;
    private Set<String> dead;
    private Map<String, Double> hit;
    private Map<String, Double> missed;
    private Map<String, Double> threat;
    private double totalThreat;

    private SortedMap<Double, String> sorted;

    private String self;
    private int remaining;
    private int total;
    private long time;

    State(String self, int robots) {
        this(self, robots, 0);
    }

    State(String self, int robots, long time) {
        history = new HashMap<String, List<Snapshot>>();
        bullets = new HashMap<Bullet, String>();
        alive = new HashSet<String>();
        dead = new HashSet<String>();
        hit = new HashMap<String, Double>();
        missed = new HashMap<String, Double>();
        threat = new HashMap<String, Double>();
        sorted = new TreeMap<Double, String>();
        remaining = robots;
        total = robots;
        this.time = time;
        this.self = self;
    }

    public void reset() {
        history = new HashMap<String, List<Snapshot>>();
        bullets = new HashMap<Bullet, String>();
        alive.addAll(dead);
        dead.clear();
        remaining = total;
        time = 0;
        for (String name : missed.keySet()) {
            System.out.println(name+": "+hit.get(name)+"/"+missed.get(name));
        }
    }

    /**
     * Confirms whether a robot is alive-
     */
    public boolean isAlive(String name) {
        return alive.contains(name);
    }

    /**
     * Confirmed whether a robot is dead.
     */
    public boolean isDead(String name) {
        return dead.contains(name);
    }

    /**
     * Returns the set of confirmed alive targets.
     */
    public Set<String> getAlive() {
        return Collections.unmodifiableSet(alive);
    }

    /**
     * Returns the set of confirmed dead targets.
     */
    public Set<String> getDead() {
        return Collections.unmodifiableSet(dead);
    }

    /**
     * Updates the storage's clock and sorts the threats in a sorted map for
     * any future expansion of the state's functionality.
     */
    public void update(long time) {
        this.time = time;
        sorted.clear();
        Set<Snapshot> latest = getLatestSnapshots();
        for (Snapshot snapshot : latest) {
            if (snapshot != null) {
                String name = snapshot.getName();
                double threat = threat(name);
                sorted.put(threat, name);
            }
        }
    }

    /**
     * Returns the remaining number of targets.
     */
    public int getRemaining() {
        return remaining-1;
    }

    /**
     * Returns the total number of targets.
     */
    public int getTotal() {
        return total-1;
    }

    /**
     * Returns the number of recently scanned targets.
     */
    public int getScanned() {
        return getScanned(3);
    }

    /**
     * Returns the number of recently scanned targets.
     */
    public int getScanned(int age) {
        int retval = 0;
        for (List<Snapshot> list : history.values()) {
            if (list.size() > 0) {
                Snapshot snapshot = list.get(0);
                if (snapshot.getTime() >= time-age
                    && !snapshot.getName().equals(self)) {
                    retval++;
                }
            }
        }
        return retval;
    }

    /**
     * Returns a set of the names of recently scanned targets.
     */
    public Set<String> getScannedNames() {
        return getScannedNames(3);
    }

    /**
     * Returns a set of the names of recently scanned targets.
     */
    public Set<String> getScannedNames(int age) {
        Set<String> retval = new HashSet<String>();
        for (List<Snapshot> list : history.values()) {
            if (list.size() > 0) {
                Snapshot snapshot = list.get(0);
                if (snapshot.getTime() >= time-age
                    && !snapshot.getName().equals(self)) {
                    retval.add(snapshot.getName());
                }
            }
        }
        return retval;
    }

    /**
     * Returns a set of the newest snapshots.
     */
    public Set<Snapshot> getLatestSnapshots() {
        return getLatestSnapshots(18);
    }

    /**
     * Returns a set of the newest snapshots, with a maximum age specified.
     */
    public Set<Snapshot> getLatestSnapshots(int age) {
        Set<Snapshot> retval = new HashSet<Snapshot>();
        for (List<Snapshot> list : history.values()) {
            if (list.size() > 0) {
                Snapshot snapshot = list.get(0);
                if (snapshot.getTime() >= time-age
                    && !snapshot.getName().equals(self)) {
                    retval.add(list.get(0));
                }
            }
        }
        return retval;
    }

    /**
     * Adds a snapshot to the storage.
     */
    public void addSnapshot(Snapshot snapshot) {
        String name = snapshot.getName();
        alive.add(name);
        if (history.containsKey(name)) {
            List<Snapshot> list = history.get(name);
            if (list.size() > 0) {
                if (list.get(0).getTime() < snapshot.getTime()) {
                    list.add(0, snapshot);
                }
            } else {
                list.add(0, snapshot);
            }
        } else {
            history.put(name, new ArrayList<Snapshot>());
            history.get(name).add(0, snapshot);
        }
        if (!hit.containsKey(name)) {
            hit.put(name, 0D);
            missed.put(name, 0D);
            threat.put(name, 0D);
        }
    }

    /**
     * Returns the newest snapshot of the target.
     */
    public Snapshot getSnapshot(String name) {
        if (history.containsKey(name)) {
            List<Snapshot> list = history.get(name);
            if (list.size() > 0) {
                return list.get(0);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Returns the second newest snapshot of the target.
     */
    public Snapshot getPreviousSnapshot(String name) {
        if (history.containsKey(name)) {
            List<Snapshot> list = history.get(name);
            if (list.size() > 1) {
                return list.get(1);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Returns the robot as a snapshot.
     */
    public Snapshot getSelf() {
        return new Snapshot(getSnapshot(self));
    }

    /**
     * Calculates the smallest arc that contains all the projected enemies in
     * two ticks of time. Performs the most extreme lateral projections of the
     * targets movements and is sufficient for oscillation scanning, even with
     * almost no padding and gun scanning.
     */
    public double[] getArc() {
        return getArc(0, 18);
    }

    /**
     * Calculates the smallest arc that contains all the projected enemies in
     * two ticks of time. Performs the most extreme lateral projections of the
     * targets movements and is sufficient for oscillation scanning, even with
     * almost no padding and gun scanning.
     */
    public double[] getArc(double heading, long age) {
        DirectedPoint point = new DirectedPoint(getSelf());
        point.setHeading(heading);
        return getArc(point, age);
    }

    /**
     * Calculates the smallest arc that contains all the projected enemies in
     * two ticks of time. Performs the most extreme lateral projections of the
     * targets movements and is sufficient for oscillation scanning, even with
     * almost no padding and gun scanning.
     */
    public double[] getArc(DirectedPoint radar, long age) {
        long time = 2;
        VectorPoint origin = getSelf();
        Snapshot enemy;
        VectorPoint point;
        double temp;
        double left;
        double right;
        double[] retval = {Double.NaN, Double.NaN};

        // Iterate through all the known targets
        for (List<Snapshot> list : history.values()) {
            if (list.size() > 0) {
                // If the target is not ourselves and is recent enough, get
                // angles to its worst case projections.
                enemy = list.get(0);
                if (enemy.getTime() >= time-age
                    && !enemy.getName().equals(self)) {
                    left = radar.getBearingTo(enemy.projectLateral(origin,
                                                                   -1, time));
                    right = radar.getBearingTo(enemy.projectLateral(origin,
                                                                    1, time));
                } else {
                    continue;
                }
                // Flip the angles if the projection direction was switched
                if (Utility.angleBetween(left, right)
                    > Utility.angleBetween(right, left)) {
                    temp = left;
                    left = right;
                    right = temp;
                }
                // If this is the first target being scanned assign its
                // projections are the boundaries of the arc
                if (Double.isNaN(retval[0])) {
                    retval[0] = left;
                    retval[1] = right;
                // Determine the least expensive way of adding the target to
                // the arc.
                } else {
                    if (Utility.isAngleBetween(left, retval[0], retval[1])) {
                        if (Utility.isAngleBetween(right, retval[0],
                                                   retval[1])) {
                            continue;
                        } else {
                            retval[1] = right;
                        }
                    } else if (Utility.isAngleBetween(right, retval[0],
                                                      retval[1])) {
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

    /**
     * Registers robot death.
     */
    public void onRobotDeath(RobotDeathEvent e) {
        String name = e.getName();
        if (alive.contains(name)) {
            alive.remove(name);
            dead.add(name);
        }
        if (history.containsKey(name)) {
            history.remove(name);
        }
        remaining--;
    }

    /**
     * Registers hitting a target.
     */
    public void hit(Bullet bullet) {
        String target = bullets.get(bullet);
        double damage = Rules.getBulletDamage(bullet.getPower());
        if (target != null) {
            hit.put(target, hit.get(target) + damage);
        }
    }

    /**
     * Registers missing a target.
     */
    public void miss(Bullet bullet) {
        String target = bullets.get(bullet);
        double damage = Rules.getBulletDamage(bullet.getPower());
        if (target != null) {
            missed.put(target, missed.get(target) + damage);
        }
    }

    /**
     * Registers being hit by target.
     */
    public void hitBy(Bullet bullet, String target) {
        double damage = Rules.getBulletDamage(bullet.getPower());
        threat.put(target, threat.get(target) + damage);
        totalThreat = totalThreat + damage;
    }

    /**
     * Adds a bullet to the storage.
     */
    public void addBullet(Bullet bullet, String target) {
        if (bullet != null) {
            bullets.put(bullet, target);
        }
    }

    /**
     * Returns a threat rating of a target.
     */
    public double threat(String target) {
        double value = Math.log(threat.get(target)/(2*totalThreat)+1);
        if (Double.isNaN(value)) {
            value = 0;
        }
        double num = hit.get(target);
        double rate = num / num + missed.get(target);
        if (Double.isNaN(rate)) {
            rate = 0;
        }
        return value * (1 - rate * 0.75) + 0.5;
    }

    /**
     * Returns the weighted hit rate of the robot on a target.
     */
    public double hitRate(String target) {
        double num = hit.get(target);
        double rate = num / num + missed.get(target);
        if (Double.isNaN(rate)) {
            return 0;
        }
        return rate;
    }

    public String toString() {
        String retval = "State:";
        for (List<Snapshot> list : history.values()) {
            if (list.size() > 0) {
                retval += "\r\n  ->"+list.get(0);
            }
        }
        return retval;
    }
}
