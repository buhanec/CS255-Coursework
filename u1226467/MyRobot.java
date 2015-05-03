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

	// state
	private State state;
	private long previousTime;
	private double previousEnergy;
	private double energyDecay;
	// radar
	Radar radar;
	// movement
	SurfPilot surfer;

	public void run() {
		// state
		previousTime = -1;
		previousEnergy = getEnergy();
		state = new State(getOthers() + 1);
		System.out.println("[State] Initialised for " + (getOthers() + 1));
		Snapshot snap = new Snapshot(this);
		state.addSnapshot(snap);
		System.out.println("[State] Added "+snap.name+" snapshot.");
		// radar
		radar = new Radar(this, state);
		// movement
		surfer = new SurfPilot(state, null, getBattleFieldWidth(), getBattleFieldHeight());

		while(true) {
			if (getTime() >= previousTime + 1) {
				System.out.println("=========================");
				System.out.println("Current time: "+getTime());
				// Check for skipped time
				if (getTime() > previousTime + 1) {
					System.out.println("Skipped time "+previousTime+" to "+getTime());
				}
				previousTime = getTime();
				// Check for energy decay
				if (previousEnergy - getEnergy() == 10) {
					energyDecay = 0.1;
				} else if (previousEnergy - getEnergy() == 0) {
					energyDecay = 0;
				} else {
					System.out.println("[Energy] delta: " + (previousEnergy - getEnergy()));
				}
				previousEnergy = getEnergy();
				// Targetting TODO
				System.out.println("-------------------------");
				System.out.println(state);
				// Calculations
				radar.setStrategy();
				surfer.update(getTime());
				surfer.setTarget(radar.getTarget());
				System.out.println("-------------------------");
				state.update(getTime()+1);
				// Operations
				radar.turn();
				surfer.move();
				//gun.fire();
			} else {
				doNothing();
			}
		}
	}

	public void onStatus(StatusEvent e)  {
		if (state != null) {
			Snapshot snap = new Snapshot(getName(), e.getStatus());
			state.addSnapshot(snap);
			System.out.println("[State] Added "+snap.name+" snapshot.");
		}
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		Snapshot snap = new Snapshot(e, this);
		state.addSnapshot(snap);
		//System.out.println("[State] Added "+snap.name+" snapshot.");
		System.out.println("[Robot] !!! Passing to radar");
		radar.onScannedRobot(e);
		System.out.println("[Robot] !!! Passing to pilot");
		surfer.onScannedRobot(e);
	}

	public void onHitByBullet(HitByBulletEvent e) {
		surfer.onHitByBullet(e);
	}
}
