package com.atomist.rug.cli.command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;

import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.ArtifactDescriptor.Extension;
import com.atomist.rug.resolver.DefaultArtifactDescriptor;

public abstract class AbstractRugEnabledCommandInfo extends AbstractCommandInfo
        implements ArtifactDescriptorProvider {

    private static final String RUG_LIB_VERSION = ".*<rug-lib.version>(.*)<\\/rug-lib.version>.*";

    public AbstractRugEnabledCommandInfo(Class<? extends Command> commandClass,
            String commandName) {
        super(commandClass, commandName);
    }

    @Override
    public ArtifactDescriptor artifactDescriptor(CommandLine commandLine) {
        String version = readRugVersionFromPom();
        return new DefaultArtifactDescriptor("com.atomist", "rug-lib", version, Extension.JAR);
    }

    protected String readRugVersionFromPom() {
        String version = "latest";
        Pattern pattern = Pattern.compile(RUG_LIB_VERSION);
        try (InputStream is = AbstractRugEnabledCommandInfo.class.getClassLoader()
                .getResourceAsStream("META-INF/maven/com.atomist/rug-cli/pom.xml")) {

            if (is != null) {
                BufferedReader in = new BufferedReader(new InputStreamReader(is));
                String line = null;
                while ((line = in.readLine()) != null) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.matches()) {
                        version = matcher.group(1);
                        break;
                    }
                }
            }
        }
        catch (IOException e) {
            // just use latest as fallback
        }
        return version;
    }

}
