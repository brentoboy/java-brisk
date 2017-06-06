package com.bflarsen.scripting;


import java.nio.file.Path;

public class BabelTranspiler implements AutoCloseable {

    JavascriptEngine jsEngine;

    public BabelTranspiler(Path babelJsFile) throws Exception {
        jsEngine = new JavascriptEngine();
        jsEngine.evalFile(babelJsFile);
    }

    public String transform(String inputCode) throws Exception {
        jsEngine.setValue("babelInput", inputCode);
        String outputCode = jsEngine.eval("Babel.transform(babelInput, { presets: ['react', 'es2015'] }).code").toString();
        jsEngine.setValue("babelInput", null);
        return outputCode;
    }

    @Override
    public void close() throws Exception {
        jsEngine.close();
    }
}
