package u1226467;

import robocode.*;
import java.util.*;
import java.awt.geom.*;

import u1226467.Snapshot;

public class ScanningRobot extends Robot
{
	// History of statuses
	private Map<String, Snapshot> radar;

	public void run() {
		radar = new HashMap<String, Snapshot>();

		while(true) {
			turnRadarRight(INFINITY);
		}
	}

	public void onStatus(RobotStatus status) {
		statuses.add(status);
	}

	public void onScannedRobot(ScannedRobotEvent event) {
		Snapshot robot = new Snapshot(event, this);
		radar.put(robot.name, robot);
	}
}
