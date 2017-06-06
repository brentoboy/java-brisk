package com.bflarsen.scripting;


import com.bflarsen.scripting.JsphTemplateEngine;

public class JsphTemplate<TModel> {

    public JsphTemplateEngine Engine;
    public String TemplateName;

    public JsphTemplate(JsphTemplateEngine engine, String templateName) {
        this.Engine = engine;
        this.TemplateName = templateName;
    }

    public String render(TModel model) throws Exception {
        return this.Engine.render(this.TemplateName, model);
    }
}
