package com.atomist.rug.cli.version;

import java.util.Optional;

public class VersionThread extends Thread {

    private Optional<String> version = Optional.empty();

    public VersionThread() {
        setDaemon(true);
        start();
    }

    public Optional<String> getVersion() {
        return version;
    }

    @Override
    public void run() {
        version = VersionUtils.newerVersion();
    }
}
