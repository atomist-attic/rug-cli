package com.atomist.rug.cli.utils;

import com.atomist.param.ParameterValues;
import com.atomist.project.ProjectOperation;
import com.atomist.rug.runtime.plans.ProjectFinder;
import com.atomist.source.ArtifactSource;
import scala.Option;

/**
 * Find projects in git based on some context
 */
public class LocalGitProjectFinder implements ProjectFinder {

    @Override
    public Option<ArtifactSource> findArtifactSource(ProjectOperation editor,
            ParameterValues arguments, String projectName) {
        return null;
    }
}
