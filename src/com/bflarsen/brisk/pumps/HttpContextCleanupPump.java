package com.bflarsen.brisk.pumps;

import com.bflarsen.brisk.HttpContext;
import com.bflarsen.brisk.HttpServer;

import java.net.SocketException;

import static com.bflarsen.util.Logger.*;

public class HttpContextCleanupPump implements Runnable {

    private HttpServer httpServerInstance;

    public HttpContextCleanupPump(HttpServer serverInstance) {
        this.httpServerInstance = serverInstance;
    }

    @Override
    public void run() {
        Thread[] workers = new Thread[httpServerInstance.NumberOfContextCleanupThreadsToCreate];
        // spawn a bunch of workers
        workers[0] = new CatchAllWorker(this);
        workers[0].start();
        for (int i = 1; i < workers.length; i++) {
            workers[i] = new NormalWorker(this);
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

    public static void performCleanup(HttpContext context) {
        if (context == null)
            return;  // TODO: perhaps log this?

        //TODO: what about persistent connections / keep-alive stuff?

        if (context.RequestStream != null) {
            try { context.RequestStream.close(); }
            catch (Exception ex) {}
        }
        if (context.ResponseStream != null) {
            try { context.ResponseStream.close(); }
            catch (Exception ex) {}
        }
        if (context.Socket != null) {
//            if (! context.Socket.isClosed()) {
//                try {
//                    context.Server.IncomingRequests.put(context.Socket);
//                    context.Socket = null;
//                } catch (Exception ex) {}
//            }
//            else {
//                context.Server.logHandler("Socket already closed, no recycling");
//            }
            try { context.Socket.close(); }
            catch (Exception ex) {
                logEx(ex, "HttpContextCleanupPump", "cleanupContext", "context.Socket.close()");
            }
        }

        context.Stats.CompletelyFinished = System.nanoTime();
        context.Stats.totalMs = (context.Stats.CompletelyFinished - context.Stats.RequestParserEnded) / 1000000;

        // logging
        if (context.Request.Resource != null && !context.Request.Resource.isEmpty()) {
            if (context.Stats.totalMs > 5000000000L) { //5 seconds is long!
                logWarning("Super long request/response cycle (" + context.Stats.totalMs + " ms)", "HttpContextCleanupPump", "cleanupContext", String.format("%d", context.Id));
            }
            if (context.Stats.SendBodyEnded == 0 && !(context.ResponderException instanceof SocketException)) {
                logError("No SendBodyEnded", "HttpContextCleanupPump", "cleanupContext", String.format("%d", context.Id));
            }
            context.Server.logRequestResponseCompleted(context);
        }

        // close resources
        if (context.Response != null) {
            try { context.Response.close(); }
            catch (Exception ex) {
                logEx(ex, "HttpContextCleanupPump", "cleanupContext", "context.Response.close()");
            }
        }
    }

    private static class NormalWorker extends Thread {
        HttpContextCleanupPump parentPump;

        NormalWorker(HttpContextCleanupPump parent) {
            this.parentPump = parent;
        }

        @Override
        public void run() {
            while (!parentPump.httpServerInstance.isClosing && !Thread.interrupted()) {
                HttpContext context = null;
                try {
                    Thread.yield();
                    context = parentPump.httpServerInstance.DoneSending.take();
                    context.Stats.CleanupStarted = System.nanoTime();
                    performCleanup(context);
                }
                catch (InterruptedException ex) {
                    return;
                }
                catch (Exception ex) {
                    logEx(ex, this.getClass().getName(), "run()", "building a response");
                }
            }
        }
    }

    private static class CatchAllWorker extends Thread {
        HttpContextCleanupPump parentPump;

        CatchAllWorker(HttpContextCleanupPump parent) {
            this.parentPump = parent;
        }

        @Override
        public void run() {
            while (!parentPump.httpServerInstance.isClosing && !Thread.interrupted()) {
                HttpContext nextContext = null;
                HttpContext context = null;
                long now = System.nanoTime();
                long oldestAllowedStartTime = now - 60000000000L; // 60 seconds
                try {
                    Thread.sleep(100);
                    // flush out everything that has already completed successfully
                    nextContext = parentPump.httpServerInstance.AllRequests.peek();
                    while (nextContext != null
                            && (nextContext.Stats.CompletelyFinished != 0 || nextContext.Stats.RequestParserAborted != 0)
                    ) {
                        context = parentPump.httpServerInstance.AllRequests.take();
                        if (context != nextContext) {
                            logError("catchAll context != nextContext ... that's totally unexpected and means something is broken!", this.getClass().getName(), "run()", "popping a new context from the queue");
                        }
                        nextContext = parentPump.httpServerInstance.AllRequests.peek();
                    }

                    while (nextContext != null
                            && nextContext.Stats.RequestParserStarted > 0
                            && nextContext.Stats.RequestParserStarted < oldestAllowedStartTime
                    ) {
                        context = parentPump.httpServerInstance.AllRequests.take();
                        if (context != nextContext) {
                            logError("catchAll context != nextContext ... that's totally unexpected and means something is broken!", this.getClass().getName(), "run()", "popping a new context from the queue");
                        }
                        context.Stats.Expired = now;
                        parentPump.httpServerInstance.DoneSending.add(context);
                        logError("Response expired before cleanup", this.getClass().getName(), "run()", "");
                        nextContext = parentPump.httpServerInstance.AllRequests.peek();
                    }
                }
                catch (InterruptedException ex) {
                    return;
                }
                catch (Exception ex) {
                    logEx(ex, this.getClass().getName(), "run()", "building a response");
                }
            }
        }
    }
}
