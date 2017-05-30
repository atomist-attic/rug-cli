package com.atomist.rug.cli.command.error;

import java.util.ServiceLoader;

/**
 * This interface can be implemented in order to provide post-processing of error messages coming
 * out of command execution. 
 * <p>
 * Implementations should be registered using the {@link ServiceLoader} file at 
 * <code>META-INF/services/com.atomist.rug.cli.command.error.ErrorInterpreter</code>. 
 */
public interface ErrorInterpreter {
    
    boolean supports(String e);
    
    String interpret(String e);
    
    default int order() {
        return 0;
    }
}
