package com.atomist.rug.cli.templating;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.springframework.util.StringUtils;

import com.atomist.rug.cli.output.Style;
import com.github.jknack.handlebars.Options;

@SuppressWarnings("unchecked")
public class TemplateHelpers {
    
    public CharSequence empty(Object obj1, Options options) throws IOException {
        Collection<?> collection = (Collection<?>) obj1;
        return collection.isEmpty() ? options.fn() : options.inverse();
    }

    public CharSequence delimitedWithUnderline(Object obj1, Options options) throws IOException {
        List<String> collection = (List<String>) obj1;
        collection.set(0, Style.underline(collection.get(0)));
        return collection.isEmpty() ? ""
                : StringUtils.collectionToDelimitedString(collection, ", ");
    }

    public CharSequence cyan(Object obj1) throws IOException {
        return Style.cyan(obj1.toString());
    }

    public CharSequence bold(Object obj1, Options options) throws IOException {
        return Style.bold(options.param(0));
    }

    public CharSequence underline(Object obj1) throws IOException {
        return Style.underline(obj1.toString());
    }

    public CharSequence yellow(Object obj1) throws IOException {
        return Style.yellow(obj1.toString());
    }
}