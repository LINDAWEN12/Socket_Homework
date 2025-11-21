package client;

import shared.HttpConstants;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ResponseParser {
    
    public static HttpResponse parse(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        
        // 解析状态行
        String statusLine = reader.readLine();
        if (statusLine == null) {
            return null;
        }
        
        String[] statusParts = statusLine.split(" ", 3);
        if (statusParts.length < 3) {
            return null;
        }
        
        int statusCode = Integer.parseInt(statusParts[1]);
        
        // 解析响应头
        Map<String, String> headers = new HashMap<>();
        String headerLine;
        while (!(headerLine = reader.readLine()).isEmpty()) {
            int colonIndex = headerLine.indexOf(':');
            if (colonIndex > 0) {
                String headerName = headerLine.substring(0, colonIndex).trim();
                String headerValue = headerLine.substring(colonIndex + 1).trim();
                headers.put(headerName, headerValue);
            }
        }
        
        // 解析响应体
        StringBuilder bodyBuilder = new StringBuilder();
        String contentLengthHeader = headers.get("Content-Length");
        if (contentLengthHeader != null) {
            int contentLength = Integer.parseInt(contentLengthHeader);
            char[] bodyChars = new char[contentLength];
            reader.read(bodyChars, 0, contentLength);
            bodyBuilder.append(bodyChars);
        } else {
            // 如果没有Content-Length，读取直到流结束
            String line;
            while ((line = reader.readLine()) != null) {
                bodyBuilder.append(line).append("\n");
            }
        }
        
        return new HttpResponse(statusCode, headers, bodyBuilder.toString());
    }
}

class HttpResponse {
    private int statusCode;
    private Map<String, String> headers;
    private String body;
    
    public HttpResponse(int statusCode, Map<String, String> headers, String body) {
        this.statusCode = statusCode;
        this.headers = headers;
        this.body = body;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    public String getHeader(String name) {
        return headers.get(name);
    }
    
    public String getBody() {
        return body;
    }
}