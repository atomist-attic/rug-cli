package com.atomist.rug.cli.settings;

public class SettingsException extends RuntimeException {

    private static final long serialVersionUID = -6851546296579467811L;

    public SettingsException(String msg, Exception e) {
        super(msg, e);
    }
}
