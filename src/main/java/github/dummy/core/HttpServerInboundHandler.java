package github.dummy.core;

import github.dummy.util.Counter;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Shinobu
 * @since 2019/7/19
 */
@ChannelHandler.Sharable
public class HttpServerInboundHandler extends ChannelInboundHandlerAdapter {

    private static HttpServerInboundHandler instance = null;

    private final HashMap<String, ConcurrentHashMap<TimestampWrapper, TimestampWrapper>> poolMap;

    private HttpServerInboundHandler(HashMap<String, ConcurrentHashMap<TimestampWrapper, TimestampWrapper>> poolMap) {
        this.poolMap = poolMap;
    }

    public static HttpServerInboundHandler newInstance(HashMap<String, ConcurrentHashMap<TimestampWrapper, TimestampWrapper>> poolMap) {
        if (instance == null) {
            instance = new HttpServerInboundHandler(poolMap);
        }
        return instance;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpContent) {
            var content = (HttpContent) msg;
            var buf = content.content();
            var poolIndex = buf.toString(CharsetUtil.UTF_8);
            buf.release();

            var pool = poolMap.get(poolIndex);
            boolean success = false;
            String respStr;
            if (pool == null) {
                respStr = "unknown biz code";
            } else {
                respStr = genId(pool);
                success = true;
            }


            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    success ? HttpResponseStatus.OK : HttpResponseStatus.INTERNAL_SERVER_ERROR,
                    Unpooled.wrappedBuffer(respStr.getBytes(CharsetUtil.UTF_8)));
            response.headers()
                    .set(HttpHeaderNames.CONTENT_TYPE, "text/plain")
                    .set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes())
                    .set(HttpHeaderNames.CONNECTION, success ? HttpHeaderValues.KEEP_ALIVE : HttpHeaderValues.CLOSE);
            ctx.write(response);
            ctx.flush();
            if (success) {
                Counter.GENERATED.add(1);
            } else {
                ctx.close();
            }
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private String genId(ConcurrentHashMap<TimestampWrapper, TimestampWrapper> pool) {
        var timestamp = System.currentTimeMillis();
        var wrapper = new TimestampWrapper(timestamp);
        TimestampWrapper prev;
        String idString;
        if ((prev = pool.putIfAbsent(wrapper, wrapper)) == null) {
            idString = wrapper.nextIdString();
        } else {
            idString = prev.nextIdString();
        }
        return timestamp + idString;
    }

}
