package timely.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.ssl.*;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import timely.Server;
import timely.TimelyConfiguration;
import timely.netty.http.*;
import timely.netty.http.auth.BasicAuthLoginRequestHandler;
import timely.netty.http.auth.X509LoginRequestHandler;
import timely.netty.http.timeseries.*;
import timely.netty.tcp.TcpDecoder;
import timely.netty.tcp.TcpPutHandler;
import timely.netty.tcp.TcpVersionHandler;
import timely.netty.websocket.WSMetricPutHandler;
import timely.netty.websocket.WSVersionRequestHandler;
import timely.netty.websocket.WebSocketHttpCookieHandler;
import timely.netty.websocket.WebSocketRequestDecoder;
import timely.netty.websocket.subscription.WSAddSubscriptionRequestHandler;
import timely.netty.websocket.subscription.WSCloseSubscriptionRequestHandler;
import timely.netty.websocket.subscription.WSCreateSubscriptionRequestHandler;
import timely.netty.websocket.subscription.WSRemoveSubscriptionRequestHandler;
import timely.netty.websocket.timeseries.*;
import timely.server.TimelyChannelInitializer.PipelineBuilder;

import javax.net.ssl.SSLException;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.util.Date;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Configuration
public class ServerFactory {

    private static final Logger LOG = LoggerFactory.getLogger(Server.class);
    private static final String WS_PATH = "/websocket";

    @Bean(name = "putBuilder")
    @Scope(SCOPE_PROTOTYPE)
    public PipelineBuilder putBuilder() {
        return new PipelineBuilder() {

            @Autowired
            private TcpDecoder tcpDecoder;
            @Autowired
            private TcpPutHandler tcpPutHandler;
            @Autowired
            private TcpVersionHandler tcpVersionHandler;

            @Override
            public void build(Channel channel) {
                ChannelPipeline pipeline = channel.pipeline();
                pipeline.addLast("frame", new DelimiterBasedFrameDecoder(8192, true, Delimiters.lineDelimiter()));
                pipeline.addLast("putDecoder", tcpDecoder);
                pipeline.addLast("putHandler", tcpPutHandler);
                pipeline.addLast("versionHandler", tcpVersionHandler);
            }
        };
    }

