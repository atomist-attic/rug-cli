package com.atomist.rug.cli.utils;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

public abstract class FileUtils {

    public static File createProjectRoot(String path) {
        if (path == null) {
            path = System.getProperty("user.dir");
        }
        if (path.startsWith("~")) {
            path = path.replace("~", "${user.home}");
        }
        path = StringUtils.expandEnvironmentVars(path);
        return new File(path);
    }

    public static void setPermissionsToOwnerOnly(File file) {
        try {
            Set<PosixFilePermission> perms = new HashSet<>();
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(file.toPath(), perms);
        }
        catch (Exception e) {
            // On some file systems we might get an exception attempting to set permissions
            // Just ignore it
        }
    }

    public static String sizeOf(URI uri) {
        return sizeOf(new File(uri));
    }

    public static String sizeOf(File file) {
        return org.apache.commons.io.FileUtils
                .byteCountToDisplaySize(org.apache.commons.io.FileUtils.sizeOf(file)).toLowerCase();
    }

    public static String relativize(URI uri) {
        return relativize(new File(uri));
    }

    public static String relativize(File file) {
        URI userHome = new File(System.getProperty("user.home")).toURI();
        String path = (file.toURI().toString().startsWith(userHome.toString())
                ? "~" + File.separator + userHome.relativize(file.toURI()).toString()
                : file.getAbsolutePath().toString());
        return path;
    }
}
