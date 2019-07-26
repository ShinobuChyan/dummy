package github.dummy.core;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Shinobu
 * @since 2019/7/25
 */
public class TimestampWrapper {

    private static final long MAX_ALIVE_MILLIS = 2000;

    private static final VarHandle ID;

    private final long timestamp;

    private volatile int i = 0;

    static {
        MethodHandles.Lookup l = MethodHandles.lookup();
        try {
            ID = l.findVarHandle(TimestampWrapper.class, "i", int.class);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    TimestampWrapper(long timestamp) {
        this.timestamp = timestamp;

    }

    String nextIdString() {
        int ti = i;
        for (; !ID.compareAndSet(this, ti, ti + 1); ti = i) {}
        return String.format("%04d", ti + 1);
    }

    /**
     * @return 1 if success, else 0
     */
    public int helpGC(long current, ConcurrentHashMap<TimestampWrapper, TimestampWrapper> pool) {
        if (current - timestamp >= MAX_ALIVE_MILLIS) {
            synchronized (this) {
                pool.remove(this);
                return 1;
            }
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TimestampWrapper that = (TimestampWrapper) o;
        return timestamp == that.timestamp;
    }

    @Override
    public int hashCode() {
        return (int) (timestamp ^ (timestamp >>> 32));
    }
}
