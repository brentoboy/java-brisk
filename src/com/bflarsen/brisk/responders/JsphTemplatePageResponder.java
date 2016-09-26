package com.bflarsen.brisk.responders;

import com.bflarsen.brisk.*;
import com.bflarsen.brisk.responses.BaseBufferedResponse;
import com.bflarsen.util.FileStatCache;
import com.bflarsen.util.Logger;

import java.nio.file.*;
import java.util.*;


public class JsphTemplatePageResponder extends BaseResponder {

    protected String page;
    protected List<String> widgets;

    public JsphTemplatePageResponder(String page_path) {
        try {
            this.page = page_path;
            this.widgets = new ArrayList<>();
        } catch (Exception ex) {
            Logger.logEx(ex, "JsphTemplatePageResponder", "constructor", "");
        }
    }

    public void addWidget(String widget_path) {
        this.widgets.add(widget_path);
    }

    @Override
    public HttpResponse buildResponse() throws Exception {
        // make sure the view folder is setup correctly, and its a valid folder
        FileStatCache cache = http_context.Server.FileCache;
        if (cache == null) {
            throw new Exception("missing file cache!");
        }
        String viewFolder = http_context.Server.ViewFolder;
        if (viewFolder == null || viewFolder.isEmpty()) {
            throw new Exception("Please set up a view folder for your server");
        }
        String page_path = Paths.get(viewFolder, page).toString();

        // make sure that, at the very least there is an html file that matches
        if (!cache.get(page_path + ".html").isReadable) {
            Logger.logError(page_path + ".html does not exist", "JsphTemplatePageResponder", "buildResponse()", Paths.get(".").toAbsolutePath().normalize().toString());
        }

        // TODO: make sure that, for each widget there is an html, or js at the very minimum


        StringBuffer builder = new StringBuffer();
        // load the html file contents
        String html = cache.readString(page_path + ".html");

        // split it into head / body pieces
        String[] pieces = html.split("</head>");
        String head_html = pieces[0];
        html = pieces[1];
        pieces = html.split("</body>");
        String body_html = pieces[0];

        // rebuild the page with all the templates inserted
        builder.append(head_html);
        builder.append(jsphScript);

        // add any widget specific css
        builder.append("<style type='text/css'>\r\n");
        for(String widget : this.widgets) {
            String widget_path = Paths.get(viewFolder, widget).toString();
            if (cache.get(widget_path + ".css").isReadable) {
                builder.append("/* ").append(widget_path).append(" */\r\n");
                builder.append(cache.readString(widget_path + ".css"));
                builder.append("\r\n\r\n");
            }
        }
        builder.append("</style>");

        builder.append("\r\n</head>");
        builder.append(body_html);
        builder.append("\r\n");
        builder.append("\r\n");

        // add html/js scripts for each widget
        for(String widget : this.widgets) {
            String widget_path = Paths.get(viewFolder, widget).toString();
            if (cache.get(widget_path + ".html").isReadable) {
                builder.append("<script id=\"").append(widget.replace("/", "__").replace("\\", "__").replace(":", "_").replace(".", "_")).append("_html\" type=\"text/template\">\r\n");
                builder.append(cache.readString(widget_path + ".html"));
                builder.append("\r\n</script>\r\n");
            }
            else {
                builder.append("<!-- no html for ").append(widget.replace("/", "__").replace("\\", "__").replace(":", "_").replace(".", "_")).append("-->\r\n");
            }
            if (cache.get(widget_path + ".js").isReadable) {
                builder.append("<script id=\"").append(widget.replace("/", "__").replace("\\", "__").replace(":", "_").replace(".", "_")).append("_js\">\r\n");
                builder.append(cache.readString(widget_path + ".js"));
                builder.append("\r\n</script>\r\n");
            }
            builder.append("\r\n\r\n");
        }
        // add the page's js file (if it has one)
        if (cache.get(page_path + ".js").isReadable) {
            builder.append("<script id=\"").append(page.replace("/", "__").replace("\\", "__").replace(":", "_").replace(".", "_")).append("_js\">\r\n");
            builder.append(cache.readString(page_path + ".js"));
            builder.append("\r\n</script>\r\n");
        }
        builder.append("\r\n</body>\r\n</html>");


        return new BaseBufferedResponse(
                HttpStatusCode.OK
                , MimeType.lookupByExtension("html")
                , builder.toString()
        );
    }

    protected static final String jsphScript =
            "<script>\n" +
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
            "}());\n" +
            "</script>\n"
    ;
}
