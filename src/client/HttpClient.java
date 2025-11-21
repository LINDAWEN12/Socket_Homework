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
            System.err.println("Too many redirects");
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
            
            // 处理重定向
            if (followRedirects && isRedirect(response.getStatusCode())) {
                String location = response.getHeader("Location");
                if (location != null) {
                    System.out.println("Redirecting to: " + location);
                    
                    // 解析新的URL
                    URL redirectUrl = new URL(location);
                    if (!redirectUrl.getHost().equals(host) || redirectUrl.getPort() != port) {
                        System.out.println("Cross-host redirect not supported in this simple client");
                        return response;
                    }
                    
                    // 创建新的GET请求
                    HttpRequest redirectRequest = RequestBuilder.buildGetRequest(redirectUrl.getPath());
                    return sendRequest(redirectRequest, redirectCount + 1);
                }
            }
            
            return response;
            
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
            return null;
        }
    }
    
    private boolean isRedirect(int statusCode) {
        return statusCode == HttpConstants.STATUS_MOVED_PERMANENTLY || 
               statusCode == HttpConstants.STATUS_FOUND;
    }
    
    /**
     * 交互式命令行客户端
     */
    public void startInteractive() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Simple HTTP Client (Type 'quit' to exit)");
        
        while (true) {
            System.out.print("\nEnter URL path (e.g., /index.html): ");
            String path = scanner.nextLine().trim();
            
            if ("quit".equalsIgnoreCase(path)) {
                break;
            }
            
            if (path.isEmpty()) {
                path = "/";
            }
            
            HttpRequest request = RequestBuilder.buildGetRequest(path);
            HttpResponse response = sendRequest(request);
            
            if (response != null) {
                printResponse(response);
            } else {
                System.out.println("Failed to get response");
            }
        }
        
        scanner.close();
    }
    
    private void printResponse(HttpResponse response) {
        System.out.println("\n=== HTTP Response ===");
        System.out.println("Status: " + response.getStatusCode() + " " + 
                          HttpConstants.STATUS_MESSAGES.get(response.getStatusCode()));
        
        System.out.println("\nHeaders:");
        for (String key : response.getHeaders().keySet()) {
            System.out.println("  " + key + ": " + response.getHeader(key));
        }
        
        System.out.println("\nBody:");
        System.out.println(response.getBody());
    }

    public static void main(String[] args) {
        HttpClient client = new HttpClient();
        
        if (args.length > 0 && "-i".equals(args[0])) {
            // 交互式模式
            client.startInteractive();
        } else {
            // 测试模式
            testClient(client);
        }
    }
    
    private static void testClient(HttpClient client) {
        System.out.println("Testing HTTP Client...");
        
        // 测试GET请求
        System.out.println("\n1. Testing GET /index.html");
        HttpRequest getRequest = RequestBuilder.buildGetRequest("/index.html");
        HttpResponse response = client.sendRequest(getRequest);
        if (response != null) {
            System.out.println("Status: " + response.getStatusCode());
            System.out.println("Body length: " + response.getBody().length() + " characters");
        }
        
        // 测试重定向
        System.out.println("\n2. Testing redirect from / to /index.html");
        HttpRequest rootRequest = RequestBuilder.buildGetRequest("/");
        HttpResponse redirectResponse = client.sendRequest(rootRequest);
        if (redirectResponse != null) {
            System.out.println("Final Status: " + redirectResponse.getStatusCode());
        }
        
        // 测试POST请求（注册）
        System.out.println("\n3. Testing POST /api/register");
        String registerData = "username=testuser&password=testpass";
        HttpRequest postRequest = RequestBuilder.buildPostRequest("/api/register", registerData);
        HttpResponse postResponse = client.sendRequest(postRequest);
        if (postResponse != null) {
            System.out.println("Status: " + postResponse.getStatusCode());
            System.out.println("Response: " + postResponse.getBody());
        }
        
        // 测试不存在的文件
        System.out.println("\n4. Testing GET /nonexistent.html");
        HttpRequest notFoundRequest = RequestBuilder.buildGetRequest("/nonexistent.html");
        HttpResponse notFoundResponse = client.sendRequest(notFoundRequest);
        if (notFoundResponse != null) {
            System.out.println("Status: " + notFoundResponse.getStatusCode());
        }
    }
}