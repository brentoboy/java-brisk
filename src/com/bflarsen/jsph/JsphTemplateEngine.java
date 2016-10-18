package com.bflarsen.jsph;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;


public class JsphTemplateEngine {
    public final ScriptEngine jsEngine;
    public final Invocable jsEngineAsInvocable;
    public final Object jsphObjectInJsEngine;
    public String ViewFolder;

    public JsphTemplateEngine(String viewFolder) throws Exception {
        this.ViewFolder = viewFolder;

        this.jsEngine = new ScriptEngineManager().getEngineByName("nashorn");
        this.jsEngineAsInvocable = (Invocable) this.jsEngine;

        this.jsEngine.eval("var window = window || {};");
        this.jsEngine.eval("var console = console || { log: print };");
        this.jsEngine.eval(JsphTemplateEngine.jsphScript);
        this.jsEngine.eval("var jsph = window.jsph;");
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

    public void addTemplate(String templateFile) throws Exception {
        String templateName = templateFile;
        String templateCode = String.join("\n", Files.readAllLines(Paths.get(this.ViewFolder, templateFile), Charset.forName("UTF8")));
        addTemplate(templateName, templateCode);
    }

    public void addTemplate(String templateName, String templateCode) throws Exception {
        Object renderer = this.jsEngineAsInvocable.invokeMethod(this.jsphObjectInJsEngine, "compile", templateCode);
        this.jsEngineAsInvocable.invokeFunction("setObjProperty", this.jsphObjectInJsEngine, templateName, renderer);
    }

    public void render(String templateName, Object viewModel, Writer writer) throws Exception {
        writer.write(this.render(templateName, viewModel));
    }

    public String render(String templateName, Object viewModel) throws Exception {
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
            "\t\t\t\tconsole.log(err)\n" +
            "\t\t\t\tconsole.log(\"\\n\" + functionBody);\n" +
            "\t\t\t}\n" +
            "\t\t},\n" +
            "\n" +
            "\t\trender: function render(templateString, vars) {\n" +
            "\t\t\tvar renderer = jsph.compile(templateString);\n" +
            "\t\t\treturn renderer(vars);\n" +
            "\t\t},\n" +
            "\t}\n" +
            "\n" +
            "\twindow.jsph = jsph;\n" +
            "}());\n"
    ;
}
