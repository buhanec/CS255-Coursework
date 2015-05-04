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

	// state
	private static State state;
	private Rectangle arena;
	private long previousTime;
	private double previousEnergy;
	private double energyDecay;
	private int mode;
	// radar
	private Radar radar;
	private boolean lastTarget;
	private boolean shouldFire;
	private long fireTurns;
	// movement
	private static SurfPilot surfer;
	private static WhiteHole hole;


	public void run() {
		// avoid turning gun with robot
		setAdjustGunForRobotTurn(true);

		// state
		previousTime = -1;
		previousEnergy = getEnergy();
		state = new State(getName(), getOthers() + 1);
		System.out.println("[State] Initialised for " + (getOthers() + 1));
		Snapshot snap = new Snapshot(this);
		state.addSnapshot(snap);
		System.out.println("[State] Added "+snap.name+" snapshot.");
		arena = new Rectangle(getWidth()/2+1, getHeight()/2+1, getBattleFieldWidth()-getWidth()/2-1, getBattleFieldHeight()-getHeight()/2-1);
		mode = 0;
		// radar
		radar = new Radar(this, state);
		lastTarget = false;
		shouldFire = false;
		fireTurns = 0;
		String target = null;
		// movement
		surfer = new SurfPilot(this, state, arena);
		hole = new WhiteHole(this, state, arena);

		while(true) {
			if (getTime() >= previousTime + 1) {
				System.out.println("======== NEW TICK =======");
				System.out.println("Current time: "+getTime());
				previousTime = getTime();
				previousEnergy = getEnergy();
				System.out.println("------- Operations ------");
				// Radar decisions - scan or target
				fireTurns++;
				if (state.getRemaining() == 1 && !lastTarget && state.getAlive().size() > 1) {
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
				} else if ((state.getRemaining() > 1 && state.getScanned() >= state.getRemaining()*FIRE_FACTOR) ||
				    (radar.hasScanned() && fireTurns > FIRE_TIME)) {
					// find best target here
					target = null;
					fireTurns = 0;
				} else if (!lastTarget) {
					target = null;
				}
				// Movement updates
				surfer.update(getTime());
				hole.update(getTime());
				System.out.println("--------- Radar ---------");
				// Operations - should block here
				if (target != null && getGunHeat() == 0) {
					state.update(getTime()+1);
					radar.gunSimple(target);
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
		/*
		if (getEnergy() > 10 && !radar.isFiring()) {
			System.out.println("[Scanning] potshot");
			if (getGunHeat() == 0) {
				double bulletPower = Utility.constrain(3*500/state.getSelf().distanceTo(snap), Rules.MIN_BULLET_POWER, Rules.MAX_BULLET_POWER);
				fireBullet(bulletPower);
			}
		}
		*/
	}

	public void onHitByBullet(HitByBulletEvent e) {
		surfer.onHitByBullet(e);
	}

	// Should not happen under normal circumstances
	public void onHitWall(HitWallEvent e) {
		System.out.println("[onHitWall]");
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

	//TODO
	public void onBulletHit(BulletHitEvent e) {

	}

	//TODO
	public void onBulletHitBullet(BulletHitBulletEvent e) {

	}

	//TODO
	public void onBulletMissed(BulletMissedEvent e) {

	}

	//TODO
	public void onDeath(DeathEvent e) {

	}

	//TODO
	public void onBattleEnded(BattleEndedEvent e) {

	}

	//TODO
	public void onRobotDeath(RobotDeathEvent e) {
		state.onRobotDeath(e);
	}

	//TODO
	public void onRoundEnded(RoundEndedEvent e) {
		state.reset();
		radar.reset();
		lastTarget = false;
		shouldFire = false;
		fireTurns = 0;
	}

	//TODO
	public void onWin(WinEvent e) {

	}
}
