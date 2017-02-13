package com.atomist.rug.cli.command;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class CommandEventListenerRegistry {
    
    private static List<CommandEventListener> listeners = new ArrayList<>();
    
    public static void register(CommandEventListener listener) {
        listeners.add(listener);
    }
    
    public static void raiseEvent(Consumer<CommandEventListener> consumer) {
        listeners.forEach(l -> consumer.accept(l));
    }

}
