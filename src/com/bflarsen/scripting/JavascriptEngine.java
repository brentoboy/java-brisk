package com.bflarsen.scripting;


import jdk.nashorn.api.scripting.JSObject;

import javax.script.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedTransferQueue;

public class JavascriptEngine implements Runnable, AutoCloseable {

    public interface TaskFunction {
        Object f() throws Exception;
    }

    public static class Task {
        public Object result;
        public Exception exception;
        TaskFunction taskFunction;
        CountDownLatch waiter;

        public Task(TaskFunction doThis) {
            waiter = new CountDownLatch(1);
            taskFunction = doThis;
        }

        public void runFunction() {
            try {
                result = taskFunction.f();
            }
            catch (Exception ex) {
                exception = ex;
            }
            waiter.countDown();
        }

        public Object waitForResult() throws Exception {
            waiter.await(); // allow the script engine thread to do his business
            if (exception != null) {
                throw exception;
            }
            return result;
        }
    }

    private final LinkedTransferQueue<Task> taskQueue;
    private ScriptEngine engine;
    private final Thread engineThread;
    private final Task stopTask;
    private boolean isRunning;

    public JavascriptEngine() throws Exception {
        this(null);
    }
    public JavascriptEngine(Object gson) throws Exception {
        stopTask = new Task(null);
        taskQueue = new LinkedTransferQueue<>();
        engineThread = new Thread(this);
        engineThread.start();
        eval( // nashorn-polyfill
                "var global = global || this;\n" +
                "var window = window || global;\n" +
                "var process = process || {env:{}};\n" +
                "var console = console || { debug: print, log: print, warn: print, error: print };\n" +
                "function isNull(value) {\n" +
                "    return value === null || value === undefined;\n" +
                "}\n" +
                "function coalesce() {\n" +
                "    var len = arguments.length;\n" +
                "    for (var i=0; i<len; i++) {\n" +
                "        if (!isNull(arguments[i])) {\n" +
                "            return arguments[i];\n" +
                "        }\n" +
                "    }\n" +
                "    return null;\n" +
                "}"
        );
        if (gson != null) {
            this.setValue("GSON", gson);
            this.eval(
                    "JSON.originalStringify = JSON.stringify;" +
                    "JSON.stringify = function replacementStringify(obj) {" +
                    "  var result = JSON.originalStringify(obj);" +
                    "  if (result == undefined) return GSON.toJson(obj);" +
                    "  else return result;" +
                    "}"
            );
        }
    }

    @Override
    public void close() throws Exception {
        taskQueue.add(stopTask);
        engineThread.join();
    }

    @Override
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
                if (task == stopTask) {
                    isRunning = false;
                    break;
                }
                try {
                    task.runFunction();
                }
                catch(Exception ex) {
                    task.exception = ex;
                    System.out.println(ex);
                }
            }
            catch (Exception ex) {
                // hmm
                System.out.println(ex);
            }
        }
        isRunning = false;
    }

    public Object eval(final String jsCode) throws Exception {
        Task task = new Task(
                () -> {
                    return engine.eval(jsCode);
                }
        );
        taskQueue.add(task);
        return task.waitForResult();
    }


    public Object eval(final Reader reader) throws Exception {
        Task task = new Task(
                () -> {
                    return engine.eval(reader);
                }
        );
        taskQueue.add(task);
        return task.waitForResult();
    }

    public void evalResourceAsync(final String resourcePath) throws Exception {
        Task task = new Task(
                () -> {
                    try (InputStream resource = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                        try (Reader reader = new InputStreamReader(resource)) {
                            return engine.eval(reader);
                        }
                    }
                }
        );
        taskQueue.add(task);
    }

    public void compile(final String jsCode) throws Exception {
        Task task = new Task(
                () -> {
                    return ((Compilable) engine).compile(jsCode);
                }
        );
        taskQueue.add(task);
        task.waitForResult();
    }

    public void setValue(String jsVariableName, Object value) throws Exception {
        Task task = new Task(
                () -> {
                    return engine.getBindings(ScriptContext.ENGINE_SCOPE).put(jsVariableName, value);
                }
        );
        taskQueue.add(task);
        task.waitForResult();
    }

    public Object getValue(String jsVariableName) throws Exception {
        Task task = new Task(
                () -> {
                    return engine.getBindings(ScriptContext.ENGINE_SCOPE).get(jsVariableName);
                }
        );
        taskQueue.add(task);
        return task.waitForResult();
    }

    public Object getProperty(Object jsObject, String propName) throws Exception {
        Task task = new Task(
                () -> {
                    return ((JSObject)jsObject).getMember(propName);
                }
        );
        taskQueue.add(task);
        return task.waitForResult();
    }

    public void setProperty(Object jsObject, String propName, Object value) throws Exception {
        Task task = new Task(
                () -> {
                    ((JSObject)jsObject).setMember(propName, value);
                    return null;
                }
        );
        taskQueue.add(task);
        task.waitForResult();
    }

    public Object runMethod(String jsObjectName, String methodName, Object... params) throws Exception {
        Task task = new Task(
                () -> {
                    return ((Invocable) engine).invokeMethod(engine.get(jsObjectName), methodName, params);
                }
        );
        taskQueue.add(task);
        return task.waitForResult();
    }

    public Object runFunction(String fnName, Object... params) throws Exception {
        Task task = new Task(
                () -> {
                    return ((Invocable) engine).invokeFunction(fnName, params);
                }
        );
        taskQueue.add(task);
        return task.waitForResult();
    }
}
