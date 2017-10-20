package com.bflarsen.brisk.pumps;

import com.bflarsen.brisk.*;

import java.util.Map;

import static com.bflarsen.util.Logger.*;

public class WebSocketMessagePump implements Runnable {
    private HttpServer httpServerInstance;

    public WebSocketMessagePump(HttpServer serverInstance) {
        this.httpServerInstance = serverInstance;
    }

    @Override
    public void run() {
        Worker[] workers = new Worker[httpServerInstance.NumberOfWebSocketMessageHandlingThreadsToCreate];
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

    private static class Worker extends Thread {
        WebSocketMessagePump parentPump;
        Map<String, Object> myResources;

        Worker(WebSocketMessagePump parent) {
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
                WebSocketMessage message = null;
                try {
                    Thread.yield();
                    message = parentPump.httpServerInstance.InboundWebSocketMessages.take();
                    message.WorkerThreadResources = myResources;
                    parentPump.httpServerInstance.handleWebSocketMessage(message);

                }
                catch (InterruptedException ex) {
                    return;
                }
                catch (Exception ex) {
                    logEx(ex, this.getClass().getName(), "run()", "building a response");
                }
                finally {
                    try {
                        parentPump.httpServerInstance.resetWorkerThreadResources(myResources);
                    }
                    catch(Exception ex) {
                        logEx(ex, "WebSocketMessagePump", "run()", "resetting WorkerThreadResources");
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
