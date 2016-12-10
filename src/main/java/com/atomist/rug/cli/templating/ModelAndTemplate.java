package com.atomist.rug.cli.templating;

import java.util.HashMap;
import java.util.Map;

public class ModelAndTemplate {
    
    private String template;
    
    private Map<String, Object> model;
    
    public ModelAndTemplate(String template) {
        this(template, new HashMap<>());
    }

    public ModelAndTemplate(String template, Map<String, Object> model) {
        this.template = template;
        this.model = model;
    }
    
    public void set(String key, Object value) {
        this.model.put(key, value);
    }
    
    public String template() {
        return template;
    }

    public Map<String, Object> model() {
        return model;
    }
}
