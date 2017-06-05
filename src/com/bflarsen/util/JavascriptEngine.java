package com.bflarsen.util;


import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedTransferQueue;

public class JavascriptEngine implements Runnable {

    public interface TaskFunction {
        Object f() throws ScriptException;
    }
    public static class Task {
        public Object result;
        public ScriptException exception;
        TaskFunction taskFunction;
        CountDownLatch waiter;

        public Task(TaskFunction doThis) {
            taskFunction = doThis;
            waiter = new CountDownLatch(1);
        }
        public void runFunction() {
            try {
                result = taskFunction.f();
            }
            catch (ScriptException ex) {
                exception = ex;
            }
            waiter.countDown();
        }
    }

    private LinkedTransferQueue<Task> taskQueue;
    private ScriptEngine engine;
    private Thread engineThread;
    private Task stopEngine;
    private boolean isRunning;

    public JavascriptEngine() {
        stopEngine = new Task(() -> null);
        taskQueue = new LinkedTransferQueue<>();
    }

    public void start() {
        engineThread = new Thread(this);
        engineThread.start();
    }

    public void stop() throws InterruptedException {
        taskQueue.add(stopEngine);
        engineThread.join();
    }

    public void run() {
        engine = new ScriptEngineManager().getEngineByName("nashorn");
        isRunning = true;
        while (isRunning) {
            try {
                Task task;
                try {
                    task = taskQueue.take();
                }
                catch (java.lang.InterruptedException ex) {
                    isRunning = false;
                    break;
                }
                if (task == stopEngine) {
                    isRunning = false;
                    break;
                }
                task.runFunction();
            }
            catch (Exception ex) {
                // hmm
            }
        }
        isRunning = false;
    }

    public Object eval(final String jsCode) throws Exception {
        Task task = new Task(() -> engine.eval(jsCode));

        taskQueue.add(task);
        task.wait();
        if (task.exception != null) {
            throw task.exception;
        }
        return task.result;
    }

    public void evalFile(final String path) throws Exception {

    }
}