    @Bean(name = "queryBuilder")
    @Scope(SCOPE_PROTOTYPE)
    public PipelineBuilder queryBuilder() {
        return new PipelineBuilder() {

            @Autowired
            private SslContext sslContext;
            @Autowired
            private TimelyConfiguration config;
            @Autowired
            private NonSecureHttpHandler nonSecureHttpHandler;
            @Autowired
            private timely.netty.http.HttpRequestDecoder requestDecoder;
            @Autowired
            private HttpStaticFileServerHandler staticFileServerHandler;
            @Autowired
            private StrictTransportHandler strictTransportHandler;
            @Autowired
            private X509LoginRequestHandler x509LoginRequestHandler;
            @Autowired
            private BasicAuthLoginRequestHandler basicAuthLoginRequestHandler;
            @Autowired
            private HttpAggregatorsRequestHandler httpAggregatorsRequestHandler;
            @Autowired
            private HttpMetricsRequestHandler httpMetricsRequestHandler;
            @Autowired
            private HttpQueryRequestHandler httpQueryRequestHandler;
            @Autowired
            private HttpSearchLookupRequestHandler httpSearchLookupRequestHandler;
            @Autowired
            private HttpSuggestRequestHandler httpSuggestRequestHandler;
            @Autowired
            private HttpVersionRequestHandler httpVersionRequestHandler;
            @Autowired
            private HttpMetricPutHandler httpMetricPutHandler;
            @Autowired
            private TimelyExceptionHandler timelyExceptionHandler;

            @Override
            public void build(Channel channel) {
                ChannelPipeline pipeline = channel.pipeline();
                pipeline.addLast("ssl", sslContext.newHandler(channel.alloc()));
                pipeline.addLast("encoder", new HttpResponseEncoder());
                pipeline.addLast("decoder", new HttpRequestDecoder());
                pipeline.addLast("non-secure", nonSecureHttpHandler);
                pipeline.addLast("compressor", new HttpContentCompressor());
                pipeline.addLast("decompressor", new HttpContentDecompressor());
                pipeline.addLast("aggregator", new HttpObjectAggregator(8192));
                pipeline.addLast("chunker", new ChunkedWriteHandler());
                TimelyConfiguration.Cors corsCfg = config.getCors();
                final CorsConfig.Builder ccb;
                if (corsCfg.isAllowAnyOrigin()) {
                    ccb = new CorsConfig.Builder();
                } else {
                    ccb = new CorsConfig.Builder(corsCfg.getAllowedOrigins().stream().toArray(String[]::new));
                }
                if (corsCfg.isAllowNullOrigin()) {
                    ccb.allowNullOrigin();
                }
                if (corsCfg.isAllowCredentials()) {
                    ccb.allowCredentials();
                }
                corsCfg.getAllowedMethods().stream().map(HttpMethod::valueOf).forEach(ccb::allowedRequestMethods);
                corsCfg.getAllowedHeaders().forEach(ccb::allowedRequestHeaders);
                CorsConfig cors = ccb.build();
                LOG.trace("Cors configuration: {}", cors);
                pipeline.addLast("cors", new CorsHandler(cors));
                pipeline.addLast("queryDecoder", requestDecoder);
                pipeline.addLast("fileServer", staticFileServerHandler);
                pipeline.addLast("strict", strictTransportHandler);
                pipeline.addLast("login", x509LoginRequestHandler);
                pipeline.addLast("doLogin", basicAuthLoginRequestHandler);
                pipeline.addLast("aggregators", httpAggregatorsRequestHandler);
                pipeline.addLast("metrics", httpMetricsRequestHandler);
                pipeline.addLast("query", httpQueryRequestHandler);
                pipeline.addLast("search", httpSearchLookupRequestHandler);
                pipeline.addLast("suggest", httpSuggestRequestHandler);
                pipeline.addLast("version", httpVersionRequestHandler);
                pipeline.addLast("put", httpMetricPutHandler);
                pipeline.addLast("error", timelyExceptionHandler);

            }
        };
    }

    @Bean(name = "websocketBuilder")
    @Scope(SCOPE_PROTOTYPE)
    public PipelineBuilder websocketBuilder() {
        return new PipelineBuilder() {

            @Autowired
            private SslContext sslContext;
            @Autowired
            private WebSocketHttpCookieHandler webSocketHttpCookieHandler;
            @Autowired
            private IdleStateHandler idleStateHandler;
            @Autowired
            private WebSocketServerProtocolHandler webSocketServerProtocolHandler;
            @Autowired
            private WebSocketRequestDecoder webSocketRequestDecoder;
            @Autowired
            private WSAggregatorsRequestHandler wsAggregatorsRequestHandler;
            @Autowired
            private WSMetricsRequestHandler wsMetricsRequestHandler;
            @Autowired
            private WSQueryRequestHandler wsQueryRequestHandler;
            @Autowired
            private WSSearchLookupRequestHandler wsSearchLookupRequestHandler;
            @Autowired
            private WSSuggestRequestHandler wsSuggestRequestHandler;
            @Autowired
            private WSVersionRequestHandler wsVersionRequestHandler;
            @Autowired
            private WSMetricPutHandler wsMetricPutHandler;
            @Autowired
            private WSCreateSubscriptionRequestHandler wsCreateSubscriptionRequestHandler;
            @Autowired
            private WSAddSubscriptionRequestHandler wsAddSubscriptionRequestHandler;
            @Autowired
            private WSRemoveSubscriptionRequestHandler wsRemoveSubscriptionRequestHandler;
            @Autowired
            private WSCloseSubscriptionRequestHandler wsCloseSubscriptionRequestHandler;

            @Override
            public void build(Channel channel) {
                ChannelPipeline pipeline = channel.pipeline();
                pipeline.addLast("ssl", sslContext.newHandler(channel.alloc()));
                pipeline.addLast("httpServer", new HttpServerCodec());
                pipeline.addLast("aggregator", new HttpObjectAggregator(8192));
                pipeline.addLast("sessionExtractor", webSocketHttpCookieHandler);
                pipeline.addLast("idle-handler", idleStateHandler);
                pipeline.addLast("ws-protocol", webSocketServerProtocolHandler);
                pipeline.addLast("wsDecoder", webSocketRequestDecoder);
                pipeline.addLast("aggregators", wsAggregatorsRequestHandler);
                pipeline.addLast("metrics", wsMetricsRequestHandler);
                pipeline.addLast("query", wsQueryRequestHandler);
                pipeline.addLast("lookup", wsSearchLookupRequestHandler);
                pipeline.addLast("suggest", wsSuggestRequestHandler);
                pipeline.addLast("version", wsVersionRequestHandler);
                pipeline.addLast("put", wsMetricPutHandler);
                pipeline.addLast("create", wsCreateSubscriptionRequestHandler);
                pipeline.addLast("add", wsAddSubscriptionRequestHandler);
                pipeline.addLast("remove", wsRemoveSubscriptionRequestHandler);
                pipeline.addLast("close", wsCloseSubscriptionRequestHandler);

            }
        };
    }

