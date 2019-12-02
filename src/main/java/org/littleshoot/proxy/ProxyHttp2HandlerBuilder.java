package org.littleshoot.proxy;

import io.netty.handler.codec.http2.AbstractHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2Settings;

import static io.netty.handler.logging.LogLevel.INFO;

public final class ProxyHttp2HandlerBuilder
        extends AbstractHttp2ConnectionHandlerBuilder<ProxyHttp2Handler, ProxyHttp2HandlerBuilder> {

    private static final Http2FrameLogger logger = new Http2FrameLogger(INFO, ProxyHttp2Handler.class);

    public ProxyHttp2HandlerBuilder() {
        frameLogger(logger);
    }

    @Override
    public ProxyHttp2Handler build() {
        return super.build();
    }

    @Override
    protected ProxyHttp2Handler build(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder,
                                      Http2Settings initialSettings) {
        ProxyHttp2Handler handler = new ProxyHttp2Handler(decoder, encoder, initialSettings);
        frameListener(handler);
        return handler;
    }
}
