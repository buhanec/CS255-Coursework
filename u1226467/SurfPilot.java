package u1226467;

import robocode.*;
import java.util.*;
import robocode.util.Utils;

public class SurfPilot extends AdvancedRobot {
    protected static int BINS = 47;
    protected static double WALL_STICK = 160;
    protected static double WALL_STICK_PERCENT = 0.25;
    protected static double SMOOTHING_STEP = 0.05;
    protected static double WAVE_LOCATION_THRESHOLD = 50;
    protected static double WAVE_SPEED_THRESHOLD = 1;
    protected static double stats[] = new double[BINS];

    protected Random random;

    protected State state;
    protected String target;
    protected Rectangle arena;
    protected double wallSmoothingFactor;

    protected List<Wave> minWaves;
    protected List<Wave> waves;
    protected List<Wave> maxWaves;
    protected List<Double> directions;
    protected List<Double> bearings;

    SurfPilot(State state, String target, Rectangle arena) {
        random = new Random();

        this.state = state;
        this.target = target;
        this.arena = arena;
        wallSmoothingFactor = Math.min(Math.min(arena.getWidth(), arena.getHeight())*WALL_STICK_PERCENT, WALL_STICK);

        minWaves = new ArrayList<Wave>();
        waves = new ArrayList<Wave>();
        maxWaves = new ArrayList<Wave>();
        directions = new ArrayList<Double>();
        bearings = new ArrayList<Double>();
    }

    public void setTarget(String target) {
        this.target = target;
    }

    // courtesy of wiki tutorial
    public void onScannedRobot(ScannedRobotEvent e) {
        System.out.println("[Surfer] ON SCANNED ROBOT");
        // check if we scanned the right robot
        Snapshot current;
        if (e.getName().equals(target)) {
            current = state.getSnapshot(target);
        } else {
            return;
        }
        // fetch current state & previous enemy state
        Snapshot self = state.getSelf();
        Snapshot previous = state.getPreviousSnapshot(target);
        // calculate our lateral velocity and the north bearing of the target
        double selfLateral = Utility.lateral(self.getSpeed(), self.getBearingTo(current.getX(), current.getY()));
        double northBearing = self.getNorthBearingTo(current.getX(), current.getY());
        directions.add(0, selfLateral);
        bearings.add(0, northBearing);
        // calculate bullet information
        if (previous != null) {
            System.out.println("[Surfer] HEY THERE");
            double bulletEnergy = previous.getEnergy()-current.getEnergy();
            if (Utility.containedii(bulletEnergy, Rules.MIN_BULLET_POWER-0.0001, Rules.MAX_BULLET_POWER+0.0001) && directions.size() > 2) {
                System.out.println("[Surfer] Wave detected");
                long bulletTime, bulletMinTime, bulletMaxTime;
                Wave min, avg, max;
                if (current.getTime()-previous.getTime() > 1) {
                    bulletMinTime = previous.getTime();
                    bulletMaxTime = current.getTime()-1;
                } else {
                    bulletMinTime = current.getTime()-1;
                    bulletMaxTime = bulletMinTime;
                }
                double bulletSpeed = Rules.getBulletSpeed(bulletEnergy);
                double bulletDistance = bulletSpeed;
                double bulletDirection = directions.get(2);
                double bulletHeading = bearings.get(2);
                // Construct waves
                min = new Wave(previous,
                               bulletSpeed,
                               bulletDirection,
                               bulletHeading,
                               bulletMinTime);
                if (bulletMinTime == bulletMaxTime) {
                    avg = min;
                    max = min;
                } else {
                    max = new Wave(current,
                                   bulletSpeed,
                                   bulletDirection,
                                   bulletHeading,
                                   bulletMaxTime);
                    avg = new Wave(new Point((previous.getX()+current.getX())/2,
                                             (previous.getY()+current.getY())/2),
                                   bulletSpeed,
                                   bulletDirection,
                                   bulletHeading,
                                   (bulletMinTime+bulletMaxTime)/2);
                }
                minWaves.add(min);
                waves.add(avg);
                maxWaves.add(max);
                //TODO
                //update(e.getTime());
                //surf();
                //gun();
            }
        }
    }

    public int responsibleWave(HitByBulletEvent e, List<Wave> waves) {
        if (!waves.isEmpty()) {
            Point location = new Point(e.getBullet().getX(), e.getBullet().getY());
            for (Wave wave : waves) {
                if ((Math.abs(wave.getRadius() - location.distanceTo(wave)) < WAVE_LOCATION_THRESHOLD) &&
                    (Math.abs(Rules.getBulletSpeed(e.getBullet().getPower()) - wave.getSpeed()) < WAVE_SPEED_THRESHOLD)) {
                    return waves.indexOf(wave);
                }
            }
        }
        return -1;
    }

    public void log(Wave wave, Point collision) {
        int index = getFactorIndex(wave, collision);
        for (int i = 0; i < BINS; i++) {
            stats[i] += 1.0/(Math.pow(index - i, 2) + 1);
        }
    }

