package com.bflarsen.brisk.pumps;

import com.bflarsen.brisk.*;
import com.bflarsen.brisk.responses.BaseBufferedResponse;

import static com.bflarsen.util.Logger.*;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.SocketException;
import java.util.Map;
import java.util.zip.GZIPOutputStream;


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

        try {
            HttpResponse response = context.Response;
            OutputStream stream = context.ResponseStream;
            PrintWriter streamWriter = new java.io.PrintWriter(stream, true);

            Long length = response.getContentLength();
            byte[] gzipped = null;
            if (length != null && length > 16*1024
                    && context.Request.Headers.containsKey("Request_AcceptEncoding")
                    && context.Request.Headers.get("Request_AcceptEncoding").contains("gzip")
            ) {
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream(length.intValue());
                GZIPOutputStream zipStream = new GZIPOutputStream(byteStream);
                response.sendBody(zipStream);
                zipStream.flush();
                byteStream.flush();
                zipStream.close();
                byteStream.close();
                gzipped = byteStream.toByteArray();
                length = (long)gzipped.length;
                response.setHeader("Content-Encoding", "gzip");
            }
            if (length != null) {
                response.setHeader("Content-Length", length.toString());
            }

            // send the first line ... something like this :  "HTTP/1.1 200 OK"
            streamWriter.print(String.format(
                    "%s %d %s"
                    , response.getHttpVersion()
                    , response.getStatusCode()
                    , response.getStatusDescription()
            ));
            streamWriter.print("\r\n");

            // send all the header lines ... something like this: "Header-Name: header-value"
            for (Map.Entry<String, String> header : response.getHeaders().entrySet()) {
                streamWriter.print(String.format(
                        "%s: %s"
                        , header.getKey()
                        , header.getValue()
                ));
                streamWriter.print("\r\n");
            }

            // send the cookies
            for (HttpCookie cookie : context.SendCookies) {
                streamWriter.print(cookie.getResponseLine());
                streamWriter.print("\r\n");

            }

            // send a blank line signifying "</end headers>"
            streamWriter.print("\r\n");
            streamWriter.flush();

            // record the time to first byte
            context.Stats.SendBodyStarted = System.nanoTime();

            // send body
            try {
                if (gzipped != null) {
                    stream.write(gzipped);
                }
                else {
                    response.sendBody(stream);
                }
            }
            catch (SocketException ex) {
                if (! ex.getMessage().contains("Broken pipe")
                    && ! ex.getMessage().contains("Connection reset")
                    && ! ex.getMessage().contains("Connection closed by remote host")
                ) {
                    logEx(ex, "HttpResponseSendingPump", "sendResponse", "response.sendBody()");
                }
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
        }
        catch (java.net.SocketException ex) {
            // so you had a socket error ... so what
        }
        finally {
            context.Stats.SendBodyEnded = System.nanoTime();
        }
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
                catch (SocketException ex) {
                    context.ResponderException = ex;
                    context.Stats.CompletelyFinished = System.nanoTime();
                    logWarning("Failed to send response because socket was closed.", this.getClass().getName(), "run()", "sending a response");
                }
                catch (Exception ex) {
                    logEx(ex, this.getClass().getName(), "run()", "sending a response");
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
