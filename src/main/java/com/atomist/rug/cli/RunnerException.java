package com.atomist.rug.cli;

public class RunnerException extends RuntimeException {

    private static final long serialVersionUID = -5635857936975533282L;

    public RunnerException(String msg, Throwable e) {
        super(msg, e);
    }

    public RunnerException(Throwable e) {
        super(e.getMessage(), e);
    }
}
