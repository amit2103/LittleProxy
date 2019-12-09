package org.littleshoot.proxy.http2;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

public class Http2SslClientHandler extends ChannelInitializer<Channel> {

	@Override
	protected void initChannel(Channel ch) throws Exception {
	    //TODO make it work with LittleProxy SSL handling
        SslProvider provider = OpenSsl.isAlpnSupported() ? SslProvider.OPENSSL : SslProvider.JDK;
        SslContextBuilder builder = SslContextBuilder.forClient()
            .sslProvider(provider)
            /* NOTE: the cipher filter may not include all ciphers required by the HTTP/2 specification.
             * Please refer to the HTTP/2 specification for cipher requirements. */
            .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
            .trustManager(InsecureTrustManagerFactory.INSTANCE)
            .applicationProtocolConfig(new ApplicationProtocolConfig(
                Protocol.ALPN,
                // NO_ADVERTISE is currently the only mode supported by both OpenSsl and JDK providers.
                SelectorFailureBehavior.NO_ADVERTISE,
                // ACCEPT is currently the only mode supported by both OpenSsl and JDK providers.
                SelectedListenerFailureBehavior.ACCEPT,
                ApplicationProtocolNames.HTTP_2,
                ApplicationProtocolNames.HTTP_1_1));
        ChannelPipeline p = ch.pipeline();
        String name = p.context(this).name();
        p.addAfter(name, null, new SslClientHandler(builder));
	}

}
