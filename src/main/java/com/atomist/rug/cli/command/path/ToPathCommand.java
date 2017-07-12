package com.atomist.rug.cli.command.path;

import java.io.File;

import org.apache.commons.lang3.text.WordUtils;
import org.springframework.util.StringUtils;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.command.AbstractAnnotationBasedCommand;
import com.atomist.rug.cli.command.CommandException;
import com.atomist.rug.cli.command.annotation.Argument;
import com.atomist.rug.cli.command.annotation.Command;
import com.atomist.rug.cli.command.annotation.Option;
import com.atomist.rug.cli.command.annotation.Validator;
import com.atomist.rug.cli.command.utils.ArtifactSourceUtils;
import com.atomist.rug.cli.output.ProgressReportingOperationRunner;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.utils.FileUtils;
import com.atomist.rug.kind.DefaultTypeRegistry$;
import com.atomist.rug.kind.core.ProjectMutableView;
import com.atomist.rug.spi.Typed;
import com.atomist.source.ArtifactSource;
import com.atomist.source.EmptyArtifactSource;

import scala.collection.JavaConverters;

public class ToPathCommand extends AbstractAnnotationBasedCommand {

    @Validator
    public void validate(@Option("change-dir") String projectName, @Option("kind") String kind,
            @Option(value = "line", defaultValue = "-1") int line,
            @Option(value = "column", defaultValue = "-1") int col) {
        File root = FileUtils.createProjectRoot(projectName);
        if (!root.exists()) {
            throw new CommandException(String.format(
                    "Target directory %s does not exist.\nPlease fix the directory path provided to --change-dir.",
                    projectName), "to-path");
        }
        if (!root.isDirectory()) {
            throw new CommandException(String.format(
                    "Target path %s is not a directory.\nPlease fix the directory path provided to --change-dir.",
                    projectName), "to-path");
        }
        if (!StringUtils.hasText(kind) || line < 0 || col < 0) {
            throw new CommandException("Options --kind, --line and --column are required.",
                    "to-path");
        }
        scala.Option<Typed> type = DefaultTypeRegistry$.MODULE$.findByName(kind);
        if (type.isEmpty()) {
            String kinds = StringUtils.collectionToDelimitedString(JavaConverters
                    .asJavaCollectionConverter(DefaultTypeRegistry$.MODULE$.typeNames().toBuffer())
                    .asJavaCollection(), ", ");
            throw new CommandException(
                    String.format("Provided kind %s is not valid.\nKnown kinds are:\n  %s", kind,
                            WordUtils.wrap(kinds, Constants.WRAP_LENGTH, "\n  ", false)),
                    "to-path");
        }
    }

    @Command
    public void run(@Argument(index = 2, defaultValue = "") String path,
            @Option("change-dir") String rootName, @Option("kind") String kind,
            @Option("line") int line, @Option("column") int col) {

        String exp = new ProgressReportingOperationRunner<String>(
                "Creating path expression within project").run((indicator) -> {

                    File root = FileUtils.createProjectRoot(rootName);
                    ArtifactSource source = ArtifactSourceUtils
                            .createArtifactSource(root.getCanonicalFile());

                    if (source.findFile(path).isEmpty()) {
                        throw new CommandException(
                                String.format("Target path %s does not exist in project.", path),
                                "to-path");
                    }

                    ProjectMutableView pmv = new ProjectMutableView(new EmptyArtifactSource(""),
                            source);
                    String result = pmv.pathTo(path, kind, line, col);

                    if (result == null) {
                        throw new CommandException("Failed to create path expression.", "to-path");
                    }

                    return result;
                });

        printResult(exp);
    }

    protected void printResult(String exp) {
        log.newline();
        log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Path Expression"));
        log.info("  %s", Style.yellow(exp));
        log.newline();
        log.info(Style.green("Successfully created path expression"));
    }
}
