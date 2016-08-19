package com.bflarsen.brisk.pumps;

import com.bflarsen.brisk.HttpContext;
import com.bflarsen.brisk.HttpResponder;
import com.bflarsen.brisk.HttpResponse;
import com.bflarsen.brisk.mocks.HttpServerMock;
import org.junit.After;
import org.junit.Before;

public class HttpRequestRoutingPumpTest extends junit.framework.TestCase {
    public static class IndexResponder implements HttpResponder {
        @Override
        public HttpResponse handleRequest(HttpContext context) {
            return null;
        }
    }
    public static class TestResponder implements HttpResponder {
        @Override
        public HttpResponse handleRequest(HttpContext context) {
            return null;
        }
    }

    HttpServerMock Server;
    HttpContext Context;

    @Before
    public void setUp() {
        Server = new HttpServerMock();
        try {
            Server.addRoute("/", IndexResponder.class);
            Server.addRoute("/test.html", TestResponder.class);
        }
        catch (Exception ex) {
            System.out.print(ex.toString());
        }
        Context = new HttpContext(Server);
    }

    @After
    public void tearDown() {
        Server = null;
        Context = null;
    }

    public void test() {
        this.Context.Request.Protocol = "http";
        this.Context.Request.Host = "localhost";
        this.Context.Request.Path = "/index.html";
        HttpRequestRoutingPump.chooseRoute(this.Context);
        assertNull(this.Context.ResponderClass);

        this.Context.ResponderClass = null;
        this.Context.Request.Protocol = "http";
        this.Context.Request.Host = "my.com";
        this.Context.Request.Path = "/";
        HttpRequestRoutingPump.chooseRoute(this.Context);
        assertEquals(IndexResponder.class, this.Context.ResponderClass);

        this.Context.ResponderClass = null;
        this.Context.Request.Protocol = "http";
        this.Context.Request.Host = "my.domain.com";
        this.Context.Request.Path = "/test.html";
        HttpRequestRoutingPump.chooseRoute(this.Context);
        assertEquals(TestResponder.class, this.Context.ResponderClass);

        this.Context.ResponderClass = null;
        this.Context.Request.Protocol = "https";
        this.Context.Request.Host = "your.domain.com";
        this.Context.Request.Path = "/test.html";
        HttpRequestRoutingPump.chooseRoute(this.Context);
        assertEquals(TestResponder.class, this.Context.ResponderClass);
    }
}
