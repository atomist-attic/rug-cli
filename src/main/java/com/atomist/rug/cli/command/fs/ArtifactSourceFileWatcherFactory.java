package com.atomist.rug.cli.command.fs;

import com.atomist.rug.resolver.ArtifactDescriptor;
import org.springframework.util.ClassUtils;

/**
 * Creates an instance of {@link ArtifactSourceFileWatcherThread} with using some internal SUN
 * classes if they are available.
 */
public abstract class ArtifactSourceFileWatcherFactory {

    private static final boolean SUN_CLASS_AVAILALBE = ClassUtils.isPresent(
            "com.sun.nio.file.SensitivityWatchEventModifier",
            Thread.currentThread().getContextClassLoader());

    public static FileWatcher create(ArtifactDescriptor artifact) {
        if (SUN_CLASS_AVAILALBE) {
            return new ArtifactSourceFileWatcherWithSunCreator().create(artifact);
        }
        else {
            return new ArtifactSourceFileWatcherWithoutSunCreator().create(artifact);
        }
    }

    private static class ArtifactSourceFileWatcherWithSunCreator {

        public FileWatcher create(ArtifactDescriptor artifact) {
            return new ArtifactSourceFileWatcherThread(artifact,
                    com.sun.nio.file.SensitivityWatchEventModifier.HIGH);
        }

    }

    private static class ArtifactSourceFileWatcherWithoutSunCreator {

        public FileWatcher create(ArtifactDescriptor artifact) {
            return new ArtifactSourceFileWatcherThread(artifact);
        }
    }

    public interface FileWatcher {

        void shutdown();

    }

}
