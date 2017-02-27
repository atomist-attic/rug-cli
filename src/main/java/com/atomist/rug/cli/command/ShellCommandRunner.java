package com.atomist.rug.cli.command;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.ReloadException;
import com.atomist.rug.cli.command.shell.ArchiveNameCompleter;
import com.atomist.rug.cli.command.shell.CommandInfoCompleter;
import com.atomist.rug.cli.command.shell.FileAndDirectoryNameCompleter;
import com.atomist.rug.cli.command.shell.OperationCompleter;
import com.atomist.rug.cli.command.shell.ShellUtils;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.utils.ArtifactDescriptorUtils;
import com.atomist.rug.cli.utils.StringUtils;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.LocalArtifactDescriptor;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.SystemUtils;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.LineReaderImpl;
import org.springframework.boot.loader.tools.RunProcess;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ShellCommandRunner extends ReflectiveCommandRunner {

    private CommandInfoRegistry registry;
    private LineReader reader;

    public ShellCommandRunner(CommandInfoRegistry registry) {
        super(registry);
        this.registry = registry;
    }

    private void clear() {
        ((LineReaderImpl) reader).clearScreen();
    }

    private void exit(ArtifactDescriptor artifact, List<ArtifactDescriptor> dependencies) {
        // Exit the current shell
        invokeCommand("exit", new String[] {"exit"}, artifact, dependencies);
        throw new EndOfFileException();
    }

    private void invokeCommandInLoop(ArtifactDescriptor artifact,
            List<ArtifactDescriptor> dependencies) {

        this.reader = lineReader();

        String line = null;
        try {
            while (true) {

                try {
                    line = reader.readLine(prompt(artifact));
                }
                catch (UserInterruptException e) {
                    // Ignore Ctrl-C
                    continue;
                }

                // Empty line
                if (line == null || line.length() == 0) {
                    continue;
                }

                // Now ready to handle the input line
                handleInput(artifact, dependencies, line);
            }
        }
        catch (EndOfFileException e) {
            // Handle Ctrl-D
            log.info("Goodbye!");
        }
        finally {
            // Jline creates some resources that need proper shutdown
            ShellUtils.shutdown(reader);
        }
    }

    private void handleInput(ArtifactDescriptor artifact, List<ArtifactDescriptor> dependencies,
            String line) {
        // Remove confusing whitespace from beginning and end
        line = line.trim();

        // Expand history
        line = expandHistory(line);

        if (line.startsWith("rug")) {
            line = line.substring(3).trim();
        }

        // Handle some internal commands
        // Shell command might choose to exit. If not it probably means the user wants help
        if (line.startsWith("shell") || line.startsWith("load") || line.startsWith("repl")) {
            reload(line, artifact, dependencies);
        }

        if ("exit".equals(line) || "quit".equals(line) || "q".equals(line)) {
            exit(artifact, dependencies);
        }
        else if ("/clear".equals(line) || "/cls".equals(line)) {
            clear();
        }
        else if (line.startsWith(Constants.SHELL_ESCAPE)) {
            sh(line);
        }
        else {
            // Split commands by && and call them one after the other
            String[] cmds = line.split("&&");
            Arrays.stream(cmds).forEach(c -> {
                String[] args = CommandUtils.splitCommandline(c);
                CommandLine commandLine = CommandUtils.parseInitialCommandline(args, registry);
                invokeCommand(commandLine.getArgList().get(0), args, artifact, dependencies);
            });
        }
    }

    private String expandHistory(String line) {
        try {
            return reader.getExpander().expandHistory(reader.getHistory(), line);
        }
        catch (IllegalArgumentException e) {
            log.error(e.getMessage());
            return line;
        }
    }

    private String prompt(ArtifactDescriptor artifact) {
        if (artifact != null && !(artifact.group().equals(Constants.GROUP)
                && artifact.artifact().equals(Constants.RUG_ARTIFACT))) {
            log.info(Style.gray(ArtifactDescriptorUtils.coordinates(artifact)));
        }
        return ShellUtils.DEFAULT_PROMPT;
    }

    private void reload(String line, ArtifactDescriptor artifact, List<ArtifactDescriptor> dependencies) {
        // Exit the current shell
        invokeCommand("exit", new String[] {"exit"}, artifact, dependencies);
        
        String[] args = CommandUtils.splitCommandline(line);
        CommandLine commandLine = CommandUtils.parseCommandline("shell", args, registry);
        // Only trigger reload if not help is what is requested
        if (!commandLine.hasOption("h") && !commandLine.hasOption("?")) {
            throw new ReloadException(args);
        }
    }

    private LineReader lineReader() {
        return ShellUtils.lineReader(ShellUtils.SHELL_HISTORY, new FileAndDirectoryNameCompleter(),
                new OperationCompleter(), new CommandInfoCompleter(registry),
                new ArchiveNameCompleter());
    }

    private void sh(String line) {
        // Remove leading command
        line = line.substring(Constants.SHELL_ESCAPE.length());

        // Expand all env variables
        line = StringUtils.expandEnvironmentVarsAndHomeDir(line);

        // Split multiple commands into several commands that we run one ofter the other
        String[] cmds = line.split("&&");

        // Iterator all commands
        Arrays.stream(cmds).forEach(c -> {
            String[] args = CommandUtils.splitCommandline(c);

            RunProcess process = new RunProcess(SystemUtils.getUserDir(), args[0]);
            try {
                int rc = process.run(true, Arrays.copyOfRange(args, 1, args.length));
                // Change the working directory of this shell
                if (rc == 0 && "cd".equals(args[0])) {
                    if (args.length > 1) {
                        String path = args[1];
                        File p = new File(path);
                        if (p.exists()) {
                            System.setProperty("user.dir", p.getCanonicalPath());
                        }
                        else {
                            File workingDir = new File(SystemUtils.getUserDir(), path);
                            System.setProperty("user.dir", workingDir.getCanonicalPath());
                        }
                    }
                }
            }
            catch (IOException e) {
                log.error(e.getMessage());
            }
        });
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
                // This is ok here as it means that no artifact information was provided on the
                // commandline
                return;
            }
            // Verify that in a shell session we don't support fq operation or archive name
            if (Constants.isShell() && newArtifact != null
                    && !(newArtifact instanceof LocalArtifactDescriptor)) {
                // It is ok to load rug into the runtime; that just means we stay in current scope
                if (newArtifact.group().equals(Constants.GROUP)
                        && newArtifact.artifact().equals(Constants.RUG_ARTIFACT)) {
                    return;
                }
                // It is NOT ok to request a different archive without reloading
                if (!(artifact.group().equals(newArtifact.group())
                        && artifact.artifact().equals(newArtifact.artifact()))) {
                    throw new CommandException(String.format(
                            "Fully-qualified archive or Rug names are not allowed "
                                    + "within a shell session.\nTo load an archive into the shell, run:\n  shell %s:%s",
                            newArtifact.group(), newArtifact.artifact()), info.name());
                }
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
