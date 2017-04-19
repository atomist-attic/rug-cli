package com.atomist.rug.cli.output;

public interface ProgressReporter {

    void finish(boolean success, float duration);

    void report(String message);
    
    void detail(String detail);

}