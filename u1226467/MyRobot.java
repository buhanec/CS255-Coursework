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
	private boolean sit;

	public void run() {
		// state
		previousTime = -1;
		previousEnergy = getEnergy();
		sit = false;
		state = new State(getOthers() + 1);
		System.out.println("[State] Initialised for " + (getOthers() + 1));
		Snapshot snap = new Snapshot(this);
		state.addSnapshot(snap);
		System.out.println("[State] Added "+snap.name+" snapshot.");
		// radar
		radar = new Radar(this, state);
		// movement
		sit = true;

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
				// Targetting TODO
				System.out.println("-------------------------");
				System.out.println(state);
				// Radar
				radar.setStrategy();
				System.out.println("-------------------------");
				state.update(getTime()+1);
				radar.turn();
			} else {
				doNothing();
			}
		}
	}

	//TODO
	/*
	private void move() {
		if (!sit) {
			if (targetting.getTarget() != null) {
				pilot = targettingPilot;
			} else {
				pilot = runningPilot;
			}
		} else {
			pilot = sittingPilot;
		}
		pilot.move();
	}
	//*/

	public void onStatus(StatusEvent e)  {
		if (state != null) {
			Snapshot snap = new Snapshot(getName(), e.getStatus());
			state.addSnapshot(snap);
			System.out.println("[State] Added "+snap.name+" snapshot.");
		}
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		radar.onScannedRobot(e);
		Snapshot snap = new Snapshot(e, this);
		VectorPoint self = new VectorPoint(state.getSelf());
		self.setSpeed(10);
		VectorPoint other = new VectorPoint(snap);
		other.setSpeed(5);
		state.addSnapshot(snap);
		System.out.println("[State] Added "+snap.name+" snapshot.");
	}
}
