package com.atomist.rug.cli.output;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

public class ProgressReportingPrintStream extends Slf4jFilteringPrintStream {

    public ProgressReportingPrintStream(OutputStream out) {
        super(out);
    }

    public void print(String l) {
        Optional<ProgressReporter> progressReporterOption = ProgressReporterUtils
                .getActiveProgressReporter();
        if (progressReporterOption.isPresent() && !l.startsWith("$")) {
            progressReporterOption.get().report("$  " + l);
        }
        else if (l.startsWith("$")) {
            super.print(l.substring(1));
        }
        else {
            super.print(l);
        }
    }

    public void println(String l) {
        Optional<ProgressReporter> progressReporterOption = ProgressReporterUtils
                .getActiveProgressReporter();
        if (progressReporterOption.isPresent() && !l.startsWith("$")) {
            progressReporterOption.get().report("$  " + l);
        }
        else if (l.startsWith("$")) {
            super.println(l.substring(1));
        }
        else {
            super.println(l);
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        String l = new String(b);
        Optional<ProgressReporter> progressReporterOption = ProgressReporterUtils
                .getActiveProgressReporter();
        if (progressReporterOption.isPresent() && !l.startsWith("$")) {
            progressReporterOption.get().report("$  " + l);
        }
        else if (l.startsWith("$")) {
            super.write(l.substring(1).getBytes());
        }
        else {
            super.write(b);
        }
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        String l = new String(buf, off, len);
        Optional<ProgressReporter> progressReporterOption = ProgressReporterUtils
                .getActiveProgressReporter();
        if (progressReporterOption.isPresent() && !l.startsWith("$")) {
            progressReporterOption.get().report("$  " + l);
        }
        else if (l.startsWith("$")) {
            buf = l.substring(1).getBytes();
            super.write(buf, off, buf.length);
        }
        else {
            super.write(buf, off, len);
        }
    }
}
