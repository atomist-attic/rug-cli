package com.atomist.rug.cli.command;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.atomist.rug.cli.Log;
import com.atomist.rug.cli.tree.ArtifactSourceTreeCreator;
import com.atomist.rug.cli.tree.LogVisitor;
import com.atomist.rug.cli.utils.ArtifactDescriptorUtils;
import com.atomist.rug.cli.utils.CommandLineOptions;
import com.atomist.rug.cli.utils.FileUtils;
import com.atomist.rug.loader.OperationsAndHandlers;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.source.ArtifactSource;

public abstract class CommandEventListenerRegistry {
    
    private static List<CommandEventListener> listeners = new ArrayList<>();
    static {
        register(new ArtifactSourcePrintingCommandEventListener());
        register(new CommandContextManagingCommandEventListener());
    }
    
    public static void register(CommandEventListener listener) {
        listeners.add(listener);
    }
    
    public static void raiseEvent(Consumer<CommandEventListener> consumer) {
        listeners.forEach(l -> consumer.accept(l));
    }
    
    
    private static class ArtifactSourcePrintingCommandEventListener extends CommandEventListenerAdapter {
        
        private static Log log = new Log(ArtifactSourcePrintingCommandEventListener.class);
        
        @Override
        public void artifactSourceLoaded(ArtifactDescriptor artifact, ArtifactSource source) {
            printArtifactSource(artifact, source);
        }
        
        private void printArtifactSource(ArtifactDescriptor artifact, ArtifactSource source) {
            if (CommandLineOptions.hasOption("V") && source != null) {
                log.info("Loaded archive sources for %s",
                        ArtifactDescriptorUtils.coordinates(artifact));
                log.info("  " + FileUtils.relativize(artifact.uri()));
                ArtifactSourceTreeCreator.visitTree(source, new LogVisitor(log));
            }
        }
    }
    
    private static class CommandContextManagingCommandEventListener extends CommandEventListenerAdapter {
        
        @Override
        public void artifactSourceCompiled(ArtifactDescriptor artifact, ArtifactSource source) {
            CommandContext.save(ArtifactSource.class, source);
        }
        
        @Override
        public void operationsLoaded(ArtifactDescriptor artifact,
                OperationsAndHandlers operations) {
            CommandContext.save(OperationsAndHandlers.class, operations);
        }
    }
}