    @Bean
    @Scope(SCOPE_PROTOTYPE)
    public IdleStateHandler idleStateHandler(TimelyConfiguration conf) {
        return new IdleStateHandler(conf.getWebSocket().getTimeout(), 0, 0);
    }

    @Bean
    @Scope(SCOPE_PROTOTYPE)
    public WebSocketServerProtocolHandler webSocketServerProtocolHandler() {
        return new WebSocketServerProtocolHandler(WS_PATH, null, true);
    }

    @Bean
    public SslContext sslContext(TimelyConfiguration config) throws CertificateException, SSLException {
        TimelyConfiguration.Ssl sslCfg = config.getSsl();
        Boolean generate = sslCfg.isUseGeneratedKeypair();
        SslContextBuilder ssl;
        if (generate) {
            LOG.warn("Using generated self signed server certificate");
            Date begin = new Date();
            Date end = new Date(begin.getTime() + 86400000);
            SelfSignedCertificate ssc = new SelfSignedCertificate("localhost", begin, end);
            ssl = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey());
        } else {
            String cert = sslCfg.getCertificateFile();
            String key = sslCfg.getKeyFile();
            String keyPass = sslCfg.getKeyPassword();
            if (null == cert || null == key) {
                throw new IllegalArgumentException("Check your SSL properties, something is wrong.");
            }
            ssl = SslContextBuilder.forServer(new File(cert), new File(key), keyPass);
        }

        ssl.ciphers(sslCfg.getUseCiphers());

        // Can't set to REQUIRE because the CORS pre-flight requests will fail.
        ssl.clientAuth(ClientAuth.OPTIONAL);

        Boolean useOpenSSL = sslCfg.isUseOpenssl();
        if (useOpenSSL) {
            ssl.sslProvider(SslProvider.OPENSSL);
        } else {
            ssl.sslProvider(SslProvider.JDK);
        }
        String trustStore = sslCfg.getTrustStoreFile();
        if (null != trustStore) {
            if (!trustStore.isEmpty()) {
                ssl.trustManager(new File(trustStore));
            }
        }
        SslContext sslContext = ssl.build();
        if (sslContext instanceof OpenSslContext) {
            OpenSslServerContext openssl = (OpenSslServerContext) sslContext;
            String application = "Timely_" + config.getPort().getQuery();
            OpenSslServerSessionContext opensslCtx = openssl.sessionContext();
            opensslCtx.setSessionCacheEnabled(true);
            opensslCtx.setSessionCacheSize(128);
            opensslCtx.setSessionIdContext(application.getBytes(StandardCharsets.UTF_8));
            opensslCtx.setSessionTimeout(config.getSessionMaxAge());
        }
        return sslContext;
    }
}
