package org.littleshoot.proxy;

import static org.eclipse.jetty.util.ssl.SslContextFactory.TRUST_ALL_CERTS;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.Test;

import com.google.common.net.MediaType;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Tests just a single basic proxy.
 */
public class SimpleProxyTest extends BaseProxyTest {
    @Override
    protected void setUp() {
        this.proxyServer = bootstrapProxy()
                .withPort(0)
                .start();
    }

    /*@Test
    public void testHttp2Get() throws Exception {

        OkHttpClient client = getUnsafeOkHttpClient();

        Request request = new Request.Builder()
                .url("https://google.com") // The Http2Server should be running here.
                .build();
        long startTime = System.nanoTime();
        for (int i=0; i<3; i++) {
            Thread.sleep(1000);
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    System.out.println(e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    long duration = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTime);
                    System.out.println("After " + duration + " seconds: " + response.body().string());
                }
            });
        }

    }


    // http://stackoverflow.com/questions/25509296/trusting-all-certificates-with-okhttp
    private static OkHttpClient getUnsafeOkHttpClient() {
        try {
            List<Protocol> protocols = new ArrayList<>();
            protocols.add(Protocol.HTTP_2);
            protocols.add(Protocol.HTTP_1_1);
            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, TRUST_ALL_CERTS, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            Proxy proxyTest = new Proxy(Proxy.Type.HTTP,new InetSocketAddress("localhost", 8080));

            OkHttpClient okHttpClient = new OkHttpClient.Builder().protocols(protocols).proxy(proxyTest).sslSocketFactory(sslSocketFactory).hostnameVerifier((hostname, session) -> true).build();

            return okHttpClient;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }*/
}
