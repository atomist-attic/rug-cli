package com.atomist.rug.cli.output;

import java.io.PrintStream;

import com.atomist.rug.cli.RunnerException;
import com.atomist.rug.cli.utils.CommandLineOptions;
import com.atomist.rug.cli.utils.Timing;

public class ProgressReportingOperationRunner<T> {

    private String msg = null;

    public ProgressReportingOperationRunner(String msg) {
        this.msg = msg;
    }

    public T run(ProgressReportingOperation<T> operation) throws RunnerException {
        Timing timing = new Timing();
        boolean success = true;
        ProgressReporter indicator = createProgressReporter();
        try {
            return operation.run(indicator);
        }
        catch (Throwable e) {
            success = false;
            if (e instanceof RunnerException) {
                throw (RunnerException) e;
            }
            throw new RunnerException(e);
        }
        finally {
            if (CommandLineOptions.hasOption("t")) {
                indicator.finish(success, timing.duration());
            }
            else {
                indicator.finish(success, -1);
            }
            ProgressReporterUtils.removeActiveProgressReporter();
        }
    }

    private ProgressReporter createProgressReporter() {
        ProgressReporter indicator = null;
        PrintStream stream = (CommandLineOptions.hasOption("output") ? System.err : System.out);
        if (CommandLineOptions.hasOption("output") || CommandLineOptions.hasOption("q")
                || (msg.length()) >= ConsoleUtils.width()) {
            indicator = new PassThroughProgressReporter(msg, stream);
        }
        else {
            indicator = new SpinningProgressReporter(msg, stream);
        }
        ProgressReporterUtils.setActiveProgressReporter(indicator);
        return indicator;
    }

    public interface ProgressReportingOperation<T> {
        T run(ProgressReporter indicator) throws Exception;
    }
}
