package client;

import shared.HttpConstants;
import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.Scanner;

public class HttpClient {
    private String host;
    private int port;
    private boolean followRedirects;

    public HttpClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.followRedirects = true;
    }

    public HttpClient() {
        this(HttpConstants.SERVER_HOST, HttpConstants.SERVER_PORT);
    }

    /**
     * 发送HTTP请求并返回响应
     */
    public HttpResponse sendRequest(HttpRequest request) {
        return sendRequest(request, 0);
    }
    
    private HttpResponse sendRequest(HttpRequest request, int redirectCount) {
        if (redirectCount > 5) {
            System.err.println("Error: Too many redirects");
            return null;
        }
        
        try (Socket socket = new Socket(host, port);
             OutputStream out = socket.getOutputStream();
             InputStream in = socket.getInputStream()) {
            
            // 发送请求
            out.write(request.toString().getBytes(HttpConstants.DEFAULT_CHARSET));
            out.flush();
            
            // 解析响应
            HttpResponse response = ResponseParser.parse(in);
            
            if (response == null) {
                return null;
            }
            
            // 处理重定向 (301, 302)
            if (followRedirects && isRedirect(response.getStatusCode())) {
                String location = response.getHeader("Location");
                if (location != null && !location.isEmpty()) {
                    // 解析重定向URL
                    String redirectPath;
                    if (location.startsWith("http")) {
                        URL redirectUrl = new URL(location);
                        if (!redirectUrl.getHost().equals(host) || redirectUrl.getPort() != port) {
                            System.out.println("Cross-host redirect not supported: " + location);
                            return response;
                        }
                        redirectPath = redirectUrl.getPath();
                    } else {
                        redirectPath = location;
                    }
                    
                    // 创建新的GET请求进行重定向
                    HttpRequest redirectRequest = RequestBuilder.buildGetRequest(redirectPath);
                    return sendRequest(redirectRequest, redirectCount + 1);
                }
            }
            
            return response;
            
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            return null;
        }
    }
    
    private static boolean isRedirect(int statusCode) {
        return statusCode == HttpConstants.STATUS_MOVED_PERMANENTLY || 
               statusCode == HttpConstants.STATUS_FOUND;
    }

    /**
     * 演示使用方法 - 符合题目要求的命令行方式
     */
    public static void main(String[] args) {
        HttpClient client = new HttpClient();
        
        // 演示各种请求 - 符合题目要求
        demonstrateClient(client);
    }
    
    private static void demonstrateClient(HttpClient client) {
        System.out.println("=== HTTP Client Demonstration ===\n");
        
        // 1. 测试普通GET请求
        System.out.println("1. GET /index.html");
        HttpRequest getRequest = RequestBuilder.buildGetRequest("/index.html");
        HttpResponse response = client.sendRequest(getRequest);
        printResponse(response);
        
        // 2. 测试重定向 (题目要求的功能)
        System.out.println("\n2. GET / (测试重定向)");
        HttpRequest rootRequest = RequestBuilder.buildGetRequest("/");
        HttpResponse redirectResponse = client.sendRequest(rootRequest);
        printResponse(redirectResponse);
        
        // 3. 测试POST请求 - 注册 (题目要求的功能)
        System.out.println("\n3. POST /api/register");
        String registerData = "username=demo_user&password=demo_pass";
        HttpRequest postRequest = RequestBuilder.buildPostRequest("/api/register", registerData);
        HttpResponse postResponse = client.sendRequest(postRequest);
        printResponse(postResponse);
        
        // 4. 测试POST请求 - 登录 (题目要求的功能)
        System.out.println("\n4. POST /api/login");
        String loginData = "username=admin&password=admin123";
        HttpRequest loginRequest = RequestBuilder.buildPostRequest("/api/login", loginData);
        HttpResponse loginResponse = client.sendRequest(loginRequest);
        printResponse(loginResponse);
        
        // 5. 测试404错误
        System.out.println("\n5. GET /nonexistent.html (测试404)");
        HttpRequest notFoundRequest = RequestBuilder.buildGetRequest("/nonexistent.html");
        HttpResponse notFoundResponse = client.sendRequest(notFoundRequest);
        printResponse(notFoundResponse);
        
        System.out.println("\n=== Demonstration Completed ===");
    }
    
    private static void printResponse(HttpResponse response) {
        if (response == null) {
            System.out.println("  No response received");
            return;
        }
        
        System.out.println("  Status: " + response.getStatusCode() + " " + 
                          HttpConstants.STATUS_MESSAGES.get(response.getStatusCode()));
        
        // 对于重定向，显示Location头
        if (isRedirect(response.getStatusCode())) {
            String location = response.getHeader("Location");
            System.out.println("  Location: " + location);
        }
        
        // 显示响应体前100个字符
        String body = response.getBody();
        if (body != null && !body.isEmpty()) {
            String preview = body.length() > 100 ? body.substring(0, 100) + "..." : body;
            System.out.println("  Body: " + preview.replace("\n", " "));
        }
        
        System.out.println("  Body length: " + (body != null ? body.length() : 0) + " characters");
    }
    
    
}