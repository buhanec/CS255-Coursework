package u1226467;

import robocode.*;
import robocode.util.Utils;
import java.util.*;
import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import java.util.zip.*;

/**
 * Guess Factor Wave Surfing Bot
 * based on the guess factoring tutorial online. Thank-you Kawigi
 * http://robowiki.net/w/index.php?title=Wave_Surfing_Tutorial
 * based on the wave surfing tutorial online. Thank-you Voidious
 * http://robowiki.net/w/index.php?title=GuessFactor_Targeting_Tutorial
 * based on the description of floodMini's segmentation.  Thank-you Kawigi
 * http://robowiki.net/w/index.php?title=FloodMini
 * saves stats between battles, based on FloodMini's implementation.  Thank-you Kawigi
 *
 * Note: many of the comments are not mine, but I did try to add them to make things make more sense
 *
 * @author Mageek
 */
public class GFSurfer extends AdvancedRobot{
    public java.util.List<WaveBullet> waves = new ArrayList<WaveBullet>();

    public static final int BINS = 31;
    public static int[][][][][] stats; //segmented on distance, absolute of lateral velocity, will leave bounds, and acceleration

    public static Rectangle2D.Double fieldRect = new java.awt.geom.Rectangle2D.Double(18, 18, 764, 564);

    public int direction = 1;  //the direction the enemy is moving around us, clockwise or counter clockwise
    public int moveTimes = 0;
    public double lastVelocity = 0;

    public static String enemyName = null; //the name of our opponent

    public static double _surfStats[]; // we'll use 47 bins
    public Point2D.Double _myLocation;     // our bot's location
    public Point2D.Double _enemyLocation;  // enemy bot's location

    public ArrayList enemyWaves;          // a list of all waves currently being shot at us by our single enemy
    public ArrayList _surfDirections;      // a list of our motion relative to the enemy (clockwise or counterclockwise)
    public ArrayList _surfAbsBearings;     // a list of the absolute bearing from the enemy to us

    public static double oppEnergy = 100.0;  //the opponent's last energy level

    public static double WALL_STICK = 160;

