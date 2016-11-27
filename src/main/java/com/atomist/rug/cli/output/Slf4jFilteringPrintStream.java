package com.atomist.rug.cli.output;

import java.io.OutputStream;
import java.io.PrintStream;

public class Slf4jFilteringPrintStream extends PrintStream {

    public Slf4jFilteringPrintStream(OutputStream out) {
        super(out);
    }

    public void println(String l) {
        if (!l.startsWith("SLF4J")) {
            super.println(l);
        }
    }
}
