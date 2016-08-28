package com.bflarsen.util;

import com.bflarsen.util.exceptions.AutoConverterNotRegisteredException;
import com.bflarsen.util.exceptions.ValueNotConvertableException;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static com.bflarsen.util.Logger.*;

public class AutoConvert  {

    public interface IConverter {
        Object convert(Object value) throws Exception;
    }

    private final Map<String, IConverter> converters;

    public AutoConvert() {
        converters = new HashMap<>();
        addConverter(String.class, Long.class, AutoConvert::String_To_Long);
        addConverter(String.class, Integer.class, AutoConvert::String_To_Integer);
        addConverter(String.class, Boolean.class, AutoConvert::String_To_Boolean);
        addConverter(Integer.class, Boolean.class, AutoConvert::Number_To_Boolean);
        addConverter(Long.class, Boolean.class, AutoConvert::Number_To_Boolean);
        addConverter(Integer.class, Long.class, AutoConvert::Number_To_Long);
        addConverter(Long.class, Integer.class, AutoConvert::Number_To_Integer);
    }

    public void fill(Object target, Map<String, Object> values) throws Exception {
        try {
            Class cls = target.getClass();
            for (Field field : cls.getFields()) {
                String fieldName = field.getName();
                if (values.containsKey(fieldName)) {
                    Object value = values.get(fieldName);
                    field.set(target, convert(value, field.getType()));
                }
            }
        }
        catch (Exception ex) {
            logEx(ex, this.getClass().getName(), "fill()", "");
        }
    }

    public <T> T convert(Object value, Class<T> targetClass) throws Exception {
        try {
            if (value == null) {
                return null;
            }

            Class sourceClass = value.getClass();
            if (targetClass == value.getClass()) {
                return (T) value;
            }

            String targetClassName = targetClass.getName();
            String sourceClassName = sourceClass.getName();
            String autoConvertKey = sourceClassName + " to " + targetClassName;
            if (converters.containsKey(autoConvertKey)) {
                return (T) converters.get(autoConvertKey).convert(value);
            } else if (targetClass == String.class) {
                return (T) value.toString();
            } else {
                throw new AutoConverterNotRegisteredException(targetClass, sourceClass);
            }
        }
        catch (Exception ex) {
            logEx(ex, this.getClass().getName(), "convert()", value.getClass().getName() + " to " + targetClass.getName());
            return null;
        }
    }

    public void addConverter(Class<?> fromClass, Class<?> toClass, IConverter converter) {
        converters.put(fromClass.getName() + " to " + toClass.getName(), converter);
    }

    public static Object String_To_Long(Object value) throws Exception {
        try {
            return Long.parseLong((String) value);
        }
        catch (Exception ex) {
            throw new ValueNotConvertableException(Long.class, value, "in 'AutoConvert.String_To_Long'", ex.toString());
        }
    }

    public static Object Number_To_Long(Object value) throws Exception {
        try {
            return ((Number)value).longValue();
        }
        catch (Exception ex) {
            throw new ValueNotConvertableException(Long.class, value, "in 'AutoConvert.Number_To_Long'", ex.toString());
        }
    }

    public static Object String_To_Integer(Object value) throws Exception {
        try {
            return Integer.parseInt((String)value);
        }
        catch (Exception ex) {
            throw new ValueNotConvertableException(Integer.class, value, "in 'AutoConvert.String_To_Integer'", ex.toString());
        }
    }

    public static Object Number_To_Integer(Object value) throws Exception {
        try {
            return ((Number)value).intValue();
        }
        catch (Exception ex) {
            throw new ValueNotConvertableException(Integer.class, value, "in 'AutoConvert.Number_To_Integer'", ex.toString());
        }
    }

    public static Object String_To_Boolean(Object value) throws Exception {
        switch ((String)value) {
            case "true":
            case "TRUE":
            case "True":
            case "1":
                return true;
            case "false":
            case "False":
            case "FALSE":
            case "0":
            case "":
                return false;
            default:
                throw new ValueNotConvertableException(Boolean.class, value, "in 'AutoConvert.String_To_Boolean'");
        }
    }

    public static Object Number_To_Boolean(Object value) throws Exception {
        try {
            return value.equals(1);
        }
        catch (Exception ex) {
            throw new ValueNotConvertableException(Boolean.class, value, "in 'AutoConvert.Number_To_Boolean'", ex.toString());
        }
    }
}