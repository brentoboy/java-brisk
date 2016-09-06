package com.bflarsen.brisk.pumps;

import com.bflarsen.brisk.*;
import static com.bflarsen.util.Logger.*;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;

public class HttpResponseSendingPump implements Runnable {

    private HttpServer httpServerInstance;

    public HttpResponseSendingPump(HttpServer serverInstance) {
        this.httpServerInstance = serverInstance;
    }

    @Override
    public void run() {
        Worker[] workers = new Worker[httpServerInstance.NumberOfResponseSendingThreadsToCreate];
        // spawn a bunch of workers
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Worker(this);
            workers[i].start();
        }
        // wait for them to close
        for (int i = 0; i < workers.length; i++) {
            try {
                workers[i].wait();
            }
            catch(Exception ex) {}
        }
    }

    public static void sendResponse(HttpContext context) throws Exception {
        if (context == null)
            throw new Exception("Empty Context object in sendResponse.");
        if (context.Response == null)
            throw new Exception("Empty Response object in sendResponse.");
        if (context.RequestStream == null)
            throw new Exception("Empty ResponseStream object in sendResponse.");

        HttpResponse response = context.Response;
        OutputStream stream = context.ResponseStream;
        PrintWriter streamWriter = new java.io.PrintWriter(stream, true);

        Long length = response.getContentLength();
        if (length != null) {
            response.setHeader("Content-Length", length.toString());
        }

        // send the first line ... something like this :  "HTTP/1.1 200 OK"
        streamWriter.println(String.format(
                "%s %d %s"
                , response.getHttpVersion()
                , response.getStatusCode()
                , response.getStatusDescription()
        ));

        // send all the header lines ... something like this: "Header-Name: header-value"
        for (Map.Entry<String, String> header : response.getHeaders().entrySet()) {
            streamWriter.println(String.format(
                    "%s: %s"
                    , header.getKey()
                    , header.getValue()
            ));
        }

        // send the cookies
        for (HttpCookie cookie : context.SendCookies) {
            streamWriter.println(cookie.getResponseLine());
        }

        // send a blank line signifying "</end headers>"
        streamWriter.println("");
        streamWriter.flush();

        // record the time to first byte
        context.Stats.SendBodyStarted = System.nanoTime();

        // send body
        try {
            response.sendBody(stream);
        }
        catch (Exception ex) {
            logEx(ex, "HttpResponseSendingPump", "sendResponse", "response.sendBody()");
        }

        try {
            stream.flush();
        }
        catch (Exception ex) {
            logEx(ex, "HttpResponseSendingPump", "sendResponse", "final flush");
        }

        context.Stats.SendBodyEnded = System.nanoTime();
    }

    private static class Worker extends Thread {
        HttpResponseSendingPump parentPump;

        Worker(HttpResponseSendingPump parent) {
            this.parentPump = parent;
        }

        @Override
        public void run() {
            while (!parentPump.httpServerInstance.isClosing && !Thread.interrupted()) {
                HttpContext context = null;
                try {
                    Thread.yield();
                    context = parentPump.httpServerInstance.ResponseReady.take();
                    context.ResponseStream = new HttpContext.NoCloseOutputStream(context.Socket.getOutputStream());
                    context.Stats.ResponseSenderStarted = System.nanoTime();
                    sendResponse(context);
                }
                catch (InterruptedException ex) {
                    return;
                }
                catch (Exception ex) {
                    logEx(ex, this.getClass().getName(), "run()", "building a response");
                }
                finally {
                    if (context != null) {
                        context.Stats.ResponseSenderEnded = System.nanoTime();
                        parentPump.httpServerInstance.DoneSending.add(context);
                    }
                }
            }
        }
    }
}
