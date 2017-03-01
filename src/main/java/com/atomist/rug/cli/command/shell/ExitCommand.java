package com.atomist.rug.cli.command.shell;

import com.atomist.project.archive.Rugs;
import com.atomist.rug.cli.command.AbstractCommand;
import com.atomist.rug.cli.command.CommandContext;
import com.atomist.rug.cli.command.fs.ArtifactSourceFileWatcherFactory.FileWatcher;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.source.ArtifactSource;

import java.net.URI;

import org.apache.commons.cli.CommandLine;

public class ExitCommand extends AbstractCommand {

    @Override
    protected void run(URI[] uri, ArtifactDescriptor artifact, CommandLine commandLine) {
        // Shutdown file system watcher
        if (CommandContext.contains(FileWatcher.class)) {
            CommandContext.restore(FileWatcher.class).shutdown();
        }
        // Clear out context
        CommandContext.delete(FileWatcher.class);
        CommandContext.delete(ArtifactSource.class);
        CommandContext.delete(Rugs.class);
    }
}
