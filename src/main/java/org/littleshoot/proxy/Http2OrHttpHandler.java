package org.littleshoot.proxy;

import org.littleshoot.proxy.impl.ClientToProxyConnection;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import io.netty.util.ReferenceCountUtil;

/**
 * Negotiates with the browser if HTTP2 or HTTP is going to be used. Once decided, the Netty
 * pipeline is setup with the correct handlers for the selected protocol.
 */
public class Http2OrHttpHandler extends ApplicationProtocolNegotiationHandler {

    private static final int MAX_CONTENT_LENGTH = 1024 * 100;

    private final SslEngineSource sslEngineSource;
    private final boolean authenticateSslClients;
    private  volatile GlobalTrafficShapingHandler globalTrafficShapingHandler;
    private final DefaultHttpProxyServer defaultHttpProxyServer;
    private final HttpServerUpgradeHandler.UpgradeCodecFactory upgradeCodecFactory;

    protected Http2OrHttpHandler(SslEngineSource sslEngineSource, boolean authenticateSslClients,
                                 GlobalTrafficShapingHandler globalTrafficShapingHandler, DefaultHttpProxyServer defaultHttpProxyServer,
                                 HttpServerUpgradeHandler.UpgradeCodecFactory upgradeCodecFactory) {
        super(ApplicationProtocolNames.HTTP_1_1);
        this.sslEngineSource = sslEngineSource;
        this.authenticateSslClients = authenticateSslClients;
        this.globalTrafficShapingHandler = globalTrafficShapingHandler;
        this.defaultHttpProxyServer = defaultHttpProxyServer;
        this.upgradeCodecFactory = upgradeCodecFactory;
    }

    @Override
    protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws Exception {
        if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
            ctx.pipeline().addLast(new ProxyHttp2HandlerBuilder().build());
            return;
        }

        if (ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
            final ChannelPipeline p = ctx.pipeline();
            final HttpServerCodec sourceCodec = new HttpServerCodec();
            p.addLast(sourceCodec);
            p.addLast(new HttpServerUpgradeHandler(sourceCodec, upgradeCodecFactory));
            p.addLast(new SimpleChannelInboundHandler<HttpMessage>() {
                @Override
                protected void channelRead0(ChannelHandlerContext ctx, HttpMessage msg) throws Exception {
                    // If this handler is hit then no upgrade has been attempted and the client is just talking HTTP.
                    ChannelPipeline pipeline = ctx.pipeline();
                    ClientToProxyConnection proxyConnection = new ClientToProxyConnection(defaultHttpProxyServer, sslEngineSource,
                                                                                          authenticateSslClients, pipeline,
                                                                                          globalTrafficShapingHandler);
                    proxyConnection.setChannel(ctx.channel());
                    proxyConnection.setContext(ctx);
                    ctx.fireChannelRegistered();
                    //use the old piece of code for now


                    pipeline.addLast("idle", new IdleStateHandler(0, 0, defaultHttpProxyServer.getIdleConnectionTimeout()));
                    pipeline.addAfter(ctx.name(), null, proxyConnection);
                    pipeline.replace(this, null, new HttpObjectAggregator(MAX_CONTENT_LENGTH));
                    ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
                }
            });
        }

        throw new IllegalStateException("unknown protocol: " + protocol);
    }
}