package com.atomist.rug.cli.classloading;

import com.atomist.rug.cli.RunnerException;
import com.atomist.rug.resolver.ArtifactDescriptor;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Sets up the context classloader.
 * At runtime, all dependencies come from the Rug Archive.
 * At dev/build time, the Rug version comes from this project's pom, not from the Rug Archives used
 * for
 * test.
 */
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

        // If running from an IDE we need a different classloader hierarchy
        if (codeLocation.toString().endsWith("jar")) {
            cls = createJarClassLoader(urls);
        }
        else {
            cls = createDevClassLoader(urls);
        }

        Thread.currentThread().setContextClassLoader(cls);
    }

    private static ClassLoader createDevClassLoader(List<URL> urls) {
        // Used only in ide/dev/build so that we use Rug from this project's pom, and not from the
        // Rug Archive used in the tests!
        List<URL> filtered = urls.stream()
                .filter(url -> !url.toString().contains("/com/atomist/rug/"))
                .collect(Collectors.toList());
        return new URLClassLoader(filtered.toArray(new URL[filtered.size()]));
    }

    private static ClassLoader createJarClassLoader(List<URL> urls) {
        return new DelegatingUrlClassLoader(urls.toArray(new URL[urls.size()]),
                Thread.currentThread().getContextClassLoader());
    }

    /**
     * J2V8 is not available from the system {@link ClassLoader} but we still want to delegate to
     * shared {@link ClassLoader} when we run the shell for different archives in one session.
     */
    public static void setupJ2V8ClassLoader(List<ArtifactDescriptor> dependencies) {
        // Check if the J2V8 ClassLoader has already been installed
        if (Thread.currentThread().getContextClassLoader() instanceof J2V8ClassLoader) {
            return;
        }

        Optional<ArtifactDescriptor> j2v8 = dependencies.stream()
                .filter(d -> d.group().equals("com.eclipsesource.j2v8")).findAny();
        if (j2v8.isPresent()) {
            try {
                Thread.currentThread().setContextClassLoader(
                        new J2V8ClassLoader(new URL[] { j2v8.get().uri().toURL() },
                                Thread.currentThread().getContextClassLoader()));
            }
            catch (MalformedURLException e) {
                throw new RunnerException(e);
            }
        }
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

    /**
     * Internal {@link ClassLoader} to be able to later identify it.
     */
    private static class J2V8ClassLoader extends URLClassLoader {

        public J2V8ClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

    }
}
