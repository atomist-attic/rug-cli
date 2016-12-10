package com.atomist.rug.cli.command;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;

import com.atomist.project.archive.Operations;
import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.Log;
import com.atomist.rug.cli.templating.ModelAndTemplate;
import com.atomist.rug.cli.templating.TemplateHelpers;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;

public abstract class AbstractTemplatizedCommand extends AbstractCommand {

    private Log log = new Log(getClass());
    
    @Override
    protected final void run(Operations operations, ArtifactDescriptor artifact,
            CommandLine commandLine) {
        mergeModelAndTemplate(doRun(operations, artifact, commandLine));
    }
    
    private void mergeModelAndTemplate(ModelAndTemplate modelAndTemplate) {
        if (modelAndTemplate == null) {
            return;
        }
         
        modelAndTemplate.set("divider", Constants.DIVIDER);
        modelAndTemplate.set("rug", Constants.COMMAND);

        Handlebars handlebars = new Handlebars();
        handlebars.registerHelpers(new TemplateHelpers());
        try {
            Template template = handlebars.compile("templates/" + modelAndTemplate.template());
            log.info(template.apply(modelAndTemplate.model()));
        }
        catch (IOException e) {
            throw new CommandException("Error merging template", e);
        }
    }
    
    protected abstract ModelAndTemplate doRun(Operations operations, ArtifactDescriptor artifact,
            CommandLine commandLine);

}