    public void onHitByBullet(HitByBulletEvent e) {
        int id = responsibleWave(e, waves);
        Wave wave = null;
        Point collision = new Point(e.getBullet().getX(), e.getBullet().getY());
        if (id == -1) {
            id = responsibleWave(e, maxWaves);
        } else {
            wave = waves.get(id);
            log(wave, collision);
            return;
        }
        if (id == -1) {
            id = responsibleWave(e, minWaves);
        } else {
            wave = maxWaves.get(id);
            log(wave, collision);
            return;
        }
        if (id != -1) {
            wave = minWaves.get(id);
            log(wave, collision);
        }
    }

    public void update(long time) {
        for (Iterator<Wave> i = waves.iterator(); i.hasNext();) {
            Wave wave = i.next();
            wave.update(time);
            if (wave.getRadius() > state.getSelf().distanceTo(wave)) {
                i.remove();
            }
        }
    }

    public Wave getWave() {
        double closest = Double.POSITIVE_INFINITY;
        Wave retval = null;

        for(Wave wave : waves) {
            double distance = state.getSelf().distanceTo(wave) - wave.getRadius();
            if (distance < closest && distance > wave.getSpeed()) {
                retval = wave;
                closest = distance;
            }
        }

        return retval;
    }

    // Index calculation, courtesy of wiki tutorial
    protected static int getFactorIndex(Wave wave, Point target) {
        double offset = Utility.fixAngle(wave.getNorthBearingTo(target) - wave.getHeading());
        double factor = Utils.normalRelativeAngle(offset) / bulletEscapeAngle(wave.getSpeed());
        if (wave.getDirection() < 0) {
            factor = -factor;
        }
        factor = (factor*((BINS-1)/2)) + ((BINS-1)/2);
        return (int) Utility.constrain(factor, 0, BINS-1);
    }

    protected static double bulletEscapeAngle(double speed) {
        return Math.asin(Rules.MAX_VELOCITY/speed);
    }

    // Based on Apollon by rozue (http://robowiki.net?Apollon)
    public Point predict(Wave wave, int direction) {
        VectorPoint self = new VectorPoint(state.getSelf());
        long time = 500; // how many ticks to simulate
        boolean intercepted = false;
        double turn;

        do {
            // Heading adjustment angle
            double angle = wave.getNorthBearingTo(self) + (direction*Math.PI/2);
            angle = wallSmoothing(self, angle, direction) - self.getHeading();

            if (Math.cos(angle) < 0) {
                angle = angle + Math.PI;
                direction = -1;
            } else {
                direction = 1;
            }

            // Adjust heading
            turn = Utility.maxTurn(self.getSpeed());
            angle = Utility.constrain(angle, -turn, turn);
            self.setHeading(Utility.fixAngle(self.getHeading() + angle));

            // If speed and movedir have different signs you want to break to
            // change direction otherwise accelerate
            if (self.getSpeed()*direction > 0) {
                self.setSpeed(self.getSpeed() + direction*Rules.ACCELERATION);
            } else {
                self.setSpeed(self.getSpeed() + direction*Rules.DECELERATION);
            }

            // Adjust position
            if (self.distanceTo(wave) < wave.getRadius() + (time+1) * wave.getSpeed()) {
                intercepted = true;
            }

            // Just in case there are no waves
            time--;
        } while (!intercepted && time > 0);

        return self;
    }

    public double danger(Wave wave, int direction) {
        int index = getFactorIndex(wave, predict(wave, direction));
        return stats[index];
    }

    public MovementInformation move() {
        System.out.println("[Surfer] surfing");
        Wave wave = getWave();
        DirectedPoint self = state.getSelf();
        double angle = wallSmoothing(self, self.getHeading(), (random.nextBoolean() ? -1 : 1));
        if (wave != null) {
            angle = wave.getNorthBearingTo(self);
            if (danger(wave, -1) < danger(wave, 1)) {
                angle = Utility.fixAngle(angle - Math.PI/2);
                angle = wallSmoothing(self, angle, -1);
            } else {
                angle = Utility.fixAngle(angle + Math.PI/2);
                angle = wallSmoothing(self, angle, 1);
            }
        }

        return adjust(angle);
    }

    public MovementInformation adjust(double angle) {
        angle = Utility.fixAngle(angle - state.getSelf().getHeading());
        MovementInformation retval;
        if (Utility.isAngleBetween(angle, 0, Math.PI/2)) {
            retval = new MovementInformation(angle, 1, 100, 1);
        } else if (Utility.isAngleBetween(angle, 0, Math.PI)) {
            retval = new MovementInformation(Math.PI-angle, -1, 100, -1);
        } else if (Utility.isAngleBetween(angle, 0, 3*Math.PI/2)) {
            retval = new MovementInformation(angle-Math.PI, 1, 100, -1);
        } else {
            retval = new MovementInformation(2*Math.PI-angle, -1, 100, 1);
        }
        return retval;
    }

    // Simple iterative wall smoothing
    protected double wallSmoothing(Point self, double angle, int direction) {
        while (!arena.contains(Utility.vectorAdd(self, angle, wallSmoothingFactor))) {
            angle += direction*SMOOTHING_STEP;
        }
        return angle;
    }
}
