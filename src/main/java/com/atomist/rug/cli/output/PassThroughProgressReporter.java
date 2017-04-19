package com.atomist.rug.cli.output;

import com.atomist.rug.cli.Log;

public class PassThroughProgressReporter implements ProgressReporter {

    private Log log = new Log(PassThroughProgressReporter.class);

    private String message;

    public PassThroughProgressReporter(String message) {
        this.message = message;
        report(message);
    }

    @Override
    public void finish(boolean success, float duration) {
        log.info(message + Style.green(" completed")
                + (duration > -1 ? " in " + String.format("%.2f", duration) + "s" : ""));
    }

    @Override
    public void report(String message) {
        log.info(message.replace("\t", "  "));
    }

    @Override
    public void detail(String detail) {
        // no op
    }
}
