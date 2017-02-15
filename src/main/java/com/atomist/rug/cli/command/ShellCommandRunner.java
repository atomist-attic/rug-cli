package com.atomist.rug.cli.command;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.LineReaderImpl;
import org.springframework.boot.loader.tools.RunProcess;

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
        
        LineReader reader = ShellUtils.lineReader(ShellUtils.SHELL_HISTORY,
                new ChangeDirCompleter(), new OperationCompleter(),
                new CommandInfoCompleter(registry));
        
        
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
                
                if ("exit".equals(line) || "quit".equals(line)
                        || "q".equals(line)) {
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
        catch (UserInterruptException e) {
        }
        catch (EndOfFileException e) {
            log.info("Goodbye!");
        }
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
    protected void commandCompleted(int rc, CommandInfo info, ArtifactDescriptor artifact,
            List<ArtifactDescriptor> dependencies) {
        if (rc == 0 && "shell".equals(info.name())) {
            invokeCommandInLoop(artifact, dependencies);
        }
    }
}
