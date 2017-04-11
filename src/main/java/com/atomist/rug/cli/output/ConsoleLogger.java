package com.atomist.rug.cli.output;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import com.atomist.rug.cli.Log;

import scala.runtime.AbstractFunction1;
import scala.runtime.BoxedUnit;

public class ConsoleLogger {
    
    private Log log = new Log(ConsoleLogger.class);
    
    public void log(String msg) {
        log.info(msg);
    }
    
    public void warn(String msg) {
        log.info(msg);
    }
    
    public void error(String msg) {
        log.error(msg);
    }
    
    public static AbstractFunction1<ScriptEngine, BoxedUnit> consoleLogger() {
        return new AbstractFunction1<ScriptEngine, BoxedUnit>() {

            @Override
            public BoxedUnit apply(ScriptEngine engine) {
                Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
                bindings.put("console", new ConsoleLogger());
                engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
                return null;
            }
        };
    }
 }