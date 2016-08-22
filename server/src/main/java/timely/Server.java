package timely;

import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.stereotype.Component;
import timely.server.ServerFactory;

@Component
@SpringBootApplication(scanBasePackageClasses = { TimelyConfiguration.class, ServerFactory.class })
public class Server {

    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    protected static final CountDownLatch LATCH = new CountDownLatch(1);

    public static void main(String[] args) {
        new SpringApplicationBuilder(Server.class).web(false).run(args);
    }

    public static void fatal(String msg, Throwable t) {
        LOG.error(msg, t);
        LATCH.countDown();
    }
}
