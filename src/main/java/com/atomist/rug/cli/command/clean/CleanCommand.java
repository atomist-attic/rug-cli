package com.atomist.rug.cli.command.clean;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.command.AbstractAnnotationBasedCommand;
import com.atomist.rug.cli.command.CommandException;
import com.atomist.rug.cli.command.CommandUtils;
import com.atomist.rug.cli.command.annotation.Argument;
import com.atomist.rug.cli.command.annotation.Command;
import com.atomist.rug.cli.command.annotation.Option;
import com.atomist.rug.cli.command.search.SearchOperations.Archive;
import com.atomist.rug.cli.command.search.SearchOperations.Operation;
import com.atomist.rug.cli.output.ProgressReportingOperationRunner;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.settings.Settings;
import com.atomist.rug.cli.tree.LogVisitor;
import com.atomist.rug.cli.tree.Node;
import com.atomist.rug.cli.tree.Node.Type;
import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("unused")
public class CleanCommand extends AbstractAnnotationBasedCommand {

    @Command
    public void run() {
        File project = CommandUtils.getRequiredWorkingDirectory();
        File target = new File(project, Constants.ATOMIST_ROOT + File.separator + "target");
        
        if (target.exists() && !FileUtils.deleteQuietly(target)) {
            throw new CommandException("Unable to delete .atomist/target directory.", "clean");
        }
        else {
            log.newline();
            log.info(Style.green("Successfully cleaned project"));
        }
    }
}
