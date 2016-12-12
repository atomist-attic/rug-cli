package com.atomist.rug.cli.templating;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.springframework.util.StringUtils;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.tree.ArtifactSourceTreeCreator;
import com.atomist.rug.cli.tree.LogVisitor;
import com.atomist.rug.cli.utils.FileUtils;
import com.atomist.source.ArtifactSource;
import com.github.jknack.handlebars.Options;

@SuppressWarnings("unchecked")
public class TemplateHelpers {
    
    // Constant helpers
    public CharSequence rug() throws IOException {
        return Constants.COMMAND;
    }
    
    public CharSequence divider() throws IOException {
        return Constants.DIVIDER;
    }
    
    public CharSequence treeNode() throws IOException {
        return Constants.TREE_NODE;
    }

    public CharSequence lastTreeNode() throws IOException {
        return Constants.LAST_TREE_NODE;
    }

    // Logic helpers
    public CharSequence empty(Object obj1, Options options) throws IOException {
        Collection<?> collection = (Collection<?>) obj1;
        return (collection == null || collection.isEmpty()) ? options.fn() : options.inverse();
    }

    // Formatting helpers
    public CharSequence tree(Object obj1) throws IOException {
        StringBuilder sb = new StringBuilder();
        ArtifactSourceTreeCreator.visitTree((ArtifactSource) obj1, new LogVisitor(sb));
        return sb.toString();
    }

    public CharSequence delimitedWithUnderline(Object obj1, Options options) throws IOException {
        List<String> collection = (List<String>) obj1;
        collection.set(0, Style.underline(collection.get(0)));
        return collection.isEmpty() ? ""
                : StringUtils.collectionToDelimitedString(collection, ", ");
    }

    public CharSequence sizeOf(Object obj1, Options options) throws IOException {
        File file = (File) obj1;
        return FileUtils.sizeOf(file);
    }

    public CharSequence realativize(Object obj1, Options options) throws IOException {
        File file = (File) obj1;
        return FileUtils.relativize(file);
    }

    // Style helpers
    public CharSequence cyan(Object obj1, Options options) throws IOException {
        return stylize(obj1, options, Style::cyan);
    }

    public CharSequence blue(Object obj1, Options options) throws IOException {
        return stylize(obj1, options, Style::blue);
    }

    public CharSequence red(Object obj1, Options options) throws IOException {
        return stylize(obj1, options, Style::red);
    }

    public CharSequence yellow(Object obj1, Options options) throws IOException {
        return stylize(obj1, options, Style::yellow);
    }
    
    public CharSequence green(Object obj1, Options options) throws IOException {
        return stylize(obj1, options, Style::green);
    }

    public CharSequence underline(Object obj1, Options options) throws IOException {
        return stylize(obj1, options, Style::underline);
    }

    public CharSequence bold(Object obj1, Options options) throws IOException {
        return stylize(obj1, options, Style::bold);
    }

    private String stylize(Object obj, Options options, Function<String, String> style)
            throws IOException {
        if (!(obj instanceof String)) {
            return style.apply(options.fn().toString());
        }
        return style.apply(obj.toString());
    }
}