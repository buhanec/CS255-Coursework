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

	// History of statuses
	private List<RobotStatus> statuses;

	public void run() {
		statuses = new ArrayList<RobotStatus>();
		Snapshot test = new Snapshot(this);

		addCustomEvent(new RadarTurnCompleteCondition(this));
		setAdjustRadarForGunTurn(true);

		while(true) {
			System.out.println("Scanning.");
			turnRadarRight(INFINITY);
		}
	}

	public void onStatus(RobotStatus status) {
		statuses.add(status);
	}

	public void onScannedRobot(ScannedRobotEvent event) {
		System.out.println("Scanned " + event.getName() + " at " + getTime() + ".");
	}

	public void onCustomEvent(CustomEvent e) {
		if (e.getCondition() instanceof RadarTurnCompleteCondition) {
			sweep();
		}
	}

	private void sweep() {
		double maxBearingAbs=0, maxBearing=0;
		int scannedBots=0;
		Iterator iterator = theEnemyMap.values().
		iterator();

		while(iterator.hasNext()) {
			Enemy tmp = (Enemy)iterator.next();

			if (tmp!=null && tmp.isUpdated()) {
				double bearing = normalRelativeAngle (getHeading() + tmp.getBearing() - getRadarHeading());
				if (Math.abs(bearing)>maxBearingAbs) {
					maxBearingAbs=Math.abs(bearing);
					maxBearing=bearing;
				}
				scannedBots++;
			}
		}

		double radarTurn=180*radarDirection;
		if (scannedBots==getOthers())
		radarTurn=maxBearing+sign(maxBearing)*22.5;

		setTurnRadarRight(radarTurn);
		radarDirection=sign(radarTurn);
	}
}
