package com.atomist.rug.cli.output;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.Log;

import scala.runtime.AbstractFunction1;
import scala.runtime.BoxedUnit;

public class ConsoleLogger {
    
    private static Log log = new Log(ConsoleLogger.class);

    private String indent = Constants.LEFT_PADDING;
    
    private ConsoleLogger(String indent) {
        this.indent = indent;
    }
    
    public void log(String msg) {
        log.info(indent + msg);
    }
    
    public void warn(String msg) {
        log.info(indent + msg);
    }
    
    public void error(String msg) {
        log.error(indent + msg);
    }
    
    public static AbstractFunction1<ScriptEngine, BoxedUnit> consoleLogger(String indent) {
        return new AbstractFunction1<ScriptEngine, BoxedUnit>() {

            @Override
            public BoxedUnit apply(ScriptEngine engine) {
                Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
                bindings.put("console", new ConsoleLogger(indent));
                engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
                return null;
            }
        };
    }
    
    public static AbstractFunction1<ScriptEngine, BoxedUnit> consoleLogger() {
        return consoleLogger(Constants.LEFT_PADDING);
    }
 }