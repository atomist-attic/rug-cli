package com.atomist.rug.cli.command;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.LineReaderImpl;
import org.springframework.boot.loader.tools.RunProcess;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.ReloadException;
import com.atomist.rug.cli.command.shell.ArchiveNameCompleter;
import com.atomist.rug.cli.command.shell.ChangeDirCompleter;
import com.atomist.rug.cli.command.shell.CommandInfoCompleter;
import com.atomist.rug.cli.command.shell.OperationCompleter;
import com.atomist.rug.cli.command.shell.ShellUtils;
import com.atomist.rug.resolver.ArtifactDescriptor;

public class ShellCommandRunner extends ReflectiveCommandRunner {

    private CommandInfoRegistry registry;

    public ShellCommandRunner(CommandInfoRegistry registry) {
        super(registry);
        this.registry = registry;
    }

    private void clear(LineReader reader) {
        ((LineReaderImpl) reader).clearScreen();
    }

    private void exit() {
        throw new EndOfFileException();
    }

    private void invokeCommandInLoop(ArtifactDescriptor artifact,
            List<ArtifactDescriptor> dependencies) {

        configureEnv();
        LineReader reader = lineReader();

        String line = null;
        try {
            while ((line = reader.readLine(ShellUtils.DEFAULT_PROMPT)) != null) {
                if (line.length() == 0) {
                    continue;
                }

                line = line.trim();
                if (line.startsWith("rug")) {
                    line = line.substring(3).trim();
                }
                if (line.startsWith("shell") || line.startsWith("load") || line.startsWith("sh")
                        || line.startsWith("repl")) {
                    reload(line);
                }
                else if ("exit".equals(line) || "quit".equals(line) || "q".equals(line)) {
                    exit();
                }
                else if ("clear".equals(line) || "cls".equals(line)) {
                    clear(reader);
                }
                else if (line.startsWith("!")) {
                    ps(line);
                }
                else {
                    String[] args = CommandUtils.splitCommandline(line);
                    invokeCommand(args, artifact, dependencies, null);
                }
            }
        }
        catch (EndOfFileException | UserInterruptException e) {
            log.info("Goodbye!");
        }
    }

    private void configureEnv() {
        // TODO this is hacky
        Constants.COMMAND = Constants.DEFAULT_COMMAND + " " + Constants.DIVIDER;
        Constants.IS_SHELL = true;
    }

    private void reload(String line) {
        String[] args = CommandUtils.splitCommandline(line);
        throw new ReloadException(args);
    }

    private LineReader lineReader() {
        return ShellUtils.lineReader(ShellUtils.SHELL_HISTORY, new ChangeDirCompleter(),
                new OperationCompleter(), new CommandInfoCompleter(registry),
                new ArchiveNameCompleter());
    }

    private void ps(String line) {
        String[] args = CommandUtils.splitCommandline(line.substring(1));
        RunProcess process = new RunProcess(args[0]);
        try {
            process.run(true, Arrays.copyOfRange(args, 1, args.length));
        }
        catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    protected void artifactChanged(ArtifactDescriptor artifact, CommandInfo info,
            CommandLine commandLine) {
        if (info instanceof ArtifactDescriptorProvider) {
            ArtifactDescriptor newArtifact = null;
            try {
                newArtifact = ((ArtifactDescriptorProvider) info).artifactDescriptor(commandLine);

            }
            catch (CommandException e) {
                // This is ok here as it means that no artifact information was provided
            }
            // Verify that in a shell session we don't support fq operation or archive name
            if (Constants.IS_SHELL && newArtifact != null
                    && !(artifact.group().equals(newArtifact.group())
                            && artifact.artifact().equals(newArtifact.artifact()))) {
                throw new CommandException(String.format(
                        "Fully-qualified archive or operation names are not allowed for this command while running a shell.\nPlease load the archive by running:\n  load %s:%s",
                        newArtifact.group(), newArtifact.artifact()), info.name());
            }
        }

    }

    @Override
    protected void commandCompleted(int rc, CommandInfo info, ArtifactDescriptor artifact,
            List<ArtifactDescriptor> dependencies) {
        if (rc == 0 && "shell".equals(info.name())) {
            invokeCommandInLoop(artifact, dependencies);
        }
    }
}
