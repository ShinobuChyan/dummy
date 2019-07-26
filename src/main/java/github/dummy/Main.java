package github.dummy;
import github.dummy.config.Configuration;
import github.dummy.config.ConfigurationKeyEnum;
import github.dummy.core.HttpServerInboundHandler;
import github.dummy.core.TimestampWrapper;
import github.dummy.util.CopiedThreadFactory;
import github.dummy.util.Counter;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;


/**
 * @author Shinobu
 * @since 2019/7/18
 */
public class Main {

    private static volatile boolean running = true;

    private static Configuration configuration;

    private static HashMap<String, ConcurrentHashMap<TimestampWrapper, TimestampWrapper>> poolMap;

    private static ThreadPoolExecutor workerPool = new ThreadPoolExecutor(
            1, 1,
            60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(), new CopiedThreadFactory("worker"));

    public static void main(String[] args) throws Exception {
        buildConfiguration();
        init();

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            // start listen
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    .addLast(new HttpResponseEncoder())
                                    .addLast(new HttpRequestDecoder())
                                    .addLast(new HttpObjectAggregator(1024))
                                    .addLast(HttpServerInboundHandler.newInstance(poolMap));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture f = b.bind(configuration.port).sync();
            System.out.println("system started on " + configuration.port + " with pool-map-cap: " + configuration.poolMapCap);

            // help gc per 5 seconds
            workerPool.execute(() -> {
                while (running) {
                    var current = System.currentTimeMillis();
                    System.out.println("# ------------------------------");
                    System.out.println("# " + current + ": " + helpGC(current) + " wrappers released");
                    System.out.println("# " + current + ": " + Counter.GENERATED.sum() + " id generated");
                    System.out.println("# ------------------------------");
                    LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(5));
                }
            });

            // block here
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    private static void buildConfiguration() {
        var p = System.getProperties();
        var port = p.get(ConfigurationKeyEnum.PORT.key);
        var poolMapCap = p.get(ConfigurationKeyEnum.POOL_MAP_CAP.key);

        configuration = new Configuration(
                port == null ? null : Integer.valueOf(port.toString()),
                poolMapCap == null ? null : Integer.valueOf(poolMapCap.toString()));
    }

    private static void init() {
        if (poolMap != null) {
            return;
        }

        var poolMapCap = configuration.poolMapCap;
        poolMap = new HashMap<>(poolMapCap << 1);
        for (int i = 0; i < poolMapCap; i++) {
            poolMap.put(String.valueOf(i), new ConcurrentHashMap<>(1024));
        }
    }

    private static int helpGC(long current) {
        var count = 0;
        var pm = poolMap;
        for (Map.Entry<String, ConcurrentHashMap<TimestampWrapper, TimestampWrapper>> entry : pm.entrySet()) {
            var p = entry.getValue();
            for (TimestampWrapper wrapper : p.keySet()) {
                count += wrapper.helpGC(current, p);
            }
        }
        return count;
    }

    public static void noNeed() {
        running = false;
    }

}
