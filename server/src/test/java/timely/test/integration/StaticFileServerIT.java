package timely.test.integration;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import timely.Server;
import timely.test.IntegrationTest;

@Category(IntegrationTest.class)
@SpringBootTest(classes= Server.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@RunWith(SpringRunner.class)
public class StaticFileServerIT extends OneWaySSLBase {

    @BeforeClass
    public static void beforeClass() throws Exception {
        OneWaySSLBase.beforeClass();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        OneWaySSLBase.afterClass();
    }

    @Test(expected = NotSuccessfulException.class)
    public void testGetFavIconRequest() throws Exception {
        query("https://127.0.0.1:54322/favicon.ico", 404, "application/json");
    }

    @Test(expected = NotSuccessfulException.class)
    public void testGetBadPath() throws Exception {
        query("https://127.0.0.1:54322/index.html", 403, "application/json");
    }

    @Test(expected = NotSuccessfulException.class)
    public void testGetGoodPath() throws Exception {
        query("https://127.0.0.1:54322/webapp/test.html", 404, "application/json");
    }

 }
