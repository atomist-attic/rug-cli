package com.atomist.rug.cli.command;

import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.LocalArtifactDescriptor;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;

public class ReflectiveCommandRunMethodRunner {

    public void invokeCommand(ArtifactDescriptor artifact, CommandInfo info, String[] args,
            List<URI> uris) throws Exception {
        Class<?> commandClass = Thread.currentThread().getContextClassLoader()
                .loadClass(info.className());

        if (info instanceof ArtifactDescriptorProvider) {
            Method runMethod = commandClass.getMethod("run", String.class, String.class,
                    String.class, String.class, boolean.class, URI[].class, String[].class);
            runMethod.invoke(commandClass.newInstance(), artifact.group(), artifact.artifact(),
                    artifact.version(), artifact.extension().toString(),
                    (artifact instanceof LocalArtifactDescriptor),
                    uris.toArray(new URI[uris.size()]), args);
        }
        else {
            Method runMethod = commandClass.getMethod("run", String[].class);
            runMethod.invoke(commandClass.newInstance(), (Object) args);
        }
    }

}
