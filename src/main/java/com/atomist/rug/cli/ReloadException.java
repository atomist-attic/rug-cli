package com.atomist.rug.cli;

/**
 * Exception raise to signal that the current CLI session should fall back all the way to
 * {@link Main} and be reloaded with provided {@link #args} again.
 */
public class ReloadException extends RuntimeException {

    private static final long serialVersionUID = -7477818216340143932L;

    private String[] args;

    public ReloadException(String[] args) {
        this.args = args;
    }

    public String[] args() {
        return args;
    }
}
