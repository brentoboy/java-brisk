package com.bflarsen.brisk.responders;


import com.bflarsen.brisk.*;
import com.bflarsen.brisk.responses.*;
import com.bflarsen.util.*;

import java.io.File;
import java.nio.file.*;
import java.util.*;

public class BabelPageResponder extends BaseResponder{

    protected String appFolder;
    protected List<String> components;
    protected List<String> rawScripts;

    public BabelPageResponder(String appFolder) {
        try {
            this.appFolder = appFolder;
            this.components = new ArrayList<>();
            this.rawScripts = new ArrayList<>();
            this.addComponent(this.appFolder);
        } catch (Exception ex) {
            Logger.logEx(ex, "BabelPageResponder", "constructor", "");
        }
    }

    public void addComponent(String component_path) throws Exception {
        if (! this.components.contains(component_path)) {
            this.components.add(component_path);
        }
    }

    private void addComponentTree(FileStatCache cache, String viewFolder, String base_path) throws Exception {
        String path = Paths.get(viewFolder, base_path).toString();
        if (cache.get(path).isDirectory) {
            File[] directories = new File(path).listFiles(File::isDirectory); // should probably do this from cache...
            if (directories != null) {
                for (File subDir : directories) {
                    this.addComponent(base_path + "/" + subDir.getName());
                }
            }
            if (cache.get(path + "/requires.txt").exists) {
                String[] includeLines = cache.readString(path + "/requires.txt").split("\\r\\n|\\n|\\r");
                for (String include : includeLines) {
                    String include_path = include.trim();
                    if (include_path.length() > 0) {
                        this.addComponent(include_path);
                    }
                }
            }
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
        String html_file = Paths.get(viewFolder, appFolder, "wireframe.html").toString();

        // make sure that, at the very least there is an html file that matches
        if (!cache.get(html_file).isReadable) {
            throw new Exception(html_file + " does not exist");
        }

        // recursively add all the component sub folders
        for (int i = 0; i < this.components.size(); i++) {
            this.addComponentTree(cache, viewFolder, this.components.get(i));
        }

        StringBuilder builder = new StringBuilder();

        // load the html file contents
        String html = cache.readString(html_file);

        // split it into head / body pieces
        String[] pieces = html.split("</head>");
        if (pieces.length < 2) {
            throw new Exception(html_file + " does not look like a valid html appFolder (with head / body elements)");
        }
        String head_html = pieces[0];
        html = pieces[1];
        pieces = html.split("</body>");
        String body_html = pieces[0];

        // rebuild the appFolder with all the templates inserted
        builder.append(head_html);

        // add any component specific css
        builder.append("<style type='text/css'>\r\n");
        for(String component : this.components) {
            // inside of each component folder, add *.css
            String component_path = Paths.get(viewFolder, component).toString();
            if (cache.get(component_path).isDirectory) {
                File[] files = new File(component_path)
                        .listFiles((x) -> x.isFile() && x.getName().endsWith(".css"))
                ;
                if (files != null) {
                    for (File file : files) {
                        appendCss(builder, cache, viewFolder, component + "/" + file.getName());
                    }
                }
            }
        }
        builder.append("</style>");

        builder.append("\r\n</head>");
        builder.append(body_html);
        builder.append("\r\n");
        builder.append("\r\n");

        // add js/jsx scripts for each components
        for(String component : this.components) {
            // some "components" are a file, others are a folder
            // so, add component.js and component.jsx  (if they exist)
            // and, add component/*.js and component/*.jsx
            String component_path = Paths.get(viewFolder, component).toString();
            if (cache.get(component_path).isDirectory) {
                File[] files = new File(component_path)
                        .listFiles((x) -> x.isFile() && (x.getName().endsWith(".js") || x.getName().endsWith(".jsx")))
                ;
                if (files != null) {
                    for (File file : files) {
                        appendJs(builder, cache, viewFolder, component + "/" + file.getName());
                    }
                }
            }
            else {
                if (cache.get(component_path + ".js").isReadable) {
                    appendJs(builder, cache, viewFolder, component + ".js");
                }
                if (cache.get(component_path + ".jsx").isReadable) {
                    appendJs(builder, cache, viewFolder, component + ".jsx");
                }
            }
            builder.append("\r\n\r\n");
        }

        for (String script : this.rawScripts) {
            appendRawJs(builder, script);
        }

        builder.append("\r\n</body>\r\n</html>");

        return new BaseBufferedResponse(
                HttpStatusCode.OK
                , MimeType.lookupByExtension("html")
                , builder.toString()
        );
    }

    private void appendCss(StringBuilder builder, FileStatCache cache, String viewFolder, String relative_path) throws Exception {
        builder.append("/* ./").append(relative_path).append(" */\r\n");
        if (cache.get(viewFolder + "/" + relative_path).isReadable) {
            builder.append(cache.readString(viewFolder + "/" + relative_path));
        }
        builder.append("\r\n/* END: ./").append(relative_path).append(" */\r\n")
                .append("\r\n\r\n");
    }

    private void appendJs(StringBuilder builder, FileStatCache cache, String viewFolder, String relative_path) throws Exception {
        // String widget_identifier = relative_path.replace("/", "__").replace("\\", "__").replace(":", "_").replace(".", "_");
        builder.append("<!-- ./").append(relative_path).append(" -->\r\n");
        if (cache.get(viewFolder + "/" + relative_path).isReadable) {
            builder.append("<script type=\"text/babel\">\r\n")
                    .append(cache.readString(viewFolder + "/" + relative_path))
                    .append("\r\n</script>\r\n");
        }
    }

    private void appendRawJs(StringBuilder builder, String rawJs) {
        builder.append("<script type=\"text/babel\">\r\n")
                .append(rawJs)
                .append("\r\n</script>\r\n")
                .append("\r\n\r\n");
    }
}