    @Override
    public void run(){

        enemyWaves = new ArrayList();  //initialize the lists
        _surfDirections = new ArrayList();
        _surfAbsBearings = new ArrayList();

        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        while(true){
            turnRadarRightRadians(Double.POSITIVE_INFINITY);
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e)
    {

        if(enemyName == null){  //no enemy
            enemyName = e.getName();  //we now have an enemy
            try
            {
                ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(new FileInputStream(getDataFile(enemyName))));
                stats = (int[][][][][])in.readObject(); //get guess factor gun stats
                _surfStats = (double[])in.readObject(); //get surf wave stats
                in.close();
                System.out.println("Successfully loaded data for " + enemyName);
            }
            catch (Exception ex)
            {
                System.out.println("Unable to find data");
                stats = new int[10][3][2][3][BINS];
                _surfStats = new double[BINS];
            }
        }

        _myLocation = new Point2D.Double(getX(), getY());  //where we are
        double lateralVelocity = getVelocity()*Math.sin(e.getBearingRadians());  //our motion perpendicual to them
        double absBearing = Utils.normalAbsoluteAngle(getHeadingRadians() + e.getBearingRadians());

        _surfDirections.add(0, new Integer((lateralVelocity >= 0) ? 1 : -1));  //add an integer to surf directions, determining if we are going clockwise or counterclockwise relative to them
        _surfAbsBearings.add(0, new Double(absBearing + Math.PI));  //add the absolute bearing

         double bulletPower = oppEnergy - e.getEnergy();  //their energy drop
        if (bulletPower < 3.01 && bulletPower > 0.09 && _surfDirections.size() > 2) {  //if their drop corresponds to them having fired
            EnemyWave ew = new EnemyWave();  //set up a new wave
            ew.fireTime = getTime() - 1;
            ew.bulletVelocity = bulletVelocity(bulletPower);
            ew.distanceTraveled = bulletVelocity(bulletPower);
            ew.direction = ((Integer)_surfDirections.get(2)).intValue();  //why 2???
            ew.directAngle = ((Double)_surfAbsBearings.get(2)).doubleValue();
            ew.fireLocation = (Point2D.Double)_enemyLocation.clone(); // last tick

            enemyWaves.add(ew);  //insert wave into enemywaves
        }

        oppEnergy = e.getEnergy();  //update energy


        // update after EnemyWave detection, because that needs the previous
        // enemy location as the source of the wave
        _enemyLocation = project(_myLocation, absBearing, e.getDistance());  //current tick

        updateWaves();  //update the waves
        doSurfing();  //do the surfing

        // find our enemy's location:
        double ex = getX() + Math.sin(absBearing) * e.getDistance();
        double ey = getY() + Math.cos(absBearing) * e.getDistance();

        for (int i=0; i < waves.size(); i++){  //go through list
            WaveBullet currentWave = (WaveBullet)waves.get(i);

            if (currentWave.checkHit(ex, ey, getTime())){
                waves.remove(currentWave);
                i--; //decrease indexer
            }
        }

        //RADAR
        double maxRobotTurnAngle = Rules.MAX_VELOCITY / e.getDistance() + 0.1;  //s = r * t, the maximum the robot can move in a tick in degrees relative to us
        double radarTurnAngle = Utils.normalRelativeAngle(absBearing - getRadarHeadingRadians());
        setTurnRadarRightRadians(radarTurnAngle + maxRobotTurnAngle * (radarTurnAngle > 0 ? 1 : -1));

        double power = Math.min(3, Math.max(.1, 2));  //set power to 2 for now
        // don't try to figure out the direction they're moving
        // they're not moving, just use the direction we had before
        if (e.getVelocity() != 0)
        {
            if (Math.sin(e.getHeadingRadians()-absBearing)*e.getVelocity() < 0){
                direction = -1;  //counterclockwise
            }
            else
                direction = 1;  //clockwise
        }

        if(e.getVelocity() == 0) moveTimes=0;  else  moveTimes++;
        double changeInVelocity = e.getVelocity() - lastVelocity;  //negative if slowing down
        lastVelocity = e.getVelocity();

        int DistanceSegmentation = Math.min((int)(e.getDistance() / 100),9);
        int AbsoluteLatVelocitySegmentation = Math.min((int)(Math.abs(e.getVelocity() * Math.sin(e.getHeadingRadians() - (e.getBearingRadians() + getHeadingRadians())))/3),2);
        int WallCollisionSegmentation = fieldRect.contains(project(new Point2D.Double(ex, ey), e.getHeading(), e.getVelocity()*10)) ? 1 : 0;  //if will leave field, 1 else 0
        int AccelerationSegmentation = ((changeInVelocity > 0 ? 0 : 1) == 1) ? (changeInVelocity < 0 ? 2 : 1) : 0;  //0 for acceleration, 1 for gliding, 2 for decelleration

        //int[] currentStats = stats[(int)((e.getVelocity() + 8)/2)][(int)(e.getDistance() / 100)][Math.min(9, moveTimes/5)]; // It doesn't look silly now!
        int[] currentStats = stats[DistanceSegmentation][AbsoluteLatVelocitySegmentation][WallCollisionSegmentation][AccelerationSegmentation];

        WaveBullet newWave = new WaveBullet(getX(), getY(), absBearing, power, direction, getTime(), currentStats);

        int bestindex = 15; // initialize it to be in the middle, guessfactor 0.
        for (int i=0; i<31; i++){
            if (currentStats[bestindex] < currentStats[i])
                bestindex = i;
        }  //get the best index to use

        //this should do the opposite of the math in the WaveBullet:
        double guessfactor = (double)(bestindex - (BINS - 1) / 2) / ((BINS - 1) / 2);
        double angleOffset = direction * guessfactor * newWave.maxEscapeAngle();
        setTurnGunRightRadians(Utils.normalRelativeAngle(absBearing - getGunHeadingRadians() + angleOffset));
        if (setFireBullet(power) != null)
            waves.add(newWave);
    }

