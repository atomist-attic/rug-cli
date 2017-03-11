package com.atomist.rug.cli.command;

import static com.atomist.rug.cli.command.MkDocs.compareOptions;
import static com.atomist.rug.cli.command.MkDocs.formatCommand;
import static com.atomist.rug.cli.command.MkDocs.formatOption;
import static com.atomist.rug.cli.command.MkDocs.markdownDocs;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.junit.Test;

import com.atomist.rug.cli.command.publish.PublishCommand;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.LocalArtifactDescriptor;

public class MkDocsTest {

    @Test
    public void testSortShortOptions() {
        Option a = new Option("a", "z a option");
        Option b = new Option("b", "y b option");
        assert(compareOptions(a, b) == -1);
        assert(compareOptions(b, a) == 1);
        assert(compareOptions(a, a) == 0);
    }

    @Test
    public void testSortLongOptions() {
        Option a = Option.builder().longOpt("a-long-opt").desc("z long a").build();
        Option b = Option.builder().longOpt("b-long-opt").desc("y long b").build();
        assert(compareOptions(a, b) == -1);
        assert(compareOptions(b, a) == 1);
        assert(compareOptions(a, a) == 0);
    }

    @Test
    public void testSortMixedOptions() {
        Option aShort = new Option("a", "z a option");
        Option bShort = new Option("b", "y b option");
        Option aLong = Option.builder().longOpt("a-long-opt").desc("z long a").build();
        Option bLong = Option.builder().longOpt("b-long-opt").desc("y long b").build();
        assert(compareOptions(aShort, bLong) == -1);
        assert(compareOptions(bLong, aShort) == 1);
        assert(compareOptions(aLong, bShort) == -1);
        assert(compareOptions(bShort, aLong) == 1);
    }

    @Test
    public void testFormatShortOption() {
        String name = "a";
        String desc = "the description of short option a";
        Option o = new Option(name, desc);
        String optionDoc = formatOption(o);
        assert(optionDoc.contains("`-" + name + "`\n"));
        assert(optionDoc.contains(":   " + desc));
    }

    @Test
    public void testFormatLongOption() {
        String name = "a-long-opt";
        String desc = "the description of long option a-long-opt";
        Option o = Option.builder().longOpt(name).desc(desc).build();
        String optionDoc = formatOption(o);
        assert(optionDoc.contains("`--" + name + "`\n"));
        assert(optionDoc.contains(":   " + desc));
    }

    @Test
    public void testFormatOption() {
        String shortName = "a";
        String longName = "a-long-opt";
        String desc = "the description of long option a-long-opt";
        Option o = new Option(shortName, longName, false, desc);
        String optionDoc = formatOption(o);
        assert(optionDoc.contains("`-" + shortName + "`"));
        assert(optionDoc.contains("`--" + longName + "`"));
        assert(optionDoc.contains(":   " + desc));
    }

    @Test
    public void testFormatShortOptionArgument() {
        String name = "a";
        String desc = "the description of long option a-long-opt";
        String arg = "ARG";
        Option o = Option.builder(name).desc(desc).argName(arg).hasArg(true).build();
        String optionDoc = formatOption(o);
        assert(optionDoc.contains("`-" + name + " " + arg + "`\n"));
        assert(optionDoc.contains(":   " + desc));
    }

    @Test
    public void testFormatLongOptionArgument() {
        String name = "a-long-opt";
        String desc = "the description of long option a-long-opt";
        String arg = "ARG";
        Option o = Option.builder().longOpt(name).desc(desc).argName(arg).hasArg(true).build();
        String optionDoc = formatOption(o);
        assert(optionDoc.contains("`--" + name + "=" + arg + "`\n"));
        assert(optionDoc.contains(":   " + desc));
    }

    @Test
    public void testFormatCommand() {
        String cmdName = "test-publish";
        String cmdDescription = "Create and publish an archive into a remote repository";
        String cmdDetail = "Create a Rug archive from the current repo and publish it in a remote repository.  "
                + "Ensure that there is a manifest.yml descriptor in the .atomist directory.  "
                + "Use -i to specify what repository configuration should be used to publish.  "
                + "ID should refer to a repository name in cli.yml";
        String agOpt = "archive-group";
        String agOptDesc = "Override archive group with AG";
        String avOpt = "archive-version";
        String avOptDesc = "Override archive version with AV";

        class TestCommandInfo extends AbstractLocalArtifactDescriptorProvider
                implements CommandInfo {

            public TestCommandInfo() {
                super(PublishCommand.class, cmdName);
            }

            @Override
            public String description() {
                return cmdDescription;
            }

            @Override
            public String detail() {
                return cmdDetail;
            }

            @Override
            public Options options() {
                Options options = new Options();
                options.addOption(Option.builder().longOpt(agOpt).argName("AG").hasArg(true)
                        .required(false).desc(agOptDesc).build());
                options.addOption(Option.builder("a").longOpt(avOpt).argName("AV").hasArg(true)
                        .required(false).desc(avOptDesc).build());
                options.addOption(Option.builder("i").longOpt("id").argName("ID").hasArg(true)
                        .required(false).desc("ID identifying the repository to publish into").build());

                return options;
            }

            @Override
            public int order() {
                return 70;
            }

            @Override
            public String usage() {
                return "publish [OPTION]...";
            }

            @Override
            public String group() {
                return "2";
            }

            @Override
            public boolean enabled(ArtifactDescriptor artifact) {
                return artifact instanceof LocalArtifactDescriptor
                        || artifact.extension().equals(ArtifactDescriptor.Extension.ZIP);
            }
        }

        CommandInfo cmdInfo = new TestCommandInfo();
        String doc = formatCommand(cmdInfo, "##");
        assert(doc.contains("# `" + cmdName + "`"));
        assert(doc.contains(cmdDescription));
        assert(doc.contains(cmdDetail));
        assert(doc.contains("`--" + agOpt + "=AG`"));
        assert(doc.contains(agOptDesc));
        assert(doc.contains("`--" + avOpt + "=AV`"));
        assert(doc.contains(avOptDesc));
    }

    @Test
    public void testMarkdownDocs() {
        ServiceLoadingCommandInfoRegistry commandInfoRegistry = new ServiceLoadingCommandInfoRegistry();
        String docs = markdownDocs(commandInfoRegistry);
        assert(docs.contains("# Global command-line options\n"));
        assert(docs.contains("# Commands\n"));
    }
}
