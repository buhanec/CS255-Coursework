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

    public boolean isAlive(String name) {
        return alive.contains(name);
    }

    public boolean isDead(String name) {
        return dead.contains(name);
    }

    public Set<String> getAlive() {
        return Collections.unmodifiableSet(alive);
    }

    public Set<String> getDead() {
        return Collections.unmodifiableSet(dead);
    }

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

    public int getRemaining() {
        return remaining-1;
    }

    public int getTotal() {
        return total;
    }

    public int getScanned() {
        return getScanned(3);
    }

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

    public Set<String> getScannedNames() {
        return getScannedNames(3);
    }

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

    public Set<Snapshot> getLatestSnapshots() {
        return getLatestSnapshots(18);
    }

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

    public Snapshot getSelf() {
        return new Snapshot(getSnapshot(self));
    }

    public double[] getArc() {
        return getArc(0, 18);
    }

    public double[] getArc(double heading, long age) {
        DirectedPoint point = new DirectedPoint(getSelf());
        point.setHeading(heading);
        return getArc(point, age);
    }

    public double[] getArc(DirectedPoint radar, long age) {
        long time = 2;
        VectorPoint origin = getSelf();
        Snapshot enemy;
        VectorPoint point;
        double temp;
        double left;
        double right;
        double[] retval = {Double.NaN, Double.NaN};

        for (List<Snapshot> list : history.values()) {
            if (list.size() > 0) {
                enemy = list.get(0);
                if (enemy.getTime() >= time-age && !enemy.getName().equals(self)) {
                    left = radar.getBearingTo(enemy.projectLateral(origin, -1, time));
                    right = radar.getBearingTo(enemy.projectLateral(origin, 1, time));
                } else {
                    continue;
                }
                if (Utility.angleBetween(left, right) > Utility.angleBetween(right, left)) {
                    temp = left;
                    left = right;
                    right = temp;
                }
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

    public void hit(Bullet bullet) {
        String target = bullets.get(bullet);
        double damage = Rules.getBulletDamage(bullet.getPower());
        if (target != null) {
            hit.put(target, hit.get(target) + damage);
        }
    }

    public void miss(Bullet bullet) {
        String target = bullets.get(bullet);
        double damage = Rules.getBulletDamage(bullet.getPower());
        if (target != null) {
            missed.put(target, missed.get(target) + damage);
        }
    }

    public void hitBy(Bullet bullet, String target) {
        double damage = Rules.getBulletDamage(bullet.getPower());
        threat.put(target, threat.get(target) + damage);
        totalThreat = totalThreat + damage;
    }

    public void addBullet(Bullet bullet, String target) {
        if (bullet != null) {
            bullets.put(bullet, target);
        }
    }

    // range from 0.5 - 4.5, usually under 2 for larger battles
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
