package com.atomist.rug.cli.command.utils;

import java.util.Iterator;

import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;

public class ParseExceptionProcessor {

    @SuppressWarnings("unchecked")
    public static String process(ParseException e) {
        if (e instanceof MissingOptionException) {
            StringBuilder sb = new StringBuilder();
            Iterator<String> options = ((MissingOptionException) e).getMissingOptions().iterator();
            while (options.hasNext()) {
                sb.append(options.next());
                if (options.hasNext()) {
                    sb.append(", ");
                }
            }
            return String.format("Missing required option(s) %s.", sb.toString());
        }
        else if (e instanceof MissingArgumentException) {
            return String.format("%s is missing a required argument.",
                    ((MissingArgumentException) e).getOption());
        }
        else if (e instanceof UnrecognizedOptionException) {
            return String.format("%s is not a valid option.",
                    ((UnrecognizedOptionException) e).getOption());
        }
        else {
            return String.format("%s.", e.getMessage());
        }
    }

}
