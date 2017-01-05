package com.atomist.rug.cli.command;

import java.net.URI;

public interface Command {

    void run(String... args);

    void run(String group, String artifact, String version, String extension, boolean local,
            URI[] files, String... args);

}
