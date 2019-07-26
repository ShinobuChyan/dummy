package github.dummy.config;

/**
 * Main configurations
 *
 * @author Shinobu
 * @since 2019/7/18
 */
public class Configuration {

    private static final int DEFAULT_PORT = 7777;

    private static final int DEFAULT_POOL_MAP_CAP = 64;

    public final int port;

    public final int poolMapCap;

    public Configuration(Integer port, Integer poolMapCap) {
        this.port = port == null ? DEFAULT_PORT : port;
        this.poolMapCap = poolMapCap == null ? DEFAULT_POOL_MAP_CAP : poolMapCap;
    }

    @Override
    public String toString() {
        return "Configuration{" +
                "port=" + port +
                ", poolMapCap=" + poolMapCap +
                '}';
    }
}
