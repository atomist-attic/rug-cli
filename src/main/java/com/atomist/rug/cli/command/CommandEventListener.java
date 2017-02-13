package com.atomist.rug.cli.command;

import com.atomist.rug.loader.OperationsAndHandlers;

public interface CommandEventListener {

    void operationsLoaded(OperationsAndHandlers operations);
    
}
