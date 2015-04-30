package u1226467;

import robocode.*;
import java.util.*;
import java.awt.geom.*;

public class MyRobot2 extends Robot
{
	private static int BINS = 20;
	private static double INFINITY = Double.POSITIVE_INFINITY;
	private static double PI = Math.PI;
	private static double PADDING = 0.1;
	private static double MIN_PADDING = 100;
	private static double DIRECTION_THRESHOLD = 0.1;

	// arena size
	private Rectangle2D.Double arena;

	// robot stats
	private List<RobotStatus> statuses;
	private List<Map<String, Snapshot>> scans;
	private Map<String, List<long>> times;


	// surfing
	private List waves;

	public void run() {
		// TODO: get arena size
		arena = new Rectangle2D.Double(0, 0, 800, 600);

		// robot stats
		rLocation = new ArrayList();
		eLocation = new ArrayList();
		rDirection = new ArrayList();
		eHeading = new ArrayList();
		eEnergy = new ArrayList();

		// surfing
		waves = new ArrayList();

		// Gun/radar turn locking
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

		while(true) {
			turnRadarRight(INFINITY);
		}
	}

	public void onStatus(StatusEvent e) {
		statuses.add(e.getStatus());
	}





	class History<T> {
		private T[] store;
		private long pos;
		private long length;
		private long maxlength;
		private long offset;

		public History() {
			this(1000);
		}

		public History(long maxElements) {
			pos = 0;
			length = Math.min(maxElements, 10);
			maxlength = maxElements;
			offset = 0;
			store = new T[length];
		}

		public add(T element) {
			this.add(getTime(), element);
		}

		public add(long time, T element) {
			// check if time is within scope
			if (time < offset-length+pos) {
				// throw new Exception("Time outside scope");
			} else {

			}
			// Expand or cycle through array when we reach the end
			if (pos => length) {
				if (length == maxlength) {
					pos = 0;
					offset += length;
				} else {
					long templength = Math.min(maxlength, length*2);
					T[] tempstore = new T[templength];
					System.arraycopy(store, 0, tempstore, 0, length);
					store = tempstore;
					length = templength;
				}
			}

		}

		private long mintime() {
			return offset-length+pos;
		}

		private long maxtime() {
			return offset+length-1;
		}

		public T last() {
			return store.get(length-1);
		}

		public List<T> last(int number) {
			return store.subList(length-1-number, length-1);
		}

		public get(int index) {
			return store.get(index);
		}

		public get(int start, int end) {
			return store.subList(start, end);
		}
	}

}
