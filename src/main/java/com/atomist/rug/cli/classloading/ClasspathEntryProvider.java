package com.atomist.rug.cli.classloading;

import com.atomist.rug.resolver.ArtifactDescriptor;

import java.net.URL;
import java.util.List;

public interface ClasspathEntryProvider {

    List<URL> classpathEntries(ArtifactDescriptor artifact);

}
