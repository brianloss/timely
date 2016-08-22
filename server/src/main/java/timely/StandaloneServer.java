package timely;

import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.admin.SecurityOperations;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.minicluster.MemoryUnit;
import org.apache.accumulo.minicluster.MiniAccumuloCluster;
import org.apache.accumulo.minicluster.MiniAccumuloConfig;
import org.apache.accumulo.minicluster.ServerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.stereotype.Component;
import timely.server.ServerFactory;

import java.io.File;
import java.io.IOException;

@Component
@SpringBootApplication(scanBasePackageClasses = { TimelyConfiguration.class, ServerFactory.class })
public class StandaloneServer {

    private static final Logger LOG = LoggerFactory.getLogger(StandaloneServer.class);
    private static MiniAccumuloCluster mac;

    public static void main(String[] args) {
        File tmp = new File(args[0]);
        if (!tmp.canWrite()) {
            System.err.println("Unable to write to directory: " + tmp);
            System.exit(1);
        }
        File accumuloDir = new File(tmp, "accumulo");
        MiniAccumuloConfig macConfig = new MiniAccumuloConfig(accumuloDir, "secret");
        macConfig.setInstanceName("TimelyStandalone");
        macConfig.setZooKeeperPort(9804);
        macConfig.setNumTservers(1);
        macConfig.setMemory(ServerType.TABLET_SERVER, 1, MemoryUnit.GIGABYTE);
        try {
            mac = new MiniAccumuloCluster(macConfig);
            LOG.info("Starting MiniAccumuloCluster");
            mac.start();
            LOG.info("MiniAccumuloCluster started.");
            String instanceName = mac.getInstanceName();
            LOG.info("MiniAccumuloCluster instance name: {}", instanceName);

        } catch (IOException | InterruptedException e) {
            System.err.println("Error starting MiniAccumuloCluster: " + e.getMessage());
            System.exit(1);
        }
        try {
            Connector conn = mac.getConnector("root", "secret");
            SecurityOperations sops = conn.securityOperations();
            Authorizations rootAuths = new Authorizations("A", "B", "C", "D", "E", "F", "G", "H", "I");
            sops.changeUserAuthorizations("root", rootAuths);
        } catch (AccumuloException | AccumuloSecurityException e) {
            System.err.println("Error configuring root user");
            System.exit(1);
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                StandaloneServer.shutdown();
            }
        });

        new SpringApplicationBuilder(StandaloneServer.class).web(false).run(args);
    }

    public static void shutdown() {
        try {
            if (mac != null) {
                mac.stop();
                LOG.info("MiniAccumuloCluster shutdown.");
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error stopping MiniAccumuloCluster");
            e.printStackTrace();
        }
    }
}