    /**
     * updates the position of the waves
     */
    public void updateWaves() {
        for (int x = 0; x < enemyWaves.size(); x++) {
            EnemyWave ew = (EnemyWave)enemyWaves.get(x);  //go through each wave in list

            ew.distanceTraveled = (getTime() - ew.fireTime) * ew.bulletVelocity;  //update the distance moved
            if (ew.distanceTraveled > _myLocation.distance(ew.fireLocation) + 50) { //if it has passed us by 50
                enemyWaves.remove(x);  //remove from list
                x--;  //decrease list count size
            }
        }
    }

    /**
     * returns the closest wave which we can change our movement for to "surf"
     * @return
     */
    public EnemyWave getClosestSurfableWave() {
        double closestDistance = 50000; // arbitrary large number
        EnemyWave surfWave = null;  //the surfable wave we will return

        for (int x = 0; x < enemyWaves.size(); x++) {
            EnemyWave ew = (EnemyWave)enemyWaves.get(x);  //go through wave list
            double distance = _myLocation.distance(ew.fireLocation) - ew.distanceTraveled;  //distance from us to the crest of the wave

            if (distance > ew.bulletVelocity && distance < closestDistance) {  //if this is the closest wave, and the wave will not hit us next tick
                surfWave = ew;  //make this the best surfable wave
                closestDistance = distance;  //make this the closest distance
            }
        }

        return surfWave;
    }

    /**
     * Given the EnemyWave that the bullet was on, and the point where we
     * were hit, calculate the index into our stat array for that factor.
     */
    public static int getFactorIndex(EnemyWave ew, Point2D.Double targetLocation) {
        double offsetAngle = (absoluteBearing(ew.fireLocation, targetLocation) - ew.directAngle);  //angle off of the absolute bearing
        double factor = Utils.normalRelativeAngle(offsetAngle)  / maxEscapeAngle(ew.bulletVelocity) * ew.direction;  //the actual guess factor

        return (int)limit(0, (factor * ((BINS - 1) / 2)) + ((BINS - 1) / 2),  BINS - 1);  //return the index of the guess factor
    }

    /**
     * Given the EnemyWave that the bullet was on, and the
     * point where we were hit, update our stat array to reflect
     * the danger in that area.
     * A larger number means we are less likely to go there
     */
    public void logHit(EnemyWave ew, Point2D.Double targetLocation) {
        int index = getFactorIndex(ew, targetLocation);  //get the index to be incremented

        for (int x = 0; x < BINS; x++) {
            // for the spot bin that we were hit on, add 1;
            // for the bins next to it, add 1 / 2;
            // the next one, add 1 / 5; and so on...
            _surfStats[x] += 1.0 / (Math.pow(index - x, 2) + 1);  // use simple bin smoothing for dodging
        }
    }

    @Override
    public void onBulletHitBullet(BulletHitBulletEvent e) {
        // If the enemyWaves collection is empty, we must have missed the
        // detection of this wave somehow.
        if (!enemyWaves.isEmpty()) {
            Point2D.Double hitBulletLocation = new Point2D.Double(e.getBullet().getX(), e.getBullet().getY());  //where the bullet is which hit the otherone
            EnemyWave hitWave = null;  //the wave which hit the bullet

            // look through the EnemyWaves, and find one that could've collided.
            for (int x = 0; x < enemyWaves.size(); x++) {
                EnemyWave ew = (EnemyWave)enemyWaves.get(x);

                if (Math.abs(ew.distanceTraveled - _myLocation.distance(hitBulletLocation)) < 50
                    /*&& Math.round(bulletVelocity(e.getBullet().getPower()) * 10) == Math.round(ew.bulletVelocity * 10)*/) {
                    //if it is in range
                    hitWave = ew;  //this wave hit us
                    break;
                }
            }

            if (hitWave != null) {
                logHit(hitWave, hitBulletLocation);  //we got hit by this wave, update the factors

                enemyWaves.remove(enemyWaves.lastIndexOf(hitWave)); //remove this wave
            }
        }
    }


    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        // If the enemyWaves collection is empty, we must have missed the
        // detection of this wave somehow.
        if (!enemyWaves.isEmpty()) {
            Point2D.Double hitBulletLocation = new Point2D.Double(e.getBullet().getX(), e.getBullet().getY());  //where the bullet is which hit us
            EnemyWave hitWave = null;  //the wave which hit us

            // look through the EnemyWaves, and find one that could've hit us.
            for (int x = 0; x < enemyWaves.size(); x++) {
                EnemyWave ew = (EnemyWave)enemyWaves.get(x);

                if (Math.abs(ew.distanceTraveled - _myLocation.distance(ew.fireLocation)) < 50
                    && Math.round(bulletVelocity(e.getBullet().getPower()) * 10) == Math.round(ew.bulletVelocity * 10)) {
                    //if it is in range to hit us and their velocities match
                    hitWave = ew;  //this wave hit us
                    break;
                }
            }

            if (hitWave != null) {
                logHit(hitWave, hitBulletLocation);  //we got hit by this wave, update the factors

                enemyWaves.remove(enemyWaves.lastIndexOf(hitWave)); //remove this wave
            }
        }
    }

    // CREDIT: mini sized predictor from Apollon, by rozu
    // http://robowiki.net?Apollon
    public Point2D.Double predictPosition(EnemyWave surfWave, int direction) {
        Point2D.Double predictedPosition = (Point2D.Double)_myLocation.clone();  //where we shall be
        double predictedVelocity = getVelocity();  //what our velocity will be at any given time
        double predictedHeading = getHeadingRadians();  //what our absolute heading will be at any given time
        double maxTurning, moveAngle, moveDir;  //how much we can turn, our move angle max relative to enemy, and the direction we are moving in relative to enemy

        int counter = 0; // number of ticks in the future
        boolean intercepted = false;  //once the enemy's bullet hits us

        do {
            moveAngle =
                wallSmoothing(predictedPosition, absoluteBearing(surfWave.fireLocation,
                predictedPosition) + (direction * (Math.PI/2)), direction)
                - predictedHeading;  //the max moveAngle we can move in the given time
            moveDir = 1;

            if(Math.cos(moveAngle) < 0) {  //if the turn is greater than PI/2
                moveAngle += Math.PI;
                moveDir = -1;  //change our front vs. back
            }

            moveAngle = Utils.normalRelativeAngle(moveAngle);

            // maxTurning is built in like this, you can't turn more then this in one tick
            maxTurning = Math.PI/(720.0)*(40.0 - 3.0 * Math.abs(predictedVelocity));
            predictedHeading = Utils.normalRelativeAngle(predictedHeading + limit(-maxTurning, moveAngle, maxTurning));

            // this one is nice ;). if predictedVelocity and moveDir have
            // different signs you want to breack down  (i.e: we are moving in the wrong direction and want to be surfing the other way)
            // otherwise you want to accelerate (look at the factor "2")  accelerate at 1 pixel/turn, deccelerate at 2
            predictedVelocity += (predictedVelocity * moveDir < 0 ? 2*moveDir : moveDir);  //accelerate or deccelerate
            predictedVelocity = limit(-8, predictedVelocity, 8);  // we can't go faster than +- 8

            // calculate the new predicted position
            predictedPosition = project(predictedPosition, predictedHeading, predictedVelocity); //project our location

            counter++;  //increment ticker

            if (predictedPosition.distance(surfWave.fireLocation) <
                 surfWave.distanceTraveled + (counter * surfWave.bulletVelocity) + surfWave.bulletVelocity) {
                intercepted = true;  //if the wave has broken on us
            }
        } while(!intercepted && counter < 500);  //while the wave has not broken on us and the counter is within a reasonable amount of ticks

        return predictedPosition;  //returns where we should end up
    }

    /**
     * returns the danger associated with a given wave and the direction we would be surfing it on
     * @param surfWave
     * @param direction
     * @return
     */
    public double checkDanger(EnemyWave surfWave, int direction) {
        int index = getFactorIndex(surfWave, predictPosition(surfWave, direction));

        return _surfStats[index];
    }

    /**
     * actually calculate our surfing strategy
     */
    public void doSurfing() {
        EnemyWave surfWave = getClosestSurfableWave();  // get the next wave to surf

        if (surfWave == null) { return; }  //if no surfing waves, our work here is done

        //XXX what if we didn't surf at max speed?
        double dangerLeft = checkDanger(surfWave, -1);  //hpw dangerous would it be to surf left at max speed?
        double dangerRight = checkDanger(surfWave, 1);  //right at max speed?

        double goAngle = absoluteBearing(surfWave.fireLocation, _myLocation);  //the angle from surfwave firelocation to our location
        if (dangerLeft < dangerRight) {  //if it is less dangerous to go left
            goAngle = wallSmoothing(_myLocation, goAngle - (Math.PI/2), -1);  //adjust our movement angle with regards to wall smoothing
        } else {
            goAngle = wallSmoothing(_myLocation, goAngle + (Math.PI/2), 1);
        }

        setBackAsFront(this, goAngle);  //move with respect to our go angle
    }

    // CREDIT: Iterative WallSmoothing by Kawigi
    //   - return absolute angle to move at after account for WallSmoothing
    // robowiki.net?WallSmoothing
    public double wallSmoothing(Point2D.Double botLocation, double angle, int orientation) {
        while (!fieldRect.contains(project(botLocation, angle, 160))) {  //as long as our movement places us outside of our field rectangle
            angle += orientation*0.05;  //we adjust our angle so that we don't move out of the rect field
        }
        return angle;
    }

    // CREDIT: from CassiusClay, by PEZ
    //   - returns point length away from sourceLocation, at angle
    // robowiki.net?CassiusClay
    public static Point2D.Double project(Point2D.Double sourceLocation, double angle, double length) {
        return new Point2D.Double(sourceLocation.x + Math.sin(angle) * length,
            sourceLocation.y + Math.cos(angle) * length);
    }

    // got this from RaikoMicro, by Jamougha, but I think it's used by many authors
    //  - returns the absolute angle (in radians) from source to target points
    public static double absoluteBearing(Point2D.Double source, Point2D.Double target) {
        return Math.atan2(target.x - source.x, target.y - source.y);
    }

    public static double limit(double min, double value, double max) {
        return Math.max(min, Math.min(value, max));
    }

    public static double bulletVelocity(double power) {
        return (20.0 - (3.0*power));
    }

    public static double maxEscapeAngle(double velocity) {
        return Math.asin(8.0/velocity);
    }

    public static void setBackAsFront(AdvancedRobot robot, double goAngle) {
        double angle = Utils.normalRelativeAngle(goAngle - robot.getHeadingRadians());
        if (Math.abs(angle) > (Math.PI/2)) {
            if (angle < 0) {
                robot.setTurnRightRadians(Math.PI + angle);
            } else {
                robot.setTurnLeftRadians(Math.PI - angle);
            }
            robot.setBack(100);
        } else {
            if (angle < 0) {
                robot.setTurnLeftRadians(-1*angle);
           } else {
                robot.setTurnRightRadians(angle);
           }
            robot.setAhead(100);
        }
    }

    @Override
    public void onPaint(Graphics2D g){
        long time = getTime();
        g.setColor(Color.GREEN);
        for(WaveBullet wB : waves){
            double radius = wB.getTravelDistance(time);
            g.drawOval((int)(wB.startX - radius), (int)(wB.startY - radius), (int)(2*radius), (int)(2*radius));
        }


        g.setColor(java.awt.Color.red);

         for(int i = 0; i < enemyWaves.size(); i++){
            EnemyWave w = (EnemyWave)(enemyWaves.get(i));
            Point2D.Double center = w.fireLocation;

            //int radius = (int)(w.distanceTraveled + w.bulletVelocity);
            //hack to make waves line up visually, due to execution sequence in robocode engine
            //use this only if you advance waves in the event handlers (eg. in onScannedRobot())
            //NB! above hack is now only necessary for robocode versions before 1.4.2
            //otherwise use:
            int radius = (int)w.distanceTraveled;

            //Point2D.Double center = w.fireLocation;
            if(radius - 40 < center.distance(_myLocation))
               g.drawOval((int)(center.x - radius ), (int)(center.y - radius), radius*2, radius*2);
         }
    }

    @Override
    public void onBulletHit(BulletHitEvent e){
        oppEnergy = e.getEnergy();  //update latest energy
    }

    @Override
    public void onWin(WinEvent e)
    {  //Thank-you, Kawigi!
        try
        {
            ObjectOutputStream outWriter = new ObjectOutputStream(new GZIPOutputStream(new RobocodeFileOutputStream(getDataFile(enemyName))));  //creates it if not existant yet
            outWriter.writeObject(stats);
                    outWriter.writeObject(_surfStats);
            outWriter.close();
        }
        catch (IOException ex)
        {
            System.out.println("Error writing to file: " + ex);
        }
    }
