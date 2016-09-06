package com.bflarsen.brisk.pumps;

import com.bflarsen.brisk.*;
import com.bflarsen.brisk.responders.*;
import static com.bflarsen.util.Logger.*;

import java.util.Map;


public class HttpResponseBuildingPump implements Runnable {

    private HttpServer httpServerInstance;

    public HttpResponseBuildingPump(HttpServer serverInstance) {
        this.httpServerInstance = serverInstance;
    }

    @Override
    public void run() {
        Worker[] workers = new Worker[httpServerInstance.NumberOfResponseBuildingThreadsToCreate];
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

    public static void buildResponse(HttpContext context) throws Exception {
        if (context.Responder == null)
            throw new Exception("Responder is null in buildResponse");

        try {
            context.Response = context.Responder.respond(context);
        }
        catch(Exception ex) {
            logEx(ex, "HttpResponseBuildingPump", "buildResponse", "attempting to build response of type " + context.Responder.getClass().getSimpleName());
            context.ResponderException = ex;
            HttpResponder responder = context.Server.Error500ResponderFactory.create();
            if (responder == null) {
                responder = new DefaultError500Responder();
            }
            try {
                context.Response = responder.respond(context);
            }
            catch (Exception ex2) {
                logEx(ex2, "HttpResponseBuildingPump", "buildResponse", "attempting to build an exception response of type " + context.Responder.getClass().getSimpleName());
                responder = new DefaultError500Responder();
                try {
                    context.Response = responder.respond(context);
                }
                catch (Exception ex3) {
                    // yeah, ... about that.   I've got no more ideas
                    logEx(ex2, "HttpResponseBuildingPump", "buildResponse", "attempting to build the default exception response.");
                }
            }
        }
    }

    private static class Worker extends Thread {
        HttpResponseBuildingPump parentPump;
        Map<String, Object> myResources;

        Worker(HttpResponseBuildingPump parent) {
            parentPump = parent;
            try {
                myResources = parentPump.httpServerInstance.createWorkerThreadResources();
            }
            catch (Exception ex) {
                logEx(ex, "HttpResponseBuildingPump.Worker", "Constructor", "createWorkerThreadResources");
            }
        }

        @Override
        public void run() {
            while (!parentPump.httpServerInstance.isClosing && !Thread.interrupted()) {
                HttpContext context = null;
                try {
                    Thread.yield();
                    context = parentPump.httpServerInstance.RoutedRequests.take();
                    context.Stats.ResponseBuilderStarted = System.nanoTime();
                    context.WorkerThreadResources = myResources;
                    buildResponse(context);
                }
                catch (InterruptedException ex) {
                    return;
                }
                catch (Exception ex) {
                    logEx(ex, this.getClass().getName(), "run()", "building a response");
                }
                finally {
                    if (context != null) {
                        context.WorkerThreadResources = null;
                        context.Stats.ResponseBuilderEnded = System.nanoTime();
                        parentPump.httpServerInstance.ResponseReady.add(context);
                        try {
                            parentPump.httpServerInstance.resetWorkerThreadResources(myResources);
                        }
                        catch(Exception ex) {
                            logEx(ex, "HttpResponseBuildingPump", "run()", "resetting WorkerThreadResources");
                        }
                    }
                }
            }
            try {
                parentPump.httpServerInstance.freeWorkerThreadResources(myResources);
            }
            catch (Exception ex) {
                logEx(ex, "HttpResponseBuildingPump.Worker", "run", "freeWorkerThreadResources");
            }
        }
    }
}
