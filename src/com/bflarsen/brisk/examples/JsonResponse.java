package com.bflarsen.brisk.examples;

//import com.bflarsen.brisk.HttpContext;
//import com.bflarsen.brisk.HttpServer;
//import com.bflarsen.brisk.responses.BaseBufferedResponse;
//
//public class JsonResponse extends BaseBufferedResponse {
//
//    public Object Payload;
//    public HttpServer Server;
//
//    public JsonResponse(Object payload, HttpContext context) {
//        super(200);
//        this.Payload = payload;
//        this.Server = context.Server;
//        this.setHeader("Content-Type", "application/json");
//        this.setHeader("Cache-Control", "no-statCache, private, must-revalidate, s-maxage=0, max-age=0");
//    }
//
//    @Override
//    public byte[] getBodyBytes() throws Exception {
//        return this.Server.encodeJson(this.Payload).getBytes(UTF8);
//    }
//}