/**
 * An EnemyWave which represents where the enemy's wave is
 */
public class EnemyWave {
    Point2D.Double fireLocation;
    long fireTime;
    double bulletVelocity, directAngle, distanceTraveled;
    int direction;

    public EnemyWave() { }
}

/**
 * A wave bullet
 * @author tim
 */
public class WaveBullet
    {
    public double startX, startY, startBearing, power;
    public long   fireTime;  //the time of firing
    public int    direction;  //clockwise or counterclockwise
    public int[]  returnSegment;

    public WaveBullet(double x, double y, double bearing, double power,
            int direction, long time, int[] segment)
    {
        startX         = x;
        startY         = y;
        startBearing   = bearing;
        this.power     = power;
        this.direction = direction;
        fireTime       = time;
        returnSegment  = segment;

    }

    public double getTravelDistance(long time){
        return (time - fireTime) * getBulletSpeed();
    }

    /**
     * returns the speed of the bullet
     * @return
     */
    public double getBulletSpeed(){
        return 20.0 - power * 3.0;
    }

    /**
     * the max escape angle of the enemy based on their velocity
     * @return
     */
    public double maxEscapeAngle(){
        return Math.asin(8.0 / getBulletSpeed());
    }

    /**
     * check if wave has broken, then calculate the correct factor
     * and increment it
     * @param enemyX
     * @param enemyY
     * @param currentTime
     * @return
     */
    public boolean checkHit(double enemyX, double enemyY, long currentTime)
    {
        // if the distance from the wave origin to our enemy has passed
        // the distance the bullet would have traveled...
        if (Point2D.distance(startX, startY, enemyX, enemyY) <= (currentTime - fireTime) * getBulletSpeed()){
            double desiredDirection = Math.atan2(enemyX - startX, enemyY - startY);  //direct bearing to target
            double angleOffset = Utils.normalRelativeAngle(desiredDirection - startBearing); //the angle the bearing was off from initial direct bearing
            double guessFactor = Math.max(-1, Math.min(1, angleOffset / maxEscapeAngle())) * direction; //guess factor
            int index = (int) Math.round((returnSegment.length - 1) /2 * (guessFactor + 1)); //the index in the segment
            returnSegment[index]++;  //increment that segment
            return true;  //return true; ie, the wave broke
        }
        return false;  //return false; ie, the wave has not yet broken
    }
}

}
