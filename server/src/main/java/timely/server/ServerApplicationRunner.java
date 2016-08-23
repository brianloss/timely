package timely.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.internal.SystemPropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;
import timely.Server;
import timely.TimelyConfiguration;
import timely.api.response.TimelyException;
import timely.auth.AuthCache;
import timely.auth.VisibilityCache;
import timely.netty.websocket.WebSocketRequestDecoder;
import timely.store.DataStore;

import java.net.InetSocketAddress;

@Component
public class ServerApplicationRunner implements ApplicationRunner, ApplicationListener<ContextClosedEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(Server.class);
    private static final int EPOLL_MIN_MAJOR_VERSION = 2;
    private static final int EPOLL_MIN_MINOR_VERSION = 6;
    private static final int EPOLL_MIN_PATCH_VERSION = 32;
    private static final String OS_NAME = "os.name";
    private static final String OS_VERSION = "os.version";

    private final ApplicationContext appContext;
    private final TimelyConfiguration config;
    private final DataStore dataStore;
    private EventLoopGroup tcpWorkerGroup;
    private EventLoopGroup tcpBossGroup;
    private EventLoopGroup httpWorkerGroup;
    private EventLoopGroup httpBossGroup;
    private EventLoopGroup wsWorkerGroup;
    private EventLoopGroup wsBossGroup;
    private Channel putChannelHandle;
    private Channel queryChannelHandle;
    private Channel wsChannelHandle;

    @Autowired
    public ServerApplicationRunner(ApplicationContext appContext, TimelyConfiguration config, DataStore dataStore) {
        this.appContext = appContext;
        this.config = config;
        this.dataStore = dataStore;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        AuthCache.setSessionMaxAge(config);
        VisibilityCache.init(config);

        final boolean useEpoll = useEpoll();
        Class<? extends ServerSocketChannel> channelClass;
        if (useEpoll) {
            tcpWorkerGroup = new EpollEventLoopGroup();
            tcpBossGroup = new EpollEventLoopGroup();
            httpWorkerGroup = new EpollEventLoopGroup();
            httpBossGroup = new EpollEventLoopGroup();
            wsWorkerGroup = new EpollEventLoopGroup();
            wsBossGroup = new EpollEventLoopGroup();
            channelClass = EpollServerSocketChannel.class;
        } else {
            tcpWorkerGroup = new NioEventLoopGroup();
            tcpBossGroup = new NioEventLoopGroup();
            httpWorkerGroup = new NioEventLoopGroup();
            httpBossGroup = new NioEventLoopGroup();
            wsWorkerGroup = new NioEventLoopGroup();
            wsBossGroup = new NioEventLoopGroup();
            channelClass = NioServerSocketChannel.class;
        }
        LOG.info("Using channel class {}", channelClass.getSimpleName());

        final ServerBootstrap putServer = new ServerBootstrap();
        putServer.group(tcpBossGroup, tcpWorkerGroup);
        putServer.channel(channelClass);
        putServer.handler(new LoggingHandler());
        putServer.childHandler(new TimelyChannelInitializer(appContext, "putBuilder"));
        putServer.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        putServer.option(ChannelOption.SO_BACKLOG, 128);
        putServer.option(ChannelOption.SO_KEEPALIVE, true);
        final int putPort = config.getPort().getPut();
        putChannelHandle = putServer.bind(putPort).sync().channel();
        final String putAddress = ((InetSocketAddress) putChannelHandle.localAddress()).getAddress().getHostAddress();

        final ServerBootstrap queryServer = new ServerBootstrap();
        queryServer.group(httpBossGroup, httpWorkerGroup);
        queryServer.channel(channelClass);
        queryServer.handler(new LoggingHandler());
        queryServer.childHandler(new TimelyChannelInitializer(appContext, "queryBuilder"));
        queryServer.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        queryServer.option(ChannelOption.SO_BACKLOG, 128);
        queryServer.option(ChannelOption.SO_KEEPALIVE, true);
        final int queryPort = config.getPort().getQuery();
        queryChannelHandle = queryServer.bind(queryPort).sync().channel();
        final String queryAddress = ((InetSocketAddress) queryChannelHandle.localAddress()).getAddress()
                .getHostAddress();

        final ServerBootstrap wsServer = new ServerBootstrap();
        wsServer.group(wsBossGroup, wsWorkerGroup);
        wsServer.channel(channelClass);
        wsServer.handler(new LoggingHandler());
        wsServer.childHandler(new TimelyChannelInitializer(appContext, "websocketBuilder"));
        wsServer.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        wsServer.option(ChannelOption.SO_BACKLOG, 128);
        wsServer.option(ChannelOption.SO_KEEPALIVE, true);
        final int wsPort = config.getPort().getWebsocket();
        wsChannelHandle = wsServer.bind(wsPort).sync().channel();
        final String wsAddress = ((InetSocketAddress) wsChannelHandle.localAddress()).getAddress().getHostAddress();

        LOG.info(
                "Server started. Listening on {}:{} for TCP traffic, {}:{} for HTTP traffic, and {}:{} for WebSocket traffic",
                putAddress, putPort, queryAddress, queryPort, wsAddress, wsPort);
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        try {
            putChannelHandle.close().sync();
        } catch (final Exception e) {
            LOG.error("Error shutting down tcp channel", e);
        }
        try {
            queryChannelHandle.close().sync();
        } catch (final Exception e) {
            LOG.error("Error shutting down http channel", e);
        }
        try {
            wsChannelHandle.close().sync();
        } catch (final Exception e) {
            LOG.error("Error shutting down websocket channel", e);
        }
        try {
            tcpBossGroup.shutdownGracefully().sync();
        } catch (final Exception e) {
            LOG.error("Error closing Netty TCP boss thread group", e);
        }
        try {
            tcpWorkerGroup.shutdownGracefully().sync();
        } catch (final Exception e) {
            LOG.error("Error closing Netty TCP worker thread group", e);
        }
        try {
            httpBossGroup.shutdownGracefully().sync();
        } catch (final Exception e) {
            LOG.error("Error closing Netty HTTP boss thread group", e);
        }
        try {
            httpWorkerGroup.shutdownGracefully().sync();
        } catch (final Exception e) {
            LOG.error("Error closing Netty HTTP worker thread group", e);
        }
        try {
            wsBossGroup.shutdownGracefully().sync();
        } catch (final Exception e) {
            LOG.error("Error closing Netty websocket boss thread group", e);
        }
        try {
            wsWorkerGroup.shutdownGracefully().sync();
        } catch (final Exception e) {
            LOG.error("Error closing Netty websocket worker thread group", e);
        }
        try {
            dataStore.flush();
        } catch (TimelyException e) {
            LOG.error("Error flushing to server during shutdown", e);
        }
        WebSocketRequestDecoder.close();
        LOG.info("Server shut down.");
    }

    private static boolean useEpoll() {

        // Should we just return true if this is Linux and if we get an error
        // during Epoll
        // setup handle it there?
        final String os = SystemPropertyUtil.get(OS_NAME).toLowerCase().trim();
        final String[] version = SystemPropertyUtil.get(OS_VERSION).toLowerCase().trim().split("\\.");
        if (os.startsWith("linux") && version.length >= 3) {
            final int major = Integer.parseInt(version[0]);
            if (major > EPOLL_MIN_MAJOR_VERSION) {
                return true;
            } else if (major == EPOLL_MIN_MAJOR_VERSION) {
                final int minor = Integer.parseInt(version[1]);
                if (minor > EPOLL_MIN_MINOR_VERSION) {
                    return true;
                } else if (minor == EPOLL_MIN_MINOR_VERSION) {
                    final int patch = Integer.parseInt(version[2].substring(0, 2));
                    return patch >= EPOLL_MIN_PATCH_VERSION;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}
