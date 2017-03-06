package com.atomist.rug.cli.output;

import java.io.OutputStream;
import java.io.PrintStream;

public class FilteringPrintStream extends PrintStream {

    public FilteringPrintStream(OutputStream out) {
        super(out);
    }

    public void println(String l) {
        if (l != null && !l.startsWith("SLF4J") && !l.contains("INFO: Created user preferences directory.")) {
            super.println(l);
        }
    }
}
