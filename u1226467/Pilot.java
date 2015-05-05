package u1226467;

import robocode.*;

public interface Pilot {
    public void update(long time);
    public void move();
    public void onHitByBullet(HitByBulletEvent e);
}
