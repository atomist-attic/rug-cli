package com.atomist.rug.cli.command.shell;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.command.AbstractAnnotationBasedCommand;
import com.atomist.rug.cli.command.CommandEventListener;
import com.atomist.rug.cli.command.CommandEventListenerRegistry;
import com.atomist.rug.cli.command.annotation.Command;
import com.atomist.rug.cli.output.ProgressReportingOperationRunner;
import com.atomist.rug.cli.utils.ArtifactDescriptorUtils;
import com.atomist.rug.cli.version.VersionUtils;
import com.atomist.rug.loader.OperationsAndHandlers;
import com.atomist.rug.metadata.MetadataWriter;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.source.ArtifactSource;
import com.atomist.source.FileArtifact;

public class ShellCommand extends AbstractAnnotationBasedCommand {

    private static final String banner = "" 
            + "  ____                 ____ _     ___ \n"
            + " |  _ \\ _   _  __ _   / ___| |   |_ _|\n"
            + " | |_) | | | |/ _` | | |   | |    | | \n"
            + " |  _ <| |_| | (_| | | |___| |___ | | \n"
            + " |_| \\_\\\\__,_|\\__, |  \\____|_____|___|\n" 
            + " Atomist      |___/ %s";

    @Command
    public void run(ArtifactSource source, ArtifactDescriptor artifact,
            OperationsAndHandlers operations) {
        // TODO make this a proper thing
        Constants.COMMAND = Constants.COMMAND + " " + Constants.DIVIDER;
        
        new ProgressReportingOperationRunner<Void>(String.format("Initializing shell for %s",
                ArtifactDescriptorUtils.coordinates(artifact))).run((reporter) -> {
                    CommandEventListener listener = new OperationsLoadedEventListener(source,
                            artifact);
                    listener.operationsLoaded(operations);
                    CommandEventListenerRegistry.register(listener);
                    return null;
                });

        log.info(banner, StringUtils
                .leftPad(VersionUtils.readVersion().orElse("0.0.0").split("-")[0], 18));
        log.newline();
        log.info("Press 'Tab' to complete. Type 'help' and hit 'Return' for help, and 'exit' to quit.");
    }

    private static class OperationsLoadedEventListener implements CommandEventListener {

        private ArtifactDescriptor artifact;
        private ArtifactSource source;

        public OperationsLoadedEventListener(ArtifactSource source, ArtifactDescriptor artifact) {
            this.artifact = artifact;
            this.source = source;
        }

        @Override
        public void operationsLoaded(OperationsAndHandlers operations) {

            FileArtifact file = MetadataWriter.create(operations, artifact, source, null);

            try {
                FileUtils.write(ShellUtils.SHELL_OPERATIONS, file.content(),
                        StandardCharsets.ISO_8859_1);
                FileUtils.forceDeleteOnExit(ShellUtils.SHELL_OPERATIONS);
            }
            catch (IOException e) {
                // We can't write the operations out to a file, so what?
            }
        }
    }

}
