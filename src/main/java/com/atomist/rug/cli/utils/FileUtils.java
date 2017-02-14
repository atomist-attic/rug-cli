package com.atomist.rug.cli.utils;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.atomist.rug.cli.Constants;

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
        if (file == null) {
            return;
        }
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
    
    public static Optional<File> getWorkingDirectory() {
        return getWorkingDirectory(System.getProperty("user.dir"));
    }
    
    public static Optional<File> getWorkingDirectory(String root) {
        if (root == null) {
            root = System.getProperty("user.dir");
        }
        File projectRoot = searchFromProjectRoot(new File(root));
        return Optional.ofNullable(projectRoot);
    }

    private static File searchFromProjectRoot(File root) {
        // inside project root with a child .atomist
        File dir = new File(root, Constants.ATOMIST_ROOT);
        if (dir.exists()) {
            return root;
        }
        // inside .atomist folder
        if (root.getName().equals(Constants.ATOMIST_ROOT)) {
            return root.getParentFile();
        }
        // inside any sub-folder of .atomist
        if (root.getParentFile().getName().equals(Constants.ATOMIST_ROOT)) {
            return root.getParentFile().getParentFile();
        }
        return null;
    }

}
