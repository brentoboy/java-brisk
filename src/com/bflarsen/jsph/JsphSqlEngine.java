package com.bflarsen.jsph;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.TimeZone;
import java.util.stream.Collectors;


public class JsphSqlEngine {
    static public String escape(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (Number.class.isAssignableFrom(value.getClass())) {
            return value.toString();
        }
        if (Boolean.class.isAssignableFrom(value.getClass())) {
            return (boolean)value ? "1" : "0";
        }
        if (jdk.nashorn.api.scripting.ScriptObjectMirror.class.isAssignableFrom(value.getClass())) {
            value = jdk.nashorn.api.scripting.ScriptUtils.convert(value, Object[].class);
        }
        if (value.getClass().isArray()) {
            return Arrays.stream((Object[])value)
                    .distinct()
                    .map(x -> escape(x))
                    .collect(Collectors.joining(","))
            ;
        }
        // else
        return "'" + value.toString().replace("'", "''") + "'";
    }
    public static String escapeDate(Object val) {
        if (val == null) {
            return "NULL";
        }
        if (Number.class.isAssignableFrom(val.getClass())) {
            Long value = ((Number)val).longValue();
            if (value < 100000000000L) { // this is in sec not millis
                value = value * 1000;
            }
            java.util.Date dt = new java.util.Date(value);
            java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            return escape(formatter.format(dt));
        }
        // else
        return "null";
    }
}
