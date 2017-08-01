package com.bflarsen.scripting;

import com.bflarsen.util.FileStatCache;
import com.bflarsen.util.Logger;

import java.io.*;
import java.net.URL;
import java.util.*;


public class JsphTemplateEngine extends JavascriptEngine {

    public String ViewFolder;
    public final FileStatCache fileCache;
    public final Object jsphObjectInJsEngine;

    public final Map<String, Long> updateStampWhenLoaded = new HashMap<>();

    public JsphTemplateEngine(String viewFolder, Object gson) throws Exception {
        super(gson);
        this.ViewFolder = viewFolder;
        this.fileCache = new FileStatCache();
        try(InputStream resource = getClass().getClassLoader().getResourceAsStream("com/bflarsen/scripting/jsph.js")) {
            try (Reader reader = new InputStreamReader(resource)) {
                this.eval(reader);
            }
        }
        this.jsphObjectInJsEngine = this.getValue("jsph");
        this.eval("var sql = Java.type('com.bflarsen.scripting.SqlHelpers');");
    }

    public <TModel> JsphTemplate<TModel> addTemplateOrUpdateIfModified(String templateFile) throws Exception {
        String templateName = templateFile;
        String path = getFullPath(templateFile);
        if (!fileCache.get(path).exists) {
            Logger.logWarning("File not found: " + path, "JsphTemplateEngine", "addTemplateOrUpdateIfModified", "loading content");
        }
        Long updateStamp = updateStampWhenLoaded.get(path);
        if (updateStamp == null || updateStamp < fileCache.get(path).whenModified) {
            if (updateStamp != null) {
                Logger.logWarning("Reloading modified template: " + templateFile, "JsphTemplateEngine", "addTemplateOrUpdateIfModified", "checking update stamp");
            }
            updateStampWhenLoaded.put(path, fileCache.get(path).whenModified);
            String templateCode = fileCache.readString(path).replace("\r\n", "\n");
            return addOrUpdateTemplate(templateName, templateCode);
        }
        return null;
    }

    public <TModel> JsphTemplate<TModel> addTemplate(String templateFile) throws Exception {
        String path = getFullPath(templateFile);
        Long updateStamp = updateStampWhenLoaded.get(path);
        if (updateStamp == null) {
            return addTemplateOrUpdateIfModified(templateFile);
        }
        return null;
    }

    public String getFullPath(String templateFile) {
        if (this.ViewFolder.endsWith("/") || templateFile.startsWith("/")) {
            return this.ViewFolder + templateFile;
        }
        else {
            return this.ViewFolder + "/" + templateFile;
        }
    }

    public <TModel> JsphTemplate<TModel> addOrUpdateTemplate(String templateName, String templateCode) throws Exception {
        Object renderer = this.runMethod("jsph", "compile", templateCode);
        this.setProperty(this.jsphObjectInJsEngine, templateName, renderer);
        return new JsphTemplate<>(this, templateName);
    }

    public void render(String templateName, Object viewModel, Writer writer) throws Exception {
        writer.write(this.render(templateName, viewModel));
    }

    public String render(String templateName, Object viewModel) throws Exception {
        addTemplateOrUpdateIfModified(templateName);
        return this.runMethod("jsph", templateName, viewModel).toString();
    }
}
