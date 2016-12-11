package com.atomist.rug.cli.command;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;

import com.atomist.project.archive.Operations;
import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.Log;
import com.atomist.rug.cli.templating.TemplateHelpers;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.context.FieldValueResolver;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.context.MethodValueResolver;

public abstract class AbstractTemplatizedCommand extends AbstractCommand {

    private Log log = new Log(getClass());
    private String resultView;
    private Map<String, Object> resultContext = new HashMap<>();

    @Override
    protected final void run(Operations operations, ArtifactDescriptor artifact,
            CommandLine commandLine) {
        doRun(operations, artifact, commandLine);
        mergeModelAndTemplate();
    }

    private void mergeModelAndTemplate() {
        if (resultView == null) {
            return;
        }

        resultContext.put("divider", Constants.DIVIDER);
        resultContext.put("rug", Constants.COMMAND);

        Context context = Context.newBuilder(resultContext)
                .resolver(MapValueResolver.INSTANCE, JavaBeanValueResolver.INSTANCE,
                        MethodValueResolver.INSTANCE, FieldValueResolver.INSTANCE)
                .build();

        Handlebars handlebars = new Handlebars();
        handlebars.registerHelpers(new TemplateHelpers());
        try {
            Template template = handlebars.compile("templates/" + resultView);
            log.info(template.apply(context));
        }
        catch (IOException e) {
            throw new CommandException("Error merging template", e);
        }
    }

    protected abstract void doRun(Operations operations, ArtifactDescriptor artifact,
            CommandLine commandLine);

    protected void setResultView(String template) {
        this.resultView = template;
    }

    protected void addResultContext(String key, Object value) {
        this.resultContext.put(key, value);
    }

}
