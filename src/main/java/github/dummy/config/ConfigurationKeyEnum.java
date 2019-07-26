package github.dummy.config;

/**
 * @author Shinobu
 * @since 2019/7/18
 */
public enum ConfigurationKeyEnum {

    /**
     * listen port
     */
    PORT("dummy.server.port"),

    /**
     * number of biz sys that will request
     */
    POOL_MAP_CAP("dummy.server.pool-map-cap");


    ConfigurationKeyEnum(String key) {
        this.key = key;
    }

    public final String key;

}
