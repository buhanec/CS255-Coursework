package u1226467;

import robocode.*;
import java.util.*;
import java.awt.geom.*;

public class SimpleHistory<T> {
    private List<T> store;
    private List<long> times;
    private Map<long, T> map;

    public SimpleHistory() {
        store = new ArrayList<T>();
        times = new ArrayList<long>();
        map = new HashMap<long, T>();
    }

    public void add(long time, T element) {
        store.add(0, element);
        times.add(0, time);
        map.put(time, element);
    }

    public T get(long time) {
        return map.get(time);
    }

    public T get(int steps) {
        return
    }
}
