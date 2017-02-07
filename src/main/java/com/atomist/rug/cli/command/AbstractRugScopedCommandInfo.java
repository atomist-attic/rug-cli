package com.atomist.rug.cli.command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;

import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.ArtifactDescriptor.Extension;
import com.atomist.rug.resolver.ArtifactDescriptorFactory;
import com.atomist.rug.resolver.DefaultArtifactDescriptor;

public abstract class AbstractRugScopedCommandInfo extends AbstractLocalArtifactDescriptorProvider
        implements ArtifactDescriptorProvider {

    private static final String RUG_VERSION = ".*<rug.version>(.*)<\\/rug.version>.*";

    public AbstractRugScopedCommandInfo(Class<? extends Command> commandClass, String commandName) {
        super(commandClass, commandName);
    }

    @Override
    public ArtifactDescriptor artifactDescriptor(CommandLine commandLine) {
        Optional<ArtifactDescriptor> artifact = localArtifactDescriptor(commandLine);
        String version = readRugVersionFromPom();
        if (artifact.isPresent()) {
            // We are not interested in the operations right now; we only need the classpath
            ArtifactDescriptor ad = artifact.get();
            return ArtifactDescriptorFactory.copyFrom(ad, ad.group(), ad.artifact(), ad.version(),
                    Extension.JAR);
        }
        else {
            version = readRugVersionFromPom();
        }
        return new DefaultArtifactDescriptor("com.atomist", "rug", version, Extension.JAR);
    }

    protected String readRugVersionFromPom() {
        String version = "latest";
        Pattern pattern = Pattern.compile(RUG_VERSION);
        try (InputStream is = AbstractRugScopedCommandInfo.class.getClassLoader()
                .getResourceAsStream("META-INF/maven/com.atomist/rug-cli/pom.xml")) {
            if (is != null) {
                BufferedReader in = new BufferedReader(new InputStreamReader(is));
                String line;
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
