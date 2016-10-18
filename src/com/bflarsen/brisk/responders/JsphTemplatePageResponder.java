package com.bflarsen.brisk.responders;

import com.bflarsen.brisk.*;
import com.bflarsen.brisk.responses.BaseBufferedResponse;
import com.bflarsen.jsph.JsphTemplateEngine;
import com.bflarsen.util.FileStatCache;
import com.bflarsen.util.Logger;

import java.nio.file.*;
import java.util.*;


public class JsphTemplatePageResponder extends BaseResponder {

    protected String page;
    protected List<String> widgets;
    protected List<String> rawScripts;

    public JsphTemplatePageResponder(String page_path) {
        try {
            this.page = page_path;
            this.widgets = new ArrayList<>();
            this.rawScripts = new ArrayList<>();
        } catch (Exception ex) {
            Logger.logEx(ex, "JsphTemplatePageResponder", "constructor", "");
        }
    }

    public void addWidget(String widget_path) throws Exception {
        if (this.widgets.contains(widget_path)) {
            throw new Exception("already contains " + widget_path);
        }
        else {
            this.widgets.add(widget_path);
        }
    }

    public void addScript(String js_code) {
        this.rawScripts.add(js_code);
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


        StringBuilder builder = new StringBuilder();
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
        builder.append("<script>\n" + JsphTemplateEngine.jsphScript + "</script>");


        // add any widget specific css
        builder.append("<style type='text/css'>\r\n");
        for(String widget : this.widgets) {
            System.out.println(widget);
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
        for (String script : this.rawScripts) {
            builder.append("<script>\r\n");
            builder.append(script);
            builder.append("\r\n</script>\r\n");
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
}
