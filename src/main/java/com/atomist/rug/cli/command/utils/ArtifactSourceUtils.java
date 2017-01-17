package com.atomist.rug.cli.command.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;

import com.atomist.rug.cli.RunnerException;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.source.ArtifactSource;
import com.atomist.source.file.FileSystemArtifactSource;
import com.atomist.source.file.SimpleFileSystemArtifactSourceIdentifier;
import com.atomist.source.file.ZipFileArtifactSourceReader;
import com.atomist.source.file.ZipFileInput;
import com.atomist.source.filter.ArtifactFilter;
import com.atomist.source.filter.AtomistIgnoreFileFilter;
import com.atomist.source.filter.GitDirFilter;

public abstract class ArtifactSourceUtils {

    public static ArtifactSource createArtifactSource(File root) {
        return new FileSystemArtifactSource(new SimpleFileSystemArtifactSourceIdentifier(root),
                Arrays.asList(new GitDirFilter(root.getPath())));
    }

    public static ArtifactSource createArtifactSource(ArtifactDescriptor artifact) {
        try {
            File archiveRoot = new File(artifact.uri());
            if (archiveRoot.isFile()) {
                return ZipFileArtifactSourceReader
                        .fromZipSource(new ZipFileInput(new FileInputStream(archiveRoot)));
            }
            else {
                return new FileSystemArtifactSource(
                        new SimpleFileSystemArtifactSourceIdentifier(archiveRoot),
                        Arrays.asList(new GitDirFilter(archiveRoot.getPath()),
                                new AtomistIgnoreFileFilter(archiveRoot.getPath()),
                                new TargetDirFilter(archiveRoot)));
            }
        }
        catch (FileNotFoundException e) {
            throw new RunnerException(e);
        }
    }

    private static class TargetDirFilter implements ArtifactFilter {

        private String prefix;

        public TargetDirFilter(File root) {
            this.prefix = root.getAbsolutePath() + File.separator + ".atomist" + File.separator
                    + "target";
        }

        @Override
        public boolean apply(String path) {
            return !path.startsWith(prefix);
        }

    }
}
