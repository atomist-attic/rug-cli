package com.atomist.rug.cli.command.shell;

import java.io.File;
import java.io.IOException;

import org.jline.reader.Completer;
import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.LineReader.Option;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultHighlighter;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.command.CommandException;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.utils.FileUtils;

public abstract class ShellUtils {

    public static final File SHELL_OPERATIONS = new File(System.getProperty("user.home")
            + File.separator + ".atomist" + File.separator + ".cli-operations");

    public static final File SHELL_HISTORY = new File(System.getProperty("user.home")
            + File.separator + ".atomist" + File.separator + ".cli-history");

    public static final File INTERACTIVE_HISTORY = new File(System.getProperty("user.home")
            + File.separator + ".atomist" + File.separator + ".interactive-history");
    
    public static final File SHELL_ARCHIVES = new File(System.getProperty("user.home")
            + File.separator + ".atomist" + File.separator + ".cli-archives");

    public static final String DEFAULT_PROMPT = Style.yellow(Constants.RUG_ARTIFACT) + " "
            + Style.cyan(Constants.DIVIDER) + " ";
    
    public static final String SHELL_ESCAPE = "/";

    public static LineReader lineReader(File historyPath, Completer... completers) {
        // Protect the history file as may contain sensitive information
        FileUtils.setPermissionsToOwnerOnly(historyPath);

        // Create JLine LineReader
        History history = new DefaultHistory();
        LineReader reader = LineReaderBuilder.builder().terminal(terminal()).history(history)
                .parser(new DefaultParser()).variable(LineReader.HISTORY_FILE, historyPath)
                .completer(new AggregateCompleter(completers)).highlighter(new DefaultHighlighter())
                .build();
        history.attach(reader);
        
        reader.unsetOpt(Option.AUTO_MENU);
        reader.unsetOpt(Option.GROUP);
        reader.unsetOpt(Option.MENU_COMPLETE);
        
        reader.setOpt(Option.AUTO_LIST);
        reader.setOpt(Option.LIST_AMBIGUOUS);
        
        return reader;
    }
    
    public static void shutdown(LineReader lineReader) {
        try {
            lineReader.getTerminal().close();
        }
        catch (IOException e) {
        }
    }

    private static Terminal terminal() {
        try {
            return TerminalBuilder.builder().build();
        }
        catch (IOException e) {
            throw new CommandException("Error creating terminal for Shell.", e);
        }
    }
}
