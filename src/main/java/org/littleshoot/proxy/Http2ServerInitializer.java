package org.littleshoot.proxy;

import org.littleshoot.proxy.impl.ClientToProxyConnection;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.littleshoot.proxy.impl.ProxyConnection;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodecFactory;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import io.netty.util.AsciiString;
import io.netty.util.ReferenceCountUtil;

/**
 * Sets up the Netty pipeline for the example server. Depending on the endpoint config, sets up the
 * pipeline for NPN or cleartext HTTP upgrade to HTTP/2.
 */
public class Http2ServerInitializer extends ChannelInitializer<Channel> {

    private final SslEngineSource sslEngineSource;
    private final boolean authenticateSslClients;
    private volatile GlobalTrafficShapingHandler globalTrafficShapingHandler;
    private DefaultHttpProxyServer defaultHttpProxyServer;



    private static final UpgradeCodecFactory upgradeCodecFactory = new UpgradeCodecFactory() {
        @Override
        public UpgradeCodec newUpgradeCodec(CharSequence protocol) {
            if (AsciiString.contentEquals(Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, protocol)) {
                return new Http2ServerUpgradeCodec(new ProxyHttp2HandlerBuilder().build());
            } else {
                return null;
            }
        }
    };

    private final SslContext sslCtx;
    private final int maxHttpContentLength;


    public Http2ServerInitializer(SslEngineSource sslEngineSource, boolean authenticateSslClients, SslContext sslCtx, int maxHttpContentLength
    , GlobalTrafficShapingHandler globalTrafficShapingHandler, DefaultHttpProxyServer defaultHttpProxyServer) {
        this.sslEngineSource = sslEngineSource;
        this.authenticateSslClients = authenticateSslClients;
        if (maxHttpContentLength < 0) {
            throw new IllegalArgumentException("maxHttpContentLength (expected >= 0): " + maxHttpContentLength);
        }
        this.sslCtx = sslCtx;
        this.maxHttpContentLength = maxHttpContentLength;
        this.globalTrafficShapingHandler = globalTrafficShapingHandler;
        this.defaultHttpProxyServer = defaultHttpProxyServer;
    }

    @Override
    public void initChannel(Channel ch) {
        if (sslCtx != null) {
            configureSsl(ch);
        } else {
            configureClearText(ch);
        }
    }

    /**
     * Configure the pipeline for TLS NPN negotiation to HTTP/2.
     */
    private void configureSsl(Channel ch) {
        ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()), new Http2OrHttpHandler(sslEngineSource, authenticateSslClients,
                                                                                    globalTrafficShapingHandler, defaultHttpProxyServer, upgradeCodecFactory));
    }

    /**
     * Configure the pipeline for a cleartext upgrade from HTTP to HTTP/2.0
     */
    private void configureClearText(Channel ch) {
        final ChannelPipeline p = ch.pipeline();
        final HttpServerCodec sourceCodec = new HttpServerCodec();
        p.addLast(sourceCodec);
        p.addLast(new HttpServerUpgradeHandler(sourceCodec, upgradeCodecFactory));
        p.addLast(new SimpleChannelInboundHandler<HttpMessage>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, HttpMessage msg) throws Exception {
                // If this handler is hit then no upgrade has been attempted and the client is just talking HTTP.
                System.err.println("Directly talking: " + msg.protocolVersion() + " (no upgrade was attempted)");
                ChannelPipeline pipeline = ctx.pipeline();
                ClientToProxyConnection proxyConnection = new ClientToProxyConnection(
                        defaultHttpProxyServer,
                        sslEngineSource,
                        authenticateSslClients,
                        ch.pipeline(),
                        globalTrafficShapingHandler);
               proxyConnection.setChannel(ch);
               proxyConnection.setContext(ctx);
                //ctx.fireChannelRegistered();
                //use the old piece of code for now


                pipeline.addLast(
                        "idle",
                        new IdleStateHandler(0, 0, defaultHttpProxyServer
                                .getIdleConnectionTimeout()));
                pipeline.addAfter(ctx.name(), null, proxyConnection);
                pipeline.replace(this, null, new HttpObjectAggregator(maxHttpContentLength));
                ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
            }
        });

        p.addLast(new UserEventLogger());
    }

    /**
     * Class that logs any User Events triggered on this channel.
     */
    private static class UserEventLogger extends ChannelInboundHandlerAdapter {
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            System.out.println("User Event Triggered: " + evt);
            ctx.fireUserEventTriggered(evt);
        }
    }

}