package com.bflarsen.brisk.pumps;

import com.bflarsen.brisk.*;
import com.bflarsen.brisk.responders.*;
import org.junit.After;
import org.junit.Before;

public class HttpRequestRoutingPumpTest extends junit.framework.TestCase {
    public static class IndexResponder extends BaseResponder {
        @Override
        public HttpResponse buildResponse() throws Exception {
            return null;
        }
    }
    public static class TestResponder extends BaseResponder {
        @Override
        public HttpResponse buildResponse() throws Exception {
            return null;
        }
    }

    HttpServer Server;
    HttpContext Context;

    @Before
    public void setUp() {
        Server = new HttpServer();
        try {
            Server.addRoute("/", IndexResponder::new);
            Server.addRoute("/test.html", TestResponder::new);
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
        assertEquals(DefaultError404Responder.class, this.Context.Responder.getClass());

        this.Context.Responder = null;
        this.Context.Request.Protocol = "http";
        this.Context.Request.Host = "my.com";
        this.Context.Request.Path = "/";
        HttpRequestRoutingPump.chooseRoute(this.Context);
        assertEquals(IndexResponder.class, this.Context.Responder.getClass());

        this.Context.Responder = null;
        this.Context.Request.Protocol = "http";
        this.Context.Request.Host = "my.domain.com";
        this.Context.Request.Path = "/test.html";
        HttpRequestRoutingPump.chooseRoute(this.Context);
        assertEquals(TestResponder.class, this.Context.Responder.getClass());

        this.Context.Responder = null;
        this.Context.Request.Protocol = "https";
        this.Context.Request.Host = "your.domain.com";
        this.Context.Request.Path = "/test.html";
        HttpRequestRoutingPump.chooseRoute(this.Context);
        assertEquals(TestResponder.class, this.Context.Responder.getClass());
    }
}
