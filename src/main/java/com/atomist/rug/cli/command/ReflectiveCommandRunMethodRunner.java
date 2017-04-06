package com.atomist.rug.cli.command;

import java.lang.reflect.Method;

import com.atomist.rug.resolver.ArtifactDescriptor;

public class ReflectiveCommandRunMethodRunner {

    public void invokeCommand(ArtifactDescriptor artifact, CommandInfo info, String[] args)
            throws Exception {
        Class<?> commandClass = Thread.currentThread().getContextClassLoader()
                .loadClass(info.className());

        if (info instanceof ArtifactDescriptorProvider) {
            Method runMethod = commandClass.getMethod("run", ArtifactDescriptor.class,
                    String[].class);
            runMethod.invoke(commandClass.newInstance(), artifact, args);
        }
        else {
            Method runMethod = commandClass.getMethod("run", String[].class);
            runMethod.invoke(commandClass.newInstance(), (Object) args);
        }
    }

}
