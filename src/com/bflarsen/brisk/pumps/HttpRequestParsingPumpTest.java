package com.bflarsen.brisk.pumps;

import com.bflarsen.brisk.HttpContext;
import com.bflarsen.brisk.mocks.HttpServerMock;
import org.junit.After;
import org.junit.Before;

import java.io.BufferedReader;
import java.io.StringReader;

public class HttpRequestParsingPumpTest extends junit.framework.TestCase {

    HttpServerMock Server;

    @Before
    public void setUp() {
        Server = new HttpServerMock();
    }

    @After
    public void tearDown() {
        Server = null;
    }

    private HttpContext parseHttpRequest(String requestString) {
        HttpContext context = new HttpContext(Server);
        try {
            context.RequestStream = new BufferedReader(new StringReader(requestString));
            HttpRequestParsingPump.parseRequest(context);
        }
        finally {
            if (context.RequestStream != null) {
                try {
                    context.RequestStream.close();
                }
                catch (Exception ex) {
                    context.Server.ExceptionHandler(ex, "", "", "closing request stream");
                }
            }
        }
        return context;
    }

    public void test_it_should_parse_minimal_HTTP_1_0_request() throws Exception {
        HttpContext context = parseHttpRequest(
                "GET / HTTP/1.0\r\n"
                + "\r\n"
        );

        assertEquals("GET", context.Request.Method);
        assertEquals("/", context.Request.Resource);
        assertEquals("HTTP/1.0", context.Request.HttpVersion);
    }

    public void test_it_should_parse_minimal_HTTP_1_1_request() throws Exception {
        HttpContext context = parseHttpRequest(
                "GET / HTTP/1.1\r\n"
                + "Host: test.site\r\n"
                + "\r\n"
        );

        assertEquals("GET", context.Request.Method);
        assertEquals("/", context.Request.Resource);
        assertEquals("HTTP/1.1", context.Request.HttpVersion);
        assertEquals("test.site", context.Request.Host);
    }

    public void test_it_should_parse_a_request_with_all_sorts_of_properties() throws Exception {
        HttpContext context = parseHttpRequest(
                "GET /Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html HTTP/1.1\r\n"
                + "Host: www.w3.org\r\n"
                + "Connection: keep-alive\r\n"
                + "Cache-Control: max-age=0\r\n"
                + "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\r\n"
                + "User-Agent: Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2062.103 Safari/537.36\r\n"
                + "Accept-Encoding: gzip,deflate,sdch\r\n"
                + "Accept-Language: en-US,en;q=0.8\r\n"
                + "If-None-Match: \"3e812-32e18f06e8680\"\r\n"
                + "If-Modified-Since: Thu, 14 May 1998 01:32:58 GMT\r\n"
                + "\r\n"
        );

        assertEquals("GET", context.Request.Method);
        assertEquals("/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html", context.Request.Resource);
        assertEquals("HTTP/1.1", context.Request.HttpVersion);
        assertEquals("www.w3.org", context.Request.getHeader("Host"));
        assertEquals("keep-alive", context.Request.getHeader("Connection"));
        assertEquals("max-age=0", context.Request.getHeader("Cache-Control"));
        assertEquals("text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8", context.Request.getHeader("Accept"));
        assertEquals("Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2062.103 Safari/537.36", context.Request.getHeader("User-Agent"));
        assertEquals("gzip,deflate,sdch", context.Request.getHeader("Accept-Encoding"));
        assertEquals("en-US,en;q=0.8", context.Request.getHeader("Accept-Language"));
        assertEquals("\"3e812-32e18f06e8680\"", context.Request.getHeader("If-None-Match"));
        assertEquals("Thu, 14 May 1998 01:32:58 GMT", context.Request.getHeader("If-Modified-Since"));
    }

    public void test_it_should_always_return_CAPS_for_a_request_method() throws Exception {
        HttpContext context = parseHttpRequest(
                "get / HTTP/1.0\r\n"
                + "\r\n"
        );

        assertEquals("GET", context.Request.Method);
    }

    public void test_it_should_parse_out_url_params() {
        HttpContext context = parseHttpRequest(
                "get /index.html?stuff=hi&no-stuff=&more_stuff=blablabla HTTP/1.0\r\n"
                + "\r\n"
        );

        assertEquals("/index.html", context.Request.Path);
        assertEquals("hi", context.Request.getParam("stuff"));
        assertEquals("", context.Request.getParam("no-stuff"));
        assertEquals("blablabla", context.Request.getParam("more_stuff"));
        assertEquals(null, context.Request.getParam("stuff-not-even-sent"));
    }

    public void test_it_should_properly_decode_encoded_url_params() {
        HttpContext context = parseHttpRequest(
                "get /?copy=brent%20%26%20patricia%20larsen&amp;trade%20mark=This%20Stuff%20is%20Trademarked! HTTP/1.0\r\n"
                + "\r\n"
        );

        assertEquals("/", context.Request.Path);
        assertEquals("brent & patricia larsen", context.Request.getParam("copy"));
        assertEquals("This Stuff is Trademarked!", context.Request.getParam("trade mark"));
    }

    public void test_it_should_take_the_last_value_of_repeated_request_params() throws Exception {
        HttpContext context = parseHttpRequest(
                "get /save.html?id=1&id=joe&id=brent%20larsen&amp;name=stuff HTTP/1.0\r\n"
                + "\r\n"
        );

        assertEquals("stuff", context.Request.getParam("name"));
        assertEquals("brent larsen", context.Request.getParam("id"));
    }

    public void test_it_should_convert_array_notation_into_a_map() throws Exception {
        HttpContext context = parseHttpRequest(
                "get /?id[jim]=bob&id[5]=joe&id[up][left][7]=jimmy&id[down][right]=neutron&x[abc%20123]=stuff HTTP/1.0\r\n"
                        + "\r\n"
        );

        assertNotNull(context.Request.getParam("id"));
        assertEquals("bob", context.Request.getParam("id", "jim"));
        assertEquals("joe", context.Request.getParam("id", "5"));
        assertNotNull(context.Request.getParam("id", "up"));
        assertNotNull(context.Request.getParam("id", "down"));
        assertNotNull(context.Request.getParam("id", "up", "left"));
        assertNotNull(context.Request.getParam("id", "down", "right"));
        assertEquals("jimmy", context.Request.getParam("id", "up", "left", "7"));
        assertEquals("neutron", context.Request.getParam("id", "down", "right"));
        assertEquals("stuff", context.Request.getParam("x", "abc 123"));
        assertNull(context.Request.getParam("id", "stuff", "whatnot"));
    }

//            "should parse cookies":
//    function ()
//    {
//    },
//
//            "should parse request body":
//    function ()
//    {
//    },
//
//            "should parse request body - simple form submission":
//    function ()
//    {
//    },
//
//            "should parse request body - multi-part form submission":
//    function ()
//    {
//    },
//
//            "should parse request body - json":
//    function ()
//    {
//    },
//
}
