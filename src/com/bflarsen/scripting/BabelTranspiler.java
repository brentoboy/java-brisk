package com.bflarsen.scripting;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;


public class BabelTranspiler implements AutoCloseable {

    JavascriptEngine jsEngine;

    public BabelTranspiler() throws Exception {
        jsEngine = new JavascriptEngine();
        jsEngine.evalResourceAsync("com/bflarsen/scripting/babel.js");
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
