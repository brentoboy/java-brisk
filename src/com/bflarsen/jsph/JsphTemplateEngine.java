package com.bflarsen.jsph;

import com.bflarsen.util.FileStatCache;
import com.bflarsen.util.Logger;

import javax.script.*;
import java.io.Writer;
import java.nio.file.*;
import java.util.*;


public class JsphTemplateEngine {
    public final FileStatCache fileCache;
    public final ScriptEngine jsEngine;
    public final Invocable jsEngineAsInvocable;
    public final Object jsphObjectInJsEngine;
    public String ViewFolder;

    public final Map<String, Long> updateStampWhenLoaded = new HashMap<>();

    public JsphTemplateEngine(String viewFolder) throws Exception {
        this.ViewFolder = viewFolder;

        this.fileCache = new FileStatCache();

        this.jsEngine = new ScriptEngineManager().getEngineByName("nashorn");
        this.jsEngineAsInvocable = (Invocable) this.jsEngine;

        this.jsEngine.eval("var window = window || {};");
        this.jsEngine.eval("var console = console || { log: print };");
        this.jsEngine.eval(JsphTemplateEngine.jsphScript);
        this.jsEngine.eval("var jsph = window.jsph;");
        this.jsEngine.eval("var sql = Java.type('com.bflarsen.jsph.JsphSqlEngine');");
        this.jsEngine.eval(
                "function setObjProperty(obj, prop, val) {\n" +
                "    obj[prop] = val;\n" +
                "}");
        this.jsEngine.eval(
                "function coalesce() {\n" +
                "    var len = arguments.length;\n" +
                "    for (var i=0; i<len; i++) {\n" +
                "        if (arguments[i] !== null && arguments[i] !== undefined) {\n" +
                "            return arguments[i];\n" +
                "        }\n" +
                "    }\n" +
                "    return null;\n" +
                "}"
        );


        this.jsphObjectInJsEngine = this.jsEngine.get("jsph");
    }

    public void addGsonStringify(Object gson) throws Exception {
        this.jsEngine.put("GSON", gson);
        this.jsEngine.eval("JSON.originalStringify = JSON.stringify;");
        this.jsEngine.eval("JSON.stringify = function replacementStringify(obj) { var result = JSON.originalStringify(obj); if (result == undefined) return GSON.toJson(obj); else return result; }");
    }

    public <TModel> JsphTemplate<TModel> addTemplateOrUpdateIfModified(String templateFile) throws Exception {
        String templateName = templateFile;
        String path = getFullPath(templateFile);
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
        Object renderer = this.jsEngineAsInvocable.invokeMethod(this.jsphObjectInJsEngine, "compile", templateCode);
        this.jsEngineAsInvocable.invokeFunction("setObjProperty", this.jsphObjectInJsEngine, templateName, renderer);
        return new JsphTemplate<TModel>(this, templateName);
    }

    public void render(String templateName, Object viewModel, Writer writer) throws Exception {
        writer.write(this.render(templateName, viewModel));
    }

    public String render(String templateName, Object viewModel) throws Exception {
        addTemplateOrUpdateIfModified(templateName);
        return this.jsEngineAsInvocable.invokeMethod(this.jsphObjectInJsEngine, templateName, viewModel).toString();
    }

    public static final String jsphScript =
            "(function () {\n" +
            "\tvar nullTemplate = function() { return ''; };\n" +
            "\n" +
            "\tvar jsph = {\n" +
            "\t\tcompile: function compile(templateString) {\n" +
            "\t\t\tif (!templateString) {\n" +
            "\t\t\t\treturn nullTemplate;\n" +
            "\t\t\t}\n" +
            "\n" +
            "\t\t\ttemplateString = \"?>\" + templateString + \"<?\";\n" +
            "\n" +
            "\t\t\tvar inHtml = /[\\?\\%]>=?[\\s\\S]*?<[\\?\\%]/mgi;\n" +
            "\t\t\tvar inJs = /<[\\?\\%]=?[\\s\\S]*?<?[\\?\\%]>/mgi;\n" +
            "\n" +
            "\t\t\tvar functionBody = \"\\\n" +
            "(function(jsph) { \\n\\\n" +
            "\treturn function(vars) { \\n\\\n" +
            "\tvar model = vars; \\n\\\n" +
            "\tfor(var varName in model) { \\n\\\n" +
            "\t\ttry { eval('var ' + varName + '=model[varName];') } \\n\\\n" +
            "\t\tcatch(ex){} \\n\\\n" +
            "\t} \\n\\\n" +
            "\t\treturn (function() { \\n\\\n" +
            "\t\t\tvar o = \\\"\\\";\\n\";\n" +
            "\n" +
            "\t\t\thtmlMatch = inHtml.exec(templateString);\n" +
            "\t\t\tjsMatch = inJs.exec(templateString);\n" +
            "\n" +
            "\t\t\twhile (htmlMatch !== null || jsMatch !== null)\n" +
            "\t\t\t{\n" +
            "\t\t\t\tvar matchStartsAt, matchLen;\n" +
            "\n" +
            "\t\t\t\tif (htmlMatch !== null)\n" +
            "\t\t\t\t{\n" +
            "\t\t\t\t\tmatchStartsAt = htmlMatch['index'];\n" +
            "\t\t\t\t\tmatchLen = htmlMatch[0].length;\n" +
            "\t\t\t\t\thtmlMatch = htmlMatch[0].substring(2, matchLen - 2);\n" +
            "\n" +
            "\t\t\t\t\tif (htmlMatch !== \"\") {\n" +
            "\t\t\t\t\t\tfunctionBody += \"o += \" + JSON.stringify(htmlMatch) + \";\\n\";\n" +
            "\t\t\t\t\t}\n" +
            "\n" +
            "\t\t\t\t\thtmlMatch = inHtml.exec(templateString);\n" +
            "\t\t\t\t}\n" +
            "\n" +
            "\t\t\t\tif (jsMatch !== null)\n" +
            "\t\t\t\t{\n" +
            "\t\t\t\t\tjsMatch = jsMatch[0].substring(2, jsMatch[0].length -2);\n" +
            "\n" +
            "\t\t\t\t\tif (/^=/.test(jsMatch)) {\n" +
            "\t\t\t\t\t\tfunctionBody += \"o += (\" + jsMatch.substring(1).trim() + \");\\n\";\n" +
            "\t\t\t\t\t}\n" +
            "\t\t\t\t\telse {\n" +
            "\t\t\t\t\t\tfunctionBody += jsMatch + \"\\n\";\n" +
            "\t\t\t\t\t}\n" +
            "\n" +
            "\t\t\t\t\tjsMatch = inJs.exec(templateString);\n" +
            "\t\t\t\t}\n" +
            "\t\t\t\telse if (matchStartsAt && matchLen && (matchStartsAt + matchLen < templateString.length)) {\n" +
            "\t\t\t\t\t// they left off the closing tag, treat the rest like a js script\n" +
            "\t\t\t\t\tjsMatch = templateString.substring(matchStartsAt + matchLen, templateString.length - 2);\n" +
            "\t\t\t\t\tfunctionBody += jsMatch + \"\\n\";\n" +
            "\t\t\t\t\tjsMatch = null;\n" +
            "\t\t\t\t}\n" +
            "\t\t\t}\n" +
            "\n" +
            "\t\t\tfunctionBody += \"\\\n" +
            "\t\t\t return o;\\n\\\n" +
            "\t\t}).call(vars); \\n\\\n" +
            "\t} \\n\\\n" +
            "})(jsph)\";\n" +
            "\t\t\ttry {\n" +
            "\t\t\t\tvar fn = eval(functionBody);\n" +
            "\t\t\t\treturn fn;\n" +
            "\t\t\t} catch(err) {\n" +
            "\t\t\t\tthrow err.message + \"\\n\" + functionBody;\n" +
            "\t\t\t}\n" +
            "\t\t},\n" +
            "\n" +
            "\t\trender: function render(templateString, vars) {\n" +
            "\t\t\tvar renderer = jsph.compile(templateString);\n" +
            "\t\t\treturn renderer(vars);\n" +
            "\t\t},\n" +
            "\n" +
            "\t\tcreateModelFactory: function createModelFactory(code) {\n" +
            "\t\t\tif (code === null || code == undefined) {\n" +
            "\t\t\t\treturn function() { return null; };\n" +
            "\t\t\t}\n" +
            "\t\t\tif (typeof code != \"string\") {\n" +
            "\t\t\t\tcode = JSON.stringify(code);\n" +
            "\t\t\t}\n" +
            "\t\t\treturn function() { eval(\"var inst = \" + code + \";\"); return inst; }\n" +
            "\t\t},\n" +
            "\t}\n" +
            "\n" +
            "\twindow.jsph = jsph;\n" +
            "}());\n"
    ;
}
