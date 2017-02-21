package com.atomist.rug.cli.command.utils;

public class OperationUtils {

    public static String extractRugTypeName(String name) {
        if (name != null) {
            String[] parts = name.split(":");
            if (parts.length > 1) {
                return parts[parts.length - 1];
            }
        }
        return name;
    }

}
