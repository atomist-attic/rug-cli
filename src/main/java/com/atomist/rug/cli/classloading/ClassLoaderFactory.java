package com.atomist.rug.cli.classloading;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import com.atomist.rug.cli.RunnerException;
import com.atomist.rug.resolver.ArtifactDescriptor;

public abstract class ClassLoaderFactory {

    public static void setupClassLoader(ArtifactDescriptor artifact,
            List<ArtifactDescriptor> dependencies) {
        setupClassLoader(artifact, dependencies, null);
    }

    public static void setupClassLoader(ArtifactDescriptor artifact,
            List<ArtifactDescriptor> dependencies, ClasspathEntryProvider classpathEntryProvider) {
        List<URL> urls = getDependencies(dependencies);

        // Add the url to the enclosing JAR
        URL codeLocation = ClassLoaderFactory.class.getProtectionDomain().getCodeSource()
                .getLocation();
        urls.add(codeLocation);

        addExtensionsToClasspath(urls);
        addCommandExtensionsToClasspath(artifact, classpathEntryProvider, urls);

        ClassLoader cls = null;
        if (codeLocation.toString().endsWith("jar")) {
            // If running from an IDE we need a different classloader hierarchy
            cls = new DelegatingUrlClassLoader(urls.toArray(new URL[urls.size()]),
                    Thread.currentThread().getContextClassLoader());
        }
        else {
            cls = new URLClassLoader(urls.toArray(new URL[urls.size()]));
        }
        Thread.currentThread().setContextClassLoader(cls);
    }

    private static void addCommandExtensionsToClasspath(ArtifactDescriptor artifact,
            ClasspathEntryProvider classpathEntryProvider, List<URL> urls) {
        if (classpathEntryProvider != null) {
            urls.addAll(classpathEntryProvider.classpathEntries(artifact));
        }
    }

    private static void addExtensionsToClasspath(List<URL> urls) {
        File extDir = new File(FileUtils.getUserDirectory(), ".atomist/ext");
        if (extDir.exists() && extDir.isDirectory()) {
            FileUtils.listFiles(extDir, new String[] { "jar" }, true).forEach(f -> {
                try {
                    urls.add(f.toURI().toURL());
                }
                catch (MalformedURLException e) {
                }
            });
        }
    }

    private static List<URL> getDependencies(List<ArtifactDescriptor> dependencies) {
        return dependencies.stream().map(ad -> new File(ad.uri())).map(f -> {
            try {
                return f.toURI().toURL();
            }
            catch (MalformedURLException e) {
                throw new RunnerException("Error occured creating URL", e);
            }
        }).collect(Collectors.toList());
    }
}
