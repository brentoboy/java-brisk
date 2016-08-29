package com.bflarsen.util.exceptions;

public class ValueNotConvertableException extends RuntimeException {
    public ValueNotConvertableException(Class<?> targetClass, Object value, String... additionalDetails) {
        super(String.format("Unable to convert '%s' to instance of '%s' %s", value, targetClass.getName(), String.join(" ", additionalDetails)));
    }
}
