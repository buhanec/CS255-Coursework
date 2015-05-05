package u1226467;

import robocode.*;
import java.util.*;
import java.awt.geom.*;

public class MyRobot extends Robot
{
	private static int    BINS        = 47;
	private static double INFINITY    = Double.POSITIVE_INFINITY;
	private static double PI          = Math.PI;
	private static double PADDING     = 0.1;
	private static double MIN_PADDING = 100;
	private static double DIRECTION   = 0.1;
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
			System.out.println("[Robot] Initialised");
			double diagonal = Math.ceil(Math.sqrt(Math.pow(getWidth(), 2) + Math.pow(getHeight(), 2)));
			state = new State(getName(), getOthers() + 1);
			arena = new Rectangle(diagonal, diagonal,
			                      getBattleFieldWidth()-2*diagonal,
			                      getBattleFieldHeight()-2*diagonal);
			surfer = new SurfPilot(this, state, arena);
			hole = new WhiteHole(this, state, arena);
			initialised = true;
		}

		// avoid turning gun when the robot turns
		setAdjustGunForRobotTurn(true);

		// state
		long previousTime = -1;
		double previousEnergy = getEnergy();

		// add self to state
		Snapshot snap = new Snapshot(this);
		state.addSnapshot(snap);

		// radar
		radar = new Radar(this, state, arena);
		boolean lastTarget = false;
		boolean shouldFire = false;
		long fireTurns = 0;
		target = null;

		while(true) {
			if (getTime() >= previousTime + 1) {
				//System.out.println("======== NEW TICK =======");
				//System.out.println("Current time: "+getTime());
				previousTime = getTime();
				previousEnergy = getEnergy();
				//System.out.println("------- Targeting -------");
				// Radar decisions - scan or target
				fireTurns++;
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
				if (radar.isFiring()) {
					// check if target is still good
				} else if (state.getRemaining() > 1) {
					if ((state.getScanned() >= state.getRemaining()*FIRE_FACTOR)
					    || (radar.hasScanned() && fireTurns > FIRE_TIME)) {
						target = null;
						fireTurns = 0;
					}
				} else if (!lastTarget) {
					target = null;
				}
				// Movement updates
				surfer.update(getTime());
				hole.update(getTime());
				//System.out.println("--------- Radar ---------");
				// Operations - should block here
				//TODO only select target if energy is good enough
				System.out.println(target);
				System.out.println(getGunHeat());
				System.out.println(radar.lostTarget());
				if (target != null && getGunHeat() == 0 && !radar.lostTarget()) {
					System.out.println("Targeting");
					state.update(getTime()+1);
					radar.gunSimple(target);
				} else {
					System.out.println("Scanning");
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
		//System.out.println("[onstatus]");
		if (state != null) {
			Snapshot snap = new Snapshot(getName(), e.getStatus());
			state.addSnapshot(snap);
		}
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		Snapshot snap = new Snapshot(e, this);
		//System.out.println(snap);
		state.addSnapshot(snap);
		radar.onScannedRobot(e);
		surfer.onScannedRobot(e);
		// take potshot
		if (getEnergy() > 10 && !radar.isFiring() && target == null) {
			//System.out.println("[Potshot]");
			if (getGunHeat() == 0) {
				double power = 500/state.getSelf().distanceTo(snap);
				// make it count, kinda
				if (getEnergy() > 20 && power < 0.5) {
					power = 0.5;
				}
				power = Utility.constrain(power, Rules.MIN_BULLET_POWER, Rules.MAX_BULLET_POWER);
				state.addBullet(fireBullet(power), e.getName());
			}
		}
	}

	public void onHitByBullet(HitByBulletEvent e) {
		surfer.onHitByBullet(e);
		hole.onHitByBullet(e);
		state.hitBy(e.getBullet(), e.getName());
		hole.reactive(false);
	}

	// Should not happen under normal circumstances
	public void onHitWall(HitWallEvent e) {
		System.out.println("[onHitWall]");
		hole.reactive(true);
	}

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

		ahead(distance);
	}

	public void onBulletHit(BulletHitEvent e) {
		state.hit(e.getBullet());
	}

	public void onBulletHitBullet(BulletHitBulletEvent e) {
		state.miss(e.getBullet());
	}

	public void onBulletMissed(BulletMissedEvent e) {
		state.miss(e.getBullet());
	}

	public void onDeath(DeathEvent e) {}

	public void onBattleEnded(BattleEndedEvent e) {}

	public void onRobotDeath(RobotDeathEvent e) {
		state.onRobotDeath(e);
	}

	public void onRoundEnded(RoundEndedEvent e) {
		state.reset();
	}

	public void onWin(WinEvent e) {}
}
