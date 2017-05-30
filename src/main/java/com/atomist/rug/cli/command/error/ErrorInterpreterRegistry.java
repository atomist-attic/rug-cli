package com.atomist.rug.cli.command.error;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

public class ErrorInterpreterRegistry {
    
    private List<ErrorInterpreter> interpreters = new ArrayList<>();
    
    public ErrorInterpreterRegistry() {
        init();
    }
    
    private void init() {
        ServiceLoader<ErrorInterpreter> loader = ServiceLoader.load(ErrorInterpreter.class);
        loader.forEach(i -> interpreters.add(i));
        interpreters = interpreters.stream().sorted(Comparator.comparingInt(ErrorInterpreter::order))
                .collect(toList());
    }
    
    public String interpret(Throwable e) {
        String msg = e.getMessage();
        return interpreters.stream().filter(i -> i.supports(msg)).findFirst().get().interpret(msg);
    }
}