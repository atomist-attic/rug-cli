package com.atomist.rug.cli.classloading;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.ClassUtils;

class DelegatingUrlClassLoader extends URLClassLoader {

    // Nashorn and some of the scripting classes need to come from the system classloader;
    // everything else we need to isolate and not delegate to the parent class loader

    // J2V8 also doesn't like to be loaded by different class loaders and for the shell reload
    // we end up doing that.
    private static final String[] DEFAULT_DELEGATING_PACKAGES_AND_SUBPACKAGES = new String[] {
            "org.slf4j", "jdk.nashorn", "javax.scripting", "com.eclipsesource.v8" };

    private static final String[] DEFAULT_DELEGATING_PACKAGES = new String[] {
            "com.atomist.rug.resolver", "com.atomist.rug.cli", "com.atomist.rug.cli.settings" };

    private List<String> delegatingPackagesAndSubPackages = Arrays
            .asList(DEFAULT_DELEGATING_PACKAGES_AND_SUBPACKAGES);
    private List<String> delegatingPackages = Arrays.asList(DEFAULT_DELEGATING_PACKAGES);

    private ClassLoader parent;

    DelegatingUrlClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, null);
        this.parent = parent;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        String packageName = ClassUtils.getPackageCanonicalName(name);
        if (delegatingPackagesAndSubPackages.stream().anyMatch(name::startsWith)) {
            return parent.loadClass(name);
        }
        else if (delegatingPackages.stream().anyMatch(packageName::equals)) {
            return parent.loadClass(name);
        }
        else {
            return super.loadClass(name);
        }
    }
}
