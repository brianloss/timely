package timely.test.integration;

import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.JdkSslClientContext;
import io.netty.handler.ssl.JdkSslContext;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import java.io.File;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.minicluster.MiniAccumuloCluster;
import org.apache.accumulo.minicluster.MiniAccumuloConfig;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;

/**
 * Base test class for SSL with anonymous access
 */
@SuppressWarnings("deprecation")
public class OneWaySSLBase extends QueryBase {

    @ClassRule
    public static final TemporaryFolder temp = new TemporaryFolder();

    protected static MiniAccumuloCluster mac = null;
    private static File clientTrustStoreFile = null;

    protected SSLSocketFactory getSSLSocketFactory() throws Exception {
        SslContextBuilder builder = SslContextBuilder.forClient();
        builder.applicationProtocolConfig(ApplicationProtocolConfig.DISABLED);
        builder.sslProvider(SslProvider.JDK);
        builder.trustManager(clientTrustStoreFile); // Trust the server cert
        SslContext ctx = builder.build();
        Assert.assertEquals(JdkSslClientContext.class, ctx.getClass());
        JdkSslContext jdk = (JdkSslContext) ctx;
        SSLContext jdkSslContext = jdk.context();
        return jdkSslContext.getSocketFactory();
    }

    protected static void setupSSL() throws Exception {
        SelfSignedCertificate serverCert = new SelfSignedCertificate();
        clientTrustStoreFile = serverCert.certificate().getAbsoluteFile();
        System.setProperty("timely.ssl.certificate-file", serverCert.certificate().getAbsolutePath());
        System.setProperty("timely.ssl.key-file", serverCert.privateKey().getAbsolutePath());
        System.setProperty("timely.ssl.use-openssl", "false");
        System.setProperty("timely.ssl.use-generated-keypair", "false");
        System.setProperty("timely.allow-anonymous-access", "true");
    }

    @Override
    protected HttpsURLConnection getUrlConnection(String username, String password, URL url) throws Exception {
        // No username/password needed for anonymous access
        return getUrlConnection(url);
    }

    protected HttpsURLConnection getUrlConnection(URL url) throws Exception {
        HttpsURLConnection.setDefaultSSLSocketFactory(getSSLSocketFactory());
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        con.setHostnameVerifier((host, session) -> true);
        return con;
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        final MiniAccumuloConfig macConfig = new MiniAccumuloConfig(temp.newFolder("mac"), "secret");
        mac = new MiniAccumuloCluster(macConfig);
        mac.start();

        System.setProperty("timely.ip", "127.0.0.1");
        System.setProperty("timely.port.put", "54321");
        System.setProperty("timely.port.query", "54322");
        System.setProperty("timely.port.websocket", "54323");
        System.setProperty("timely.instance-name", mac.getInstanceName());
        System.setProperty("timely.zookeepers", mac.getZooKeepers());
        System.setProperty("timely.username", "root");
        System.setProperty("timely.password", "secret");
        System.setProperty("timely.http.host", "localhost");
        System.setProperty("timely.write.latency", "2s");
        System.setProperty("timely.websocket.timeout", "20");

        setupSSL();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        mac.stop();
    }

    @Before
    public void setup() throws Exception {
        Connector con = mac.getConnector("root", "secret");
        con.tableOperations().list().forEach(t -> {
            if (t.startsWith("timely")) {
                try {
                    con.tableOperations().deleteRows(t, null, null);
                } catch (Exception e) {
                }
            }
        });
    }
}
