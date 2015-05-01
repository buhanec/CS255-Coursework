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
	private static int    RADAR_DIR   = 1;
	private static int    RADAR_360   = 1;
	private static int    RADAR_180   = 2;
	private static int    RADAR_45    = 3;
	private static int    RADAR_0     = 4;

	// state
	private State state;
	private long previousTime;
	private double previousEnergy;
	private double energyDecay;
	// movement
	private boolean sit;
	// radar
	private boolean radar360;
	private int radarLast360;
	private boolean radar180;
	private boolean radar45;
	private double radarAngle;
	private long radarStart;
	private int radarPrevious;
	private int radarWorking;
	private int radarDirection;

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
		// movement
		sit = true;
		// radar
		radar360 = false;
		radarLast360 = 0;
		radar180 = false;
		radar45 = false;
		radarAngle = 0;
		radarStart = 0;
		radarWorking = RADAR_0;
		radarPrevious = RADAR_0;
		radarDirection = 1;

		setAdjustRadarForGunTurn(true);

		while(true) {
			if (getTime() >= previousTime + 1) {
				System.out.println("=========================");
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
				// Determine strategy
				//if (targetting.getTarget() != null) {
					// targetting
				//} else {
					// determine if we want a target
				//}
				if (state.getRemaining() == 1) {
					if (state.getScanned() == 1) {
						System.out.println("duel");
						radar360 = false;
						radar180 = false;
						radarWorking = RADAR_0;
						radarAngle = 0;
						radar45 = true;
					}
				} else {
					System.out.println(radarWorking + " " + radarPrevious + " " + state.getScanned());
					System.out.println(state);
					if (state.getScanned() >= 2) {
						if (radarWorking == RADAR_0 || (radarPrevious == RADAR_360 && radarWorking == RADAR_360)) {
							System.out.println("main 180");
							radar360 = false;
							radarWorking = RADAR_0;
							radarAngle = 0;
							radar180 = true;
						}
					} else {
						if (radarWorking == RADAR_0) {
							System.out.println("main 360");
							radar360 = true;
						}
					}
					if (radarLast360 < getTime() - 40 && state.getScanned() < state.getRemaining()) {
						System.out.println("timed 360");
						radar360 = true;
					}
				}
				if (!radar360 && !radar45 && !radar180 && radarWorking == RADAR_0) {
					System.out.println("backup");
					radar360 = true;
				}
				System.out.println("-------------------------");
				// Radar
				System.out.println("Time when commencing radar movement: " + getTime());
				radar();
			} else {
				doNothing();
			}
		}
	}

	private void radar() {
		if (radar360) {
			System.out.println("[Radar] 360 queued");
			if (radarWorking == RADAR_0) {
				System.out.println("[Radar] 360");
				radar360 = false;
				radarAngle = 360;
				radarStart = getTime();
				radarWorking = RADAR_360;
			}
		}
		if (radar180) {
			System.out.println("[Radar] 180 queued");
			if (radarWorking == RADAR_0) {
				System.out.println("[Radar] 180");
				radar180 = false;
				if (state.getScanned() > 1) {
					double[] test = state.radar180();
					System.out.println(Math.toDegrees(test[0])+"-"+Math.toDegrees(test[1]));
				} else {
					System.out.println("No robots found for a 180.");
				}
				radarStart = getTime();
				radarWorking = RADAR_180;
			}
		}
		if (radar45) {
			System.out.println("[Radar] 45 queued");
			if (radarWorking == RADAR_0) {
				System.out.println("[Radar] 45");
				radar45 = false;
				radarStart = getTime();
				radarWorking = RADAR_45;
			}
		}
		// We have now determined how we are moving, advance state
		System.out.println("Time when commencing state update: " + getTime());
		state.update();
		// Start movement
		if (radarWorking != RADAR_0) {
			// TODO: precision safety net
			if (radarAngle <= Rules.RADAR_TURN_RATE) {
				if (radarDirection == 1) {
					turnRadarRight(radarAngle);
				} else {
					turnRadarLeft(radarAngle);
				}
				radarPrevious = radarWorking;
				radarWorking = RADAR_0;
			} else {
				if (radarDirection == 1) {
					turnRadarRight(Rules.RADAR_TURN_RATE);
				} else {
					turnRadarLeft(Rules.RADAR_TURN_RATE);
				}
				radarAngle -= Rules.RADAR_TURN_RATE;
			}
		} else {
			System.out.println("Radar inactive!");
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
		Snapshot snap = new Snapshot(e, this);
		state.addSnapshot(snap);
		System.out.println("[State] Added "+snap.name+" snapshot.");
	}
}
