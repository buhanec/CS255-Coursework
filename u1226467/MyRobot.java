package u1226467;

import robocode.*;
import java.util.*;
import java.awt.geom.*;

public class MyRobot extends Robot
{
	private static double RAM_LIMIT   = 0.9;
	private static double FIRE_FACTOR = 0.5;
	private static long   FIRE_TIME   = 18;

	// static components
	private static boolean initialised = false;
	private static State state;
	private static Rectangle arena;
	private static SurfPilot surfer;
	private static WhiteHole hole;

	// radar
	private Radar radar;
	private String target;

	// main loop
	public void run() {
		// initialise static variables
		if (!initialised) {
			double diagonal = Math.ceil(Math.sqrt(Math.pow(getWidth(), 2)
			                            + Math.pow(getHeight(), 2)));
			state = new State(getName(), getOthers() + 1);
			arena = new Rectangle(diagonal, diagonal,
			                      getBattleFieldWidth()-2*diagonal,
			                      getBattleFieldHeight()-2*diagonal);
			surfer = new SurfPilot(this, state, arena);
			hole = new WhiteHole(this, state, arena);
			initialised = true;
		} else {
			state.reset();
		}

		// avoid turning gun when the robot turns
		setAdjustGunForRobotTurn(true);

		// energy and time state, currently not used but would be used if
		// the robot started losing ticks due to computationally demanding
		// tasks or long blocking functions.
		long previousTime = -1;
		double previousEnergy = getEnergy();

		// add self to state
		Snapshot snap = new Snapshot(this);
		state.addSnapshot(snap);

		// radar
		radar = new Radar(this, state, arena);
		boolean lastTarget = false; // is the last target set
		boolean shouldFire = false;
		long fireTurns = 0; // turns since not opting to firing
		target = null;

		while(true) {
			if (getTime() >= previousTime + 1) {
				previousTime = getTime();
				previousEnergy = getEnergy();

				fireTurns++;

				// if there is only one target and we have not set it as the
				// target yet, do so now
				if (state.getRemaining() == 1 && !lastTarget) {
					for (String t : state.getAlive()) {
						if (!t.equals(getName())) {
							target = t;
							radar.setTarget(target);
							surfer.setTarget(target);
							lastTarget = true;
						}
					}
				}

				// if we are not currently firing and there is more than one
				// target remaining
				if (!radar.isFiring() && state.getRemaining() > 1) {
					// the three conditions under which we choose a new target
					// are:
					//  - we have enough targets scanned
					//  - we have previously completed a full scan and the
					//    robot has not fired in a while
					//  - the radar is telling the robot to shoot already
					if ((state.getScanned()>=state.getRemaining()*FIRE_FACTOR)
					    || (radar.hasScanned() && fireTurns > FIRE_TIME)
					    || radar.shootAlready()) {
						// choose the target that is easiest to hit
						double rate = -1;
						for (String t : state.getScannedNames()) {
							double temp = state.hitRate(t);
							if (temp > rate) {
								target = t;
								rate = temp;
							}
						}
						fireTurns = 0;
					}
				// if the currently selected target is not the last target and
				// we are not currently firing at it, clear it to allow for
				// scanning to occur
				} else if (!lastTarget) {
					target = null;
				}

				// Movement updates
				surfer.update(getTime());
				hole.update(getTime());

				// if there is a target chosen and we can fire, and the target
				// was not reported lost by the radar, we attempt to fire
				if (target != null && getGunHeat() == 0
				    && !radar.lostTarget()) {
					state.update(getTime()+1);
					radar.gunSimple(target);
				// otherwise we perform scanning
				} else {
					radar.setStrategy();
					state.update(getTime()+1);
					radar.scan();
				}
			} else {
				doNothing();
			}
		}
	}

	public void onStatus(StatusEvent e)  {
		if (state != null) {
			Snapshot snap = new Snapshot(getName(), e.getStatus());
			state.addSnapshot(snap);
		}
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		Snapshot snap = new Snapshot(e, this);
		// update state, radar and surfer
		state.addSnapshot(snap);
		radar.onScannedRobot(e);
		surfer.onScannedRobot(e);
		// take potshot if possible, as it does not waste ticks and many
		// robots are stationary
		if (getEnergy() > 10 && !radar.isFiring() && target == null) {
			if (getGunHeat() == 0) {
				double power = 500/state.getSelf().distanceTo(snap);
				if (getEnergy() > 20 && power < 0.5) {
					power = 0.5;
				}
				power = Utility.constrain(power, Rules.MIN_BULLET_POWER,
				                          Rules.MAX_BULLET_POWER);
				state.addBullet(fireBullet(power), e.getName());
			}
		}
	}

	// update state and pilots, perform reactive movement
	public void onHitByBullet(HitByBulletEvent e) {
		surfer.onHitByBullet(e);
		hole.onHitByBullet(e);
		state.hitBy(e.getBullet(), e.getName());
		hole.reactive(false);
	}

	// Should not happen under normal circumstances
	public void onHitWall(HitWallEvent e) {
		hole.reactive(true);
	}

	// if we rammed someone, keep ramming
	public void onHitRobot(HitRobotEvent e) {
		DirectedPoint self = state.getSelf();
		double limx, limy, distance;

		if (Utility.isAngleBetween(self.getHeading(), 3*Math.PI/2, Math.PI)) {
			limy = (arena.getHeight()-self.getY())/Math.cos(self.getHeading());
		} else if (Utility.isAngleBetween(self.getHeading(), Math.PI, 3*Math.PI/2)) {
			limy = (arena.getY()-self.getY())/Math.cos(self.getHeading());
		} else {
			limy = Double.POSITIVE_INFINITY;
		}

		if (Utility.isAngleBetween(self.getHeading(), 0, Math.PI/2)) {
			limx = (arena.getWidth()-self.getX())/Math.sin(self.getHeading());
		} else if (Utility.isAngleBetween(self.getHeading(), Math.PI/2, 0)) {
			limx = (arena.getX()-self.getX())/Math.sin(self.getHeading());
		} else {
			limx = Double.POSITIVE_INFINITY;
		}

		distance = Math.min(limx, limy)*RAM_LIMIT;

		if (getVelocity() > 0) {
			ahead(distance);
		} else {
			back(distance);
		}
	}

	// update state
	public void onBulletHit(BulletHitEvent e) {
		state.hit(e.getBullet());
	}

	// update state
	public void onBulletHitBullet(BulletHitBulletEvent e) {
		state.miss(e.getBullet());
	}

	// update state
	public void onBulletMissed(BulletMissedEvent e) {
		state.miss(e.getBullet());
	}

	// update state
	public void onRobotDeath(RobotDeathEvent e) {
		state.onRobotDeath(e);
	}
}
