package timely.test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import timely.Configuration;

import com.google.common.io.Files;
import timely.TimelyConfiguration;

public class TestConfiguration extends Properties {

    private static final long serialVersionUID = 1L;
    public static final String TIMELY_HTTP_ADDRESS_DEFAULT = "localhost";

    public Configuration toConfiguration(File propertiesFile) throws Exception {
        this.store(Files.newWriter(propertiesFile, StandardCharsets.UTF_8), null);
        return new Configuration(propertiesFile);
    }

    public static TestConfiguration createMinimalConfigurationForTest() {
        TestConfiguration cfg = new TestConfiguration();
        cfg.put(Configuration.IP, "127.0.0.1");
        cfg.put(Configuration.PUT_PORT, "54321");
        cfg.put(Configuration.QUERY_PORT, "54322");
        cfg.put(Configuration.WEBSOCKET_PORT, "54323");
        cfg.put(Configuration.ZOOKEEPERS, "localhost:2181");
        cfg.put(Configuration.INSTANCE_NAME, "test");
        cfg.put(Configuration.USERNAME, "root");
        cfg.put(Configuration.PASSWORD, "secret");
        cfg.put(Configuration.TIMELY_HTTP_HOST, "localhost");
        cfg.put(Configuration.SSL_USE_GENERATED_KEYPAIR, "true");
        cfg.put(Configuration.MAX_LATENCY, "2s");
        cfg.put(Configuration.WS_TIMEOUT_SECONDS, "20");
        return cfg;
    }

    public static TimelyConfiguration createMinimalTimelyConfigurationForTest() {
        TimelyConfiguration cfg = new TimelyConfiguration();
        cfg.setIp("127.0.0.1");
        cfg.getPort().setPut(54321);
        cfg.getPort().setQuery(54322);
        cfg.getPort().setWebsocket(54323);
        cfg.setZookeepers("localhost:2181");
        cfg.setInstanceName("test");
        cfg.setUsername("root");
        cfg.setPassword("secret");
        cfg.getHttp().setHost("localhost");
        cfg.getSsl().setUseGeneratedKeypair(true);
        cfg.getWrite().setLatency("2s");
        cfg.getWebSocket().setTimeout(20);
        return cfg;
    }

}
