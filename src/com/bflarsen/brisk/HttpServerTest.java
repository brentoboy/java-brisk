package com.bflarsen.brisk;

import com.bflarsen.brisk.mocks.HttpServerMock;
import org.junit.After;
import org.junit.Before;

import java.util.regex.Pattern;

public class HttpServerTest extends junit.framework.TestCase {
    HttpServer Server;

    @Before
    public void setUp() {
        Server = new HttpServerMock();
    }

    @After
    public void tearDown() {
        Server = null;
    }

    public void test_buildUrlPattern() throws Exception {
        Pattern regex = Server.buildRegexPattern("/");
        assertTrue(regex.matcher("http://localhost/").matches());
        assertTrue(regex.matcher("https://localhost/").matches());
        assertTrue(regex.matcher("http://mydomain.com/").matches());

        assertFalse(regex.matcher("http://localhost/s").matches());
        assertFalse(regex.matcher("http://localhost/s/").matches());
        assertFalse(regex.matcher("httpx://localhost/").matches());


        regex = Server.buildRegexPattern("//*mydomain.com/");
        assertTrue(regex.matcher("http://mydomain.com/").matches());
        assertTrue(regex.matcher("https://www.mydomain.com/").matches());
        assertTrue(regex.matcher("http://bla.mydomain.com/").matches());
        assertTrue(regex.matcher("http://stuff-mydomain.com/").matches());

        assertFalse(regex.matcher("http://mydomain.com/s").matches());
        assertFalse(regex.matcher("http://mydomain.com/s/").matches());
        assertFalse(regex.matcher("httpx://mydomain.com/").matches());
        assertFalse(regex.matcher("http://my-domain.com/").matches());


        regex = Server.buildRegexPattern("http://*mydomain.com/stuff.html");
        assertTrue(regex.matcher("http://mydomain.com/stuff.html").matches());
        assertTrue(regex.matcher("http://www.mydomain.com/stuff.html").matches());

        assertFalse(regex.matcher("https://mydomain.com/stuff.html").matches());
        assertFalse(regex.matcher("https://www.mydomain.com/stuff.html").matches());
        assertFalse(regex.matcher("http://mydomain.com/s").matches());
        assertFalse(regex.matcher("httpx://mydomain.com/").matches());
        assertFalse(regex.matcher("http://my-domain.com/stuff.htmlstuff").matches());

        regex = Server.buildRegexPattern("//*smartcaresystem.com/x");
        assertTrue(regex.matcher("http://rs1.smartcaresystem.com/x").matches());
        assertTrue(regex.matcher("http://local.smartcaresystem.com/x").matches());
        assertTrue(regex.matcher("http://local-cloud.smartcaresystem.com/x").matches());
        assertTrue(regex.matcher("https://local-cloud.smartcaresystem.com/x").matches());

    }
}
