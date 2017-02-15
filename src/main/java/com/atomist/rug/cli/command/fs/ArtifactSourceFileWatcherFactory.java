package com.atomist.rug.cli.command.fs;

import org.springframework.util.ClassUtils;

import com.atomist.rug.resolver.ArtifactDescriptor;

/**
 * Creates an instance of {@link ArtifactSourceFileWatcherThread} with using some internal SUN
 * classes if they are available.
 */
public abstract class ArtifactSourceFileWatcherFactory {

    private static final boolean SUN_CLASS_AVAILALBE = ClassUtils.isPresent(
            "com.sun.nio.file.SensitivityWatchEventModifier",
            Thread.currentThread().getContextClassLoader());

    public static void create(ArtifactDescriptor artifact) {
        if (SUN_CLASS_AVAILALBE) {
            new ArtifactSourceFileWatcherWithSunCreator().create(artifact);
        }
        else {
            new ArtifactSourceFileWatcherWithoutSunCreator().create(artifact);
        }
    }

    private static class ArtifactSourceFileWatcherWithSunCreator {

        public void create(ArtifactDescriptor artifact) {
            new ArtifactSourceFileWatcherThread(artifact,
                    com.sun.nio.file.SensitivityWatchEventModifier.HIGH);
        }

    }

    private static class ArtifactSourceFileWatcherWithoutSunCreator {

        public void create(ArtifactDescriptor artifact) {
            new ArtifactSourceFileWatcherThread(artifact);
        }
    }

}
