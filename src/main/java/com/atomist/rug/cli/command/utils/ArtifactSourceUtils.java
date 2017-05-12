package com.atomist.rug.cli.command.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

import org.apache.commons.io.IOUtils;

import com.atomist.rug.cli.RunnerException;
import com.atomist.rug.cli.command.CommandException;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.source.Artifact;
import com.atomist.source.ArtifactSource;
import com.atomist.source.DirectoryArtifact;
import com.atomist.source.EmptyArtifactSource;
import com.atomist.source.FileArtifact;
import com.atomist.source.StringFileArtifact;
import com.atomist.source.file.FileSystemArtifactSource;
import com.atomist.source.file.SimpleFileSystemArtifactSourceIdentifier;
import com.atomist.source.file.ZipFileArtifactSourceReader;
import com.atomist.source.file.ZipFileInput;
import com.atomist.source.filter.ArtifactFilter;
import com.atomist.source.filter.AtomistIgnoreFileFilter;
import com.atomist.source.filter.GitDirFilter;

import scala.collection.JavaConverters;
import scala.runtime.AbstractFunction1;

public abstract class ArtifactSourceUtils {

    public static ArtifactSource filterMetaInf(ArtifactSource source) {
        return source.filter(new AbstractFunction1<DirectoryArtifact, Object>() {
            @Override
            public Object apply(DirectoryArtifact dir) {
                // This is required to remove our maven packaging information
                if (dir.name().equals("META-INF")) {
                    Optional<Artifact> nonMavenArtifact = JavaConverters
                            .asJavaCollectionConverter(dir.artifacts()).asJavaCollection().stream()
                            .filter(a -> !a.path().startsWith("META-INF/maven")).findAny();
                    return nonMavenArtifact.isPresent();
                }
                return (!dir.path().equals("META-INF/maven"));
            }
        }, new AbstractFunction1<FileArtifact, Object>() {
            @Override
            public Object apply(FileArtifact arg0) {
                return true;
            }
        });
    }

    public static ArtifactSource createManifestOnlyArtifactSource(File root) {
        ArtifactSource source = new EmptyArtifactSource(root.getName());
        File manifest = new File(root, ".atomist" + File.separator + "manifest.yml");
        if (manifest.exists()) {
            try (InputStream is = new FileInputStream(manifest)) {
                FileArtifact manifestFileArtifact = new StringFileArtifact("manifest.yml",
                        ".atomist", IOUtils.toString(is, StandardCharsets.UTF_8));
                source = source.plus(manifestFileArtifact);
            }
            catch (FileNotFoundException e) {
                throw new CommandException("Error reading manifest.yml.", e);
            }
            catch (IOException e) {
                throw new CommandException("Error reading manifest.yml.", e);
            }
        }
        File packageJson = new File(root, ".atomist" + File.separator + "package.json");
        if (packageJson.exists()) {
            try (InputStream is = new FileInputStream(packageJson)) {
                FileArtifact packageJsonFileArtifact = new StringFileArtifact("package.json",
                        ".atomist", IOUtils.toString(is, StandardCharsets.UTF_8));
                source = source.plus(packageJsonFileArtifact);
            }
            catch (FileNotFoundException e) {
                throw new CommandException("Error reading package.json.", e);
            }
            catch (IOException e) {
                throw new CommandException("Error reading package.json.", e);
            }
        }
        return source;
    }

    public static ArtifactSource createArtifactSource(File root) {
        return new FileSystemArtifactSource(new SimpleFileSystemArtifactSourceIdentifier(root),
                Arrays.asList(new GitDirFilter(root.getPath())));
    }

    public static ArtifactSource createArtifactSource(ArtifactDescriptor artifact) {
        try {
            File archiveRoot = new File(artifact.uri());
            if (archiveRoot.isFile()) {
                return ZipFileArtifactSourceReader
                        .fromZipSource(new ZipFileInput(new FileInputStream(archiveRoot)));
            }
            else {
                return new FileSystemArtifactSource(
                        new SimpleFileSystemArtifactSourceIdentifier(archiveRoot),
                        Arrays.asList(new GitDirFilter(archiveRoot.getPath()),
                                new AtomistIgnoreFileFilter(archiveRoot.getPath()),
                                new TargetDirFilter(archiveRoot)));
            }
        }
        catch (FileNotFoundException e) {
            throw new RunnerException(e);
        }
    }

    private static class TargetDirFilter implements ArtifactFilter {

        private String prefix;

        public TargetDirFilter(File root) {
            this.prefix = root.getAbsolutePath() + File.separator + ".atomist" + File.separator
                    + "target";
        }

        @Override
        public boolean apply(String path) {
            return !path.startsWith(prefix);
        }

    }
}
