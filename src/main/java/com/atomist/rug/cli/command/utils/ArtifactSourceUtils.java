package com.atomist.rug.cli.command.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

import com.atomist.rug.cli.RunnerException;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.source.ArtifactSource;
import com.atomist.source.DirectoryArtifact;
import com.atomist.source.FileArtifact;
import com.atomist.source.file.FileSystemArtifactSource;
import com.atomist.source.file.SimpleFileSystemArtifactSourceIdentifier;
import com.atomist.source.file.ZipFileArtifactSourceReader;
import com.atomist.source.file.ZipFileInput;

import scala.runtime.AbstractFunction1;

public abstract class ArtifactSourceUtils {

    public static final List<String> DIRS_TO_FILTER = Arrays.asList(".git");
    public static final List<String> FILES_TO_FILTER = Arrays
            .asList(".DS_Store", ".travis.yml", "travis-build.bash");

    public static ArtifactSource filter(ArtifactSource source) {
        return source.filter(new AbstractFunction1<DirectoryArtifact, Object>() {

            @Override
            public Object apply(DirectoryArtifact dir) {
                return !ArtifactSourceUtils.DIRS_TO_FILTER
                        .contains(dir.name());
            }
        }, new AbstractFunction1<FileArtifact, Object>() {

            @Override
            public Object apply(FileArtifact file) {
                return !ArtifactSourceUtils.FILES_TO_FILTER
                        .contains(file.name());
            }
        });
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
                        new SimpleFileSystemArtifactSourceIdentifier(archiveRoot));
            }
        }
        catch (FileNotFoundException e) {
            throw new RunnerException(e);
        }
    }

}
