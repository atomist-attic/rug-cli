package com.atomist.rug.cli.classloading;

import java.net.URL;
import java.util.List;

import com.atomist.rug.resolver.ArtifactDescriptor;

public interface ClasspathEntryProvider {
    
    List<URL> classpathEntries(ArtifactDescriptor artifact);
    
}
