package client;

import shared.HttpConstants;
import java.util.HashMap;
import java.util.Map;

public class RequestBuilder {
    
    public static HttpRequest buildGetRequest(String path) {
        HttpRequest request = new HttpRequest("GET", path, HttpConstants.HTTP_VERSION);
        request.setHeader("Host", HttpConstants.SERVER_HOST + ":" + HttpConstants.SERVER_PORT);
        request.setHeader("User-Agent", "SimpleJavaHTTPClient/1.0");
        request.setHeader("Accept", "text/html,application/json,image/jpeg,*/*");
        request.setHeader("Accept-Encoding", "identity");
        request.setHeader("Connection", "keep-alive");
        return request;
    }
    
    public static HttpRequest buildGetRequest(String path, Map<String, String> headers) {
        HttpRequest request = buildGetRequest(path);
        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                request.setHeader(header.getKey(), header.getValue());
            }
        }
        return request;
    }
    
    public static HttpRequest buildPostRequest(String path, String body) {
        HttpRequest request = new HttpRequest("POST", path, HttpConstants.HTTP_VERSION);
        request.setHeader("Host", HttpConstants.SERVER_HOST + ":" + HttpConstants.SERVER_PORT);
        request.setHeader("User-Agent", "SimpleJavaHTTPClient/1.0");
        request.setHeader("Content-Type", "application/x-www-form-urlencoded");
        request.setHeader("Content-Length", String.valueOf(body.length()));
        request.setHeader("Accept", "application/json");
        request.setHeader("Connection", "keep-alive");
        request.setBody(body);
        return request;
    }
    
    public static HttpRequest buildPostRequest(String path, Map<String, String> params) {
        StringBuilder bodyBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (bodyBuilder.length() > 0) {
                bodyBuilder.append("&");
            }
            bodyBuilder.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return buildPostRequest(path, bodyBuilder.toString());
    }
}

class HttpRequest {
    private String method;
    private String path;
    private String version;
    private Map<String, String> headers;
    private String body;
    
    public HttpRequest(String method, String path, String version) {
        this.method = method;
        this.path = path;
        this.version = version;
        this.headers = new HashMap<>();
    }
    
    public void setHeader(String name, String value) {
        headers.put(name, value);
    }
    
    public void setBody(String body) {
        this.body = body;
    }

      public String getBody() {
        return body;
    }
    
    public String getMethod() {
        return method;
    }
    
    public String getPath() {
        return path;
    }

        public String getHeader(String name) {
        return headers.get(name);
    }
    
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        // 请求行
        sb.append(method).append(" ").append(path).append(" ").append(version).append(HttpConstants.CRLF);
        
        // 请求头
        for (Map.Entry<String, String> header : headers.entrySet()) {
            sb.append(header.getKey()).append(": ").append(header.getValue()).append(HttpConstants.CRLF);
        }
        
        sb.append(HttpConstants.CRLF);
        
        // 请求体
        if (body != null) {
            sb.append(body);
        }
        
        return sb.toString();
    }
}