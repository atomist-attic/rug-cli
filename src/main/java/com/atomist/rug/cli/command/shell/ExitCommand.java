package com.atomist.rug.cli.command.shell;

import org.apache.commons.cli.CommandLine;

import com.atomist.project.archive.ResolvedDependency;
import com.atomist.project.archive.RugResolver;
import com.atomist.rug.cli.command.AbstractCommand;
import com.atomist.rug.cli.command.CommandContext;
import com.atomist.rug.cli.command.fs.ArtifactSourceFileWatcherFactory.FileWatcher;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.source.ArtifactSource;

public class ExitCommand extends AbstractCommand {

    @Override
    protected void run(ArtifactDescriptor artifact, CommandLine commandLine) {
        // Shutdown file system watcher
        if (CommandContext.contains(FileWatcher.class)) {
            CommandContext.restore(FileWatcher.class).shutdown();
        }
        // Clear out context
        CommandContext.delete(FileWatcher.class);
        CommandContext.delete(ArtifactSource.class);
        CommandContext.delete(ResolvedDependency.class);
        CommandContext.delete(RugResolver.class);
    }
}
