package com.bflarsen.util;

public class Logger {

    public interface Interface {
        void info(String message, String className, String functionName, String whileDoing);
        void error(String message, String className, String functionName, String whileDoing);
        void warning(String message, String className, String functionName, String whileDoing);
        void trace(String message, String className, String functionName, String whileDoing);
        void except(Exception ex, String className, String functionName, String whileDoing);
    }

    public static Interface Instance = new Logger.Interface(){

        @Override
        public void info(String message, String className, String functionName, String whileDoing) {

        }

        @Override
        public void error(String message, String className, String functionName, String whileDoing) {

        }

        @Override
        public void warning(String message, String className, String functionName, String whileDoing) {

        }

        @Override
        public void trace(String message, String className, String functionName, String whileDoing) {

        }

        @Override
        public void except(Exception ex, String className, String functionName, String whileDoing) {

        }
    };

    public static void logInfo(String message, String className, String functionName, String whileDoing) {
        Instance.info(message, className, functionName, whileDoing);
    }

    public static void logError(String message, String className, String functionName, String whileDoing) {
        Instance.error(message, className, functionName, whileDoing);
    }

    public static void logWarning(String message, String className, String functionName, String whileDoing) {
        Instance.warning(message, className, functionName, whileDoing);
    }

    public static void logTrace(String message, String className, String functionName, String whileDoing) {
        Instance.trace(message, className, functionName, whileDoing);
    }

    public static void logEx(Exception ex, String className, String functionName, String whileDoing) {
        Instance.except(ex, className, functionName, whileDoing);
    }
}
