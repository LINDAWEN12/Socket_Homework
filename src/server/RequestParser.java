package server;

import shared.HttpConstants;
import shared.HttpUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RequestParser {
    
    public static HttpRequest parse(BufferedReader in) throws IOException {
        String requestLine = in.readLine();
        if (requestLine == null) {
            return null; // 客户端关闭连接
        }
        
        // 解析请求行
        String[] requestParts = requestLine.split(" ");
        if (requestParts.length < 3) {
            return null; // 无效的请求
        }
        
        String method = requestParts[0];
        String path = HttpUtils.normalizePath(requestParts[1]);
        String version = requestParts[2];
        
        HttpRequest request = new HttpRequest(method, path, version);
        
        // 解析请求头
        String headerLine;
        while (!(headerLine = in.readLine()).isEmpty()) {
            int colonIndex = headerLine.indexOf(':');
            if (colonIndex > 0) {
                String headerName = headerLine.substring(0, colonIndex).trim();
                String headerValue = headerLine.substring(colonIndex + 1).trim();
                request.setHeader(headerName, headerValue);
            }
        }
        
        // 解析请求体（如果是POST请求）
        if ("POST".equalsIgnoreCase(method)) {
            parseRequestBody(in, request);
        }
        
        // 解析查询参数（GET请求）
        if ("GET".equalsIgnoreCase(method)) {
            int queryIndex = requestParts[1].indexOf('?');
            if (queryIndex != -1) {
                String queryString = requestParts[1].substring(queryIndex + 1);
                request.setQueryParams(HttpUtils.parseQueryString(queryString));
            }
        }
        
        return request;
    }
    
    private static void parseRequestBody(BufferedReader in, HttpRequest request) throws IOException {
        String contentLengthHeader = request.getHeader("Content-Length");
        if (contentLengthHeader != null) {
            int contentLength = Integer.parseInt(contentLengthHeader);
            char[] bodyChars = new char[contentLength];
            in.read(bodyChars, 0, contentLength);
            String body = new String(bodyChars);
            
            String contentType = request.getHeader("Content-Type");
            if (contentType != null && contentType.contains("application/x-www-form-urlencoded")) {
                request.setBodyParams(HttpUtils.parseQueryString(body));
            } else {
                request.setBody(body);
            }
        }
    }
}

class HttpRequest {
    private String method;
    private String path;
    private String version;
    private Map<String, String> headers;
    private Map<String, String> queryParams;
    private Map<String, String> bodyParams;
    private String body;
    
    public HttpRequest(String method, String path, String version) {
        this.method = method;
        this.path = path;
        this.version = version;
        this.headers = new HashMap<>();
        this.queryParams = new HashMap<>();
        this.bodyParams = new HashMap<>();
    }
    
    // Getters and Setters
    public String getMethod() { return method; }
    public String getPath() { return path; }
    public String getVersion() { return version; }
    
    public String getHeader(String name) { 
        return headers.get(name.toLowerCase()); 
    }
    
    public void setHeader(String name, String value) {
        headers.put(name.toLowerCase(), value);
    }
    
    public Map<String, String> getHeaders() { return headers; }
    
    public Map<String, String> getQueryParams() { return queryParams; }
    public void setQueryParams(Map<String, String> queryParams) { 
        this.queryParams = queryParams; 
    }
    
    public Map<String, String> getBodyParams() { return bodyParams; }
    public void setBodyParams(Map<String, String> bodyParams) { 
        this.bodyParams = bodyParams; 
    }
    
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}