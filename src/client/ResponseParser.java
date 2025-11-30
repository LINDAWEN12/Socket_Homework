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
        while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
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
            try {
                int contentLength = Integer.parseInt(contentLengthHeader);
                if (contentLength > 0) {
                    char[] bodyChars = new char[contentLength];
                    int bytesRead = reader.read(bodyChars, 0, contentLength);
                    if (bytesRead > 0) {
                        bodyBuilder.append(bodyChars, 0, bytesRead);
                    }
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid Content-Length: " + contentLengthHeader);
            }
        } else {
            // 对于没有Content-Length的响应，尝试读取可用数据
            try {
                while (reader.ready()) {
                    int charRead = reader.read();
                    if (charRead == -1) break;
                    bodyBuilder.append((char) charRead);
                }
            } catch (IOException e) {
                // 忽略读取错误
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
        this.headers = headers != null ? headers : new HashMap<>();
        this.body = body != null ? body : "";
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