package com.atomist.rug.cli.command;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.atomist.rug.cli.settings.Settings;
import com.atomist.rug.cli.settings.SettingsReader;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.ArtifactDescriptor.Extension;
import com.atomist.rug.resolver.ArtifactDescriptor.Scope;
import com.atomist.rug.resolver.DefaultArtifactDescriptor;
import com.atomist.rug.resolver.LocalArtifactDescriptor;

public abstract class AbstractVersionCommandInfo extends AbstractLocalArtifactDescriptorProvider {

    private static final String COORDINATE_PATTERN_STRING = "([a-zA-Z0-9\\-.]+):([a-zA-Z0-9\\-.]+)(:([a-zA-Z0-9\\-. ]*))?";
    private static final String SHORT_COORDINATE_PATTERN_STRING = "([a-zA-Z0-9\\-.]+)(:([a-zA-Z0-9\\-. ]*))?";

    private int ix = -1;

    public AbstractVersionCommandInfo(Class<? extends Command> commandClass, String commandName,
            int ix) {
        super(commandClass, commandName);
        this.ix = ix;
    }

    @Override
    public ArtifactDescriptor artifactDescriptor(CommandLine commandLine) {
        if (commandLine.hasOption('l')) {
            return super.artifactDescriptor(commandLine);
        }
        String version = commandLine.getOptionValue('a');

        String name = null;
        if (ix < commandLine.getArgList().size()) {
            name = commandLine.getArgList().get(ix);
        }

        return findArtifact(name, version);
    }

    @Override
    public Options options() {
        Options options = new Options();
        options.addOption(Option.builder("a").longOpt("archive-version").argName("AV").hasArg(true)
                .required(false).desc("Use archive version AV").build());
        options.addOption(Option.builder("l").longOpt("local").hasArg(false).required(false)
                .desc("Use local working directory as archive").build());
        return options;
    }

    @Override
    public boolean enabled(ArtifactDescriptor artifact) {
        return artifact instanceof LocalArtifactDescriptor
                || artifact.extension().equals(Extension.ZIP);
    }

    protected final ArtifactDescriptor findArtifact(String name, String version) {
        Pattern pattern = Pattern.compile(COORDINATE_PATTERN_STRING);
        Matcher matcher = pattern.matcher((name != null ? name : ""));

        // full qualified name given <group>:<artifact>:<name>
        if (matcher.matches() && matcher.groupCount() >= 2) {
            return new DefaultArtifactDescriptor(matcher.group(1), matcher.group(2),
                    (version == null ? "latest" : version), Extension.ZIP, Scope.COMPILE, null);
        }

        // see if we got a default group and artifact
        Settings settings = SettingsReader.read();
        String defaultGroup = settings.getDefaults().getGroup();
        String defaultArtifact = settings.getDefaults().getArtifact();
        String defaultVersion = settings.getDefaults().getVersion();

        // command line always wins in case it is given
        if (version == null) {
            version = defaultVersion;
        }
        if (version == null) {
            version = "latest";
        }

        pattern = Pattern.compile(SHORT_COORDINATE_PATTERN_STRING);
        matcher = pattern.matcher((name != null ? name : ""));

        if (defaultGroup != null && defaultArtifact == null && matcher.matches()
                && matcher.groupCount() == 3) {
            // now we need at least one ':' hence groupCount == 2
            return new DefaultArtifactDescriptor(defaultGroup, matcher.group(1), version,
                    Extension.ZIP, Scope.COMPILE, null);
        }
        else if (defaultGroup != null && defaultArtifact != null) {
            // now we can just use the defaults
            return new DefaultArtifactDescriptor(defaultGroup, defaultArtifact, version,
                    Extension.ZIP, Scope.COMPILE, null);
        }
        else if (defaultGroup != null && name != null && name.contains(":")) {
            name = name.substring(0, name.indexOf(':'));
            return new DefaultArtifactDescriptor(defaultGroup, name, version, Extension.ZIP,
                    Scope.COMPILE, null);
        }

        throw new CommandException(
                "No valid ARTIFACT provided, no default artifact defined and not in local mode.\nPlease specify a valid artifact identifier or run with -l to load your local project.",
                name());
    }
}
