package com.atomist.rug.cli.command.utils;

import java.util.Arrays;
import java.util.List;

import com.atomist.source.ArtifactSource;
import com.atomist.source.DirectoryArtifact;
import com.atomist.source.FileArtifact;

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

}
