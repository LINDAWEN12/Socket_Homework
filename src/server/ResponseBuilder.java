package server;

import shared.HttpConstants;
import shared.HttpUtils;
import java.io.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ResponseBuilder {
    
    public static void build(HttpResponse response, OutputStream out) throws IOException {
        StringBuilder headerBuilder = new StringBuilder();
        
        // 状态行
        headerBuilder.append(HttpConstants.HTTP_VERSION)
                    .append(" ")
                    .append(response.getStatusCode())
                    .append(" ")
                    .append(HttpConstants.STATUS_MESSAGES.get(response.getStatusCode()))
                    .append(HttpConstants.CRLF);
        
        // 默认头部
        response.setHeaderIfAbsent("Server", "SimpleJavaHTTPServer/1.0");
        response.setHeaderIfAbsent("Date", new Date().toString());
        response.setHeaderIfAbsent("Connection", "keep-alive");
        
        // 写入头部
        for (Map.Entry<String, String> header : response.getHeaders().entrySet()) {
            headerBuilder.append(header.getKey())
                        .append(": ")
                        .append(header.getValue())
                        .append(HttpConstants.CRLF);
        }
        
        headerBuilder.append(HttpConstants.CRLF);
        
        // 发送头部
        out.write(headerBuilder.toString().getBytes(HttpConstants.DEFAULT_CHARSET));
        
        // 发送响应体（如果有）
        if (response.getBody() != null && response.getBody().length > 0) {
            out.write(response.getBody());
        }
        
        out.flush();
    }
    
    public static HttpResponse buildFileResponse(String path) {
        File file = new File("webroot" + path);
        
        if (!file.exists() || file.isDirectory()) {
            return buildErrorResponse(HttpConstants.STATUS_NOT_FOUND);
        }
        
        try {
            FileInputStream fis = new FileInputStream(file);
            byte[] content = new byte[(int) file.length()];
            fis.read(content);
            fis.close();
            
            HttpResponse response = new HttpResponse(HttpConstants.STATUS_OK);
            response.setBody(content);
            response.setHeader("Content-Type", HttpUtils.getMimeType(path));
            response.setHeader("Content-Length", String.valueOf(content.length));
            
            return response;
            
        } catch (IOException e) {
            return buildErrorResponse(HttpConstants.STATUS_INTERNAL_ERROR);
        }
    }
    
    public static HttpResponse buildJsonResponse(int statusCode, String json) {
        HttpResponse response = new HttpResponse(statusCode);
        response.setBody(json.getBytes());
        response.setHeader("Content-Type", "application/json");
        response.setHeader("Content-Length", String.valueOf(json.length()));
        return response;
    }
    
    public static HttpResponse buildErrorResponse(int statusCode) {
        String message = HttpConstants.STATUS_MESSAGES.get(statusCode);
        String html = "<!DOCTYPE html><html><head><title>" + statusCode + " " + message + 
                     "</title></head><body><h1>" + statusCode + " " + message + 
                     "</h1></body></html>";
        
        HttpResponse response = new HttpResponse(statusCode);
        response.setBody(html.getBytes());
        response.setHeader("Content-Type", "text/html");
        response.setHeader("Content-Length", String.valueOf(html.length()));
        return response;
    }
    
    public static HttpResponse buildRedirectResponse(String location) {
        HttpResponse response = new HttpResponse(HttpConstants.STATUS_FOUND);
        response.setHeader("Location", location);
        return response;
    }
}

class HttpResponse {
    private int statusCode;
    private Map<String, String> headers;
    private byte[] body;
    
    public HttpResponse(int statusCode) {
        this.statusCode = statusCode;
        this.headers = new HashMap<>();
        this.body = new byte[0];
    }
    
    // Getters and Setters
    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }
    
    public Map<String, String> getHeaders() { return headers; }
    public String getHeader(String name) { return headers.get(name.toLowerCase()); }
    
    public void setHeader(String name, String value) {
        headers.put(name.toLowerCase(), value);
    }
    
    public void setHeaderIfAbsent(String name, String value) {
        headers.putIfAbsent(name.toLowerCase(), value);
    }
    
    public byte[] getBody() { return body; }
    public void setBody(byte[] body) { this.body = body; }
}