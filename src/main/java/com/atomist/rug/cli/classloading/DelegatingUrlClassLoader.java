package com.atomist.rug.cli.classloading;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;

class DelegatingUrlClassLoader extends URLClassLoader {

    // Nashorn and some of the scripting classes need to come from the system classloader;
    // everything else we need to isolate and not delegate to the parent class loader
    private static final String[] DEFAULT_DELEGATING_PACKAGES = new String[] { "org.slf4j",
            "jdk.nashorn", "javax.scripting" };

    private List<String> delegatingPackages = Arrays.asList(DEFAULT_DELEGATING_PACKAGES);

    private ClassLoader parent;

    public DelegatingUrlClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, null);
        this.parent = parent;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (delegatingPackages.stream().filter(dp -> name.startsWith(dp)).findFirst().isPresent()) {
            return parent.loadClass(name);
        }
        else {
            return super.loadClass(name);
        }
    }
}