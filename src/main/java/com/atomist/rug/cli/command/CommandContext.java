package com.atomist.rug.cli.command;

import java.util.HashMap;
import java.util.Map;

public abstract class CommandContext {

    private static Map<Class<?>, Object> context = new HashMap<>();

    public static void save(Class<?> key, Object value) {
        context.put(key, value);
    }

    public static boolean contains(Class<?> key) {
        return context.containsKey(key);
    }

    public static boolean delete(Class<?> key) {
        return context.remove(key) != null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T restore(Class<T> key) {
        return (T) context.get(key);
    }
}
