package client;

import shared.HttpConstants;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class HttpClient {
    private String host;
    private int port;
    private boolean followRedirects;
    private Socket connection;
    private OutputStream out;
    private InputStream in;
    private boolean connected = false;
    
    private Map<String, CachedResponse> cache;

    public HttpClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.followRedirects = true;
        this.cache = new HashMap<>();
    }

    public HttpClient() {
        this(HttpConstants.SERVER_HOST, HttpConstants.SERVER_PORT);
    }
    
    /**
     * 清除缓存（用于测试）
     */
    public void clearCache() {
        cache.clear();
        System.out.println("  [Cache] Cache cleared");
    }

    /**
     * 发送HTTP请求并返回响应
     */
    public HttpResponse sendRequest(HttpRequest request) {
        return sendRequest(request, 0, request.getMethod());
    }

    private HttpResponse sendRequest(HttpRequest request, int redirectCount, String originalMethod) {
        if (redirectCount > 5) {
            System.err.println("Error: Too many redirects");
            return null;
        }
        
        try (Socket socket = new Socket(host, port);
             OutputStream out = socket.getOutputStream();
             InputStream in = socket.getInputStream()) {
            
            // 检查缓存（仅对GET请求）
            if ("GET".equals(request.getMethod())) {
                CachedResponse cached = cache.get(request.getPath());
                if (cached != null && !cached.isExpired()) {
                    System.out.println("  [Cache] Found cached response for: " + request.getPath());
                    // 添加条件请求头
                    if (cached.getLastModified() != null) {
                        request.setHeader("If-Modified-Since", cached.getLastModified());
                    }
                    if (cached.getEtag() != null) {
                        request.setHeader("If-None-Match", cached.getEtag());
                    }
                }
            }
            
            // 发送请求
            out.write(request.toString().getBytes(HttpConstants.DEFAULT_CHARSET));
            out.flush();
            
            // 解析响应
            HttpResponse response = ResponseParser.parse(in);
            
            if (response == null) {
                return null;
            }
            
            int statusCode = response.getStatusCode();
            
            // 处理304 Not Modified
            if (statusCode == HttpConstants.STATUS_NOT_MODIFIED) {
                System.out.println("  [304] Resource not modified - using cached version");
                CachedResponse cached = cache.get(request.getPath());
                if (cached != null) {
                    cached.updateTimestamp();
                    return cached.getResponse();
                }
                return response;
            }
            
            // 缓存成功的GET响应
            if ("GET".equals(request.getMethod()) && statusCode == 200) {
                String lastModified = response.getHeader("Last-Modified");
                String etag = response.getHeader("ETag");
                cache.put(request.getPath(), new CachedResponse(response, lastModified, etag));
                System.out.println("  [Cache] Response cached for: " + request.getPath());
            }
            
            // 处理重定向 (301, 302)
            if (followRedirects && isRedirect(statusCode)) {
                System.out.println("  [" + statusCode + "] Redirect detected");
                String location = response.getHeader("Location");
                if (location != null && !location.isEmpty()) {
                    System.out.println("  Redirecting to: " + location);
                    
                    // 解析重定向URL
                    String redirectPath;
                    if (location.startsWith("http")) {
                        URL redirectUrl = new URL(location);
                        if (!redirectUrl.getHost().equals(host) || redirectUrl.getPort() != port) {
                            System.out.println("  Cross-host redirect not supported: " + location);
                            return response;
                        }
                        redirectPath = redirectUrl.getPath();
                    } else {
                        redirectPath = location;
                    }
                    
                    // 根据状态码决定重定向方法
              
HttpRequest redirectRequest;
if (statusCode == HttpConstants.STATUS_MOVED_PERMANENTLY) {
    // 301: 保持原始方法
    redirectRequest = new HttpRequest(originalMethod, redirectPath, HttpConstants.HTTP_VERSION);
    System.out.println("  [301] Permanent redirect - Keeping original method: " + originalMethod);
    
    // 复制请求体（如果是POST重定向）
    if ("POST".equals(originalMethod) && request.getBody() != null) {
        redirectRequest.setBody(request.getBody());
        redirectRequest.setHeader("Content-Type", request.getHeader("Content-Type"));
        redirectRequest.setHeader("Content-Length", String.valueOf(request.getBody().length()));
        
        // 复制其他必要的POST头
        String contentType = request.getHeader("Content-Type");
        if (contentType != null) {
            redirectRequest.setHeader("Content-Type", contentType);
        }
    }
} else {
    // 302: 总是使用GET方法
    redirectRequest = RequestBuilder.buildGetRequest(redirectPath);
    System.out.println("  [302] Temporary redirect - Using GET method (request body will be lost)");
}
                    
                    // 复制重要的请求头
                    copyHeaders(request, redirectRequest);
                    
                    return sendRequest(redirectRequest, redirectCount + 1, originalMethod);
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
    
    /**
     * 复制请求头
     */
    private void copyHeaders(HttpRequest source, HttpRequest target) {
        target.setHeader("Host", source.getHeader("Host"));
        target.setHeader("User-Agent", source.getHeader("User-Agent"));
        target.setHeader("Accept", source.getHeader("Accept"));
        target.setHeader("Accept-Encoding", source.getHeader("Accept-Encoding"));
    }

    /**
     * 使用长连接发送请求（必须在connect()后调用）
     */
    private HttpResponse sendRequestWithKeepAlive(HttpRequest request) throws IOException {
        if (!connected) {
            throw new IllegalStateException("未连接到服务器，请先建立长连接");
        }

        // 确保请求包含Connection: keep-alive
        request.setHeader("Connection", "keep-alive");

        // 发送请求
        out.write(request.toString().getBytes(HttpConstants.DEFAULT_CHARSET));
        out.flush();

        // 解析响应
        HttpResponse response = ResponseParser.parse(in);

        return response;
    }
    
    private static boolean isRedirect(int statusCode) {
        return statusCode == HttpConstants.STATUS_MOVED_PERMANENTLY || 
               statusCode == HttpConstants.STATUS_FOUND;
    }

    // 以下是main方法和演示代码，保持不变...
    
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
    
    // 清空缓存开始测试
    client.clearCache();
    
    // 1. 测试302临时重定向（根路径）
    System.out.println("1. GET / (测试302临时重定向)");
    HttpRequest rootRequest = RequestBuilder.buildGetRequest("/");
    HttpResponse response = client.sendRequest(rootRequest);
    printResponse(response);
    
    // 2. 测试301永久重定向（GET）
    System.out.println("\n2. GET /old-page (测试301永久重定向 - GET)");
    HttpRequest redirect301Request = RequestBuilder.buildGetRequest("/old-page");
    HttpResponse redirect301Response = client.sendRequest(redirect301Request);
    printResponse(redirect301Response);
    
    // 3. 测试302临时重定向（GET）
    System.out.println("\n3. GET /temp (测试302临时重定向 - GET)");
    HttpRequest redirect302Request = RequestBuilder.buildGetRequest("/temp");
    HttpResponse redirect302Response = client.sendRequest(redirect302Request);
    printResponse(redirect302Response);
    
    // 4. 测试缓存机制 - 请求同一个资源两次
    System.out.println("\n4. GET /index.html (第一次请求，将被缓存)");
    HttpRequest getRequest = RequestBuilder.buildGetRequest("/index.html");
    response = client.sendRequest(getRequest);
    printResponse(response);
    
    // 5. 测试304缓存 - 同一个请求应该使用缓存或收到304
    System.out.println("\n5. GET /index.html (第二次请求，测试304缓存)");
    response = client.sendRequest(getRequest);
    printResponse(response);
    
    // 6. 测试301永久重定向（POST）- 应该保持POST方法
    System.out.println("\n6. POST /old-form (测试301永久重定向 - POST)");
    String postData = "username=test&password=test123";
    HttpRequest post301Request = RequestBuilder.buildPostRequest("/old-form", postData);
    HttpResponse post301Response = client.sendRequest(post301Request);
    printResponse(post301Response);
    
    // 7. 测试302临时重定向（POST）- 应该转为GET方法
    System.out.println("\n7. POST /temp-post (测试302临时重定向 - POST)");
    HttpRequest post302Request = RequestBuilder.buildPostRequest("/temp-post", postData);
    HttpResponse post302Response = client.sendRequest(post302Request);
    printResponse(post302Response);
    
    // 8. 测试POST登录
    System.out.println("\n8. POST /api/login (测试登录)");
    String loginData = "username=admin&password=admin123";
    HttpRequest loginRequest = RequestBuilder.buildPostRequest("/api/login", loginData);
    HttpResponse loginResponse = client.sendRequest(loginRequest);
    printResponse(loginResponse);
    
    // 9. 测试注册
    System.out.println("\n9. POST /api/register (测试注册)");
    String registerData = "username=newuser&password=newpass123";
    HttpRequest registerRequest = RequestBuilder.buildPostRequest("/api/register", registerData);
    HttpResponse registerResponse = client.sendRequest(registerRequest);
    printResponse(registerResponse);
    
    // 10. 测试404错误
    System.out.println("\n10. GET /nonexistent.html (测试404)");
    HttpRequest notFoundRequest = RequestBuilder.buildGetRequest("/nonexistent.html");
    HttpResponse notFoundResponse = client.sendRequest(notFoundRequest);
    printResponse(notFoundResponse);
    
    System.out.println("\n=== Basic Demonstration Completed ===");
    
    // 11. 测试长链接
    keepTestClient(client);
}

    private static void keepTestClient(HttpClient client) {
        System.out.println("\n6. test keep alive");

        try {
            // 手动建立长连接
            client.connect();
            HttpRequest keepRequest = RequestBuilder.buildGetRequest("/index.html");
            keepRequest.setHeader("Connection", "keep-alive");

            System.out.println("6.1 等待一段时间(应在长链接保持时间内)再发送请求");
            System.out.println("第一次请求，建立长连接：");

            long startTime = System.currentTimeMillis();
            HttpResponse keepResponse = client.sendRequestWithKeepAlive(keepRequest);
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("花费的时间: " + duration + "ms");
            printResponse(keepResponse);

            long waitTime = 3 * 1000;
            System.out.println("接下来等待一段时间(" + waitTime / 1000 +"s)");
            Thread.sleep(waitTime);
            System.out.println("发送第二次请求，时间应该较短");
            startTime = System.currentTimeMillis();
            keepResponse = client.sendRequestWithKeepAlive(keepRequest);
            duration = System.currentTimeMillis() - startTime;
            System.out.println("花费的时间: " + duration + "ms");
            printResponse(keepResponse);

            System.out.println("6.2 尝试在一个长连接上连续发送10个请求");
            long allTime = 0;
            for (int i = 0; i < 10; i++) {
                System.out.println("第" + i + "次请求");
                startTime = System.currentTimeMillis();
                client.sendRequestWithKeepAlive(keepRequest);
                duration = System.currentTimeMillis() - startTime;
                allTime += duration;
                System.out.println("花费的时间: " + duration + "ms");
                // 短暂延迟
                Thread.sleep(100);
            }

            System.out.println("总时间: " + allTime + "ms");

            System.out.println("6.3 与短链接测试对比");
            System.out.println("进行10次短链接(GET)");

            long shortAllTime = 0;
            for (int i = 0; i < 10; i++) {
                System.out.println("第" + i + "次请求");
                HttpRequest shortRequest = RequestBuilder.buildGetRequest("/index.html");
                startTime = System.currentTimeMillis();
                client.sendRequest(shortRequest);
                duration = System.currentTimeMillis() - startTime;
                shortAllTime += duration;
                System.out.println("花费的时间: " + duration + "ms");
                // 短暂延迟
                Thread.sleep(100);
            }
            System.out.println("总时间: " + shortAllTime + "ms");

            if (allTime < shortAllTime) {
                System.out.println("长连接用时比短连接少，效率提高：" + (shortAllTime - allTime) / (double)shortAllTime * 100
                 + "%");
            } else {
                System.out.println("长连接没有带来提升");
            }

            System.out.println("6.4 长连接超时测试");
            System.out.println("等待一段时间(应在大于长链接保持时间)再发送请求");
            waitTime = 6 * 1000;
            System.out.println("接下来等待一段时间(" + waitTime / 1000 +"s)");
            Thread.sleep(waitTime);
            try {
                System.out.println("超时后发送请求");
                client.sendRequestWithKeepAlive(keepRequest);
                System.err.println("测试失败：应该断开连接！");
            } catch (SocketException e) {
                System.out.println("捕获错误信息：" + e.getMessage());
                System.out.println("如果连接断开则符合预期");
            }
        } catch (Exception e) {
            System.err.println("长连接测试失败: " + e.getMessage());
        } finally {
            if (client.connected) {
                try {
                    client.disconnect();
                } catch (IOException e) {
                    System.err.println("Client disconnect error: " + e.getMessage());
                }
            }
        }
    }
    
    private static void printResponse(HttpResponse response) {
    if (response == null) {
        System.out.println("  No response received");
        return;
    }
    
    int statusCode = response.getStatusCode();
    System.out.print("  Status: " + statusCode + " " + 
                    HttpConstants.STATUS_MESSAGES.getOrDefault(statusCode, "Unknown"));
    
    // 对于重定向，显示Location头
    if (isRedirect(statusCode)) {
        String location = response.getHeader("Location");
        if (location != null) {
            System.out.print(" -> Redirect to: " + location);
        }
    }
    
    // 对于304，显示缓存信息
    if (statusCode == HttpConstants.STATUS_NOT_MODIFIED) {
        System.out.print(" [Using cached version]");
    }
    
    System.out.println();
    
    // 显示响应体前80个字符
    String body = response.getBody();
    if (body != null && !body.isEmpty()) {
        String preview = body.length() > 80 ? body.substring(0, 80) + "..." : body;
        preview = preview.replace("\n", " ").replace("\r", "");
        System.out.println("  Body preview: " + preview);
    }
    
    System.out.println("  Body length: " + (body != null ? body.length() : 0) + " characters");
}

    // 连接到服务器
    public void connect() throws IOException {
        if (!connected) {
            connection = new Socket(host, port);
            connection.setSoTimeout(30000); // 30秒超时
            out = connection.getOutputStream();
            in = connection.getInputStream();
            connected = true;
        }
    }

    public void disconnect() throws IOException {
        if (connected) {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (connection != null) {
                connection.close();
            }
            connected = false;
        }
    }
}

/**
 * 缓存响应类
 */
class CachedResponse {
    private HttpResponse response;
    private String lastModified;
    private String etag;
    private long timestamp;
    private static final long CACHE_DURATION = 300000; // 5分钟缓存
    
    public CachedResponse(HttpResponse response, String lastModified, String etag) {
        this.response = response;
        this.lastModified = lastModified;
        this.etag = etag;
        this.timestamp = System.currentTimeMillis();
    }
    
    public HttpResponse getResponse() {
        return response;
    }
    
    public String getLastModified() {
        return lastModified;
    }
    
    public String getEtag() {
        return etag;
    }
    
    public boolean isExpired() {
        return System.currentTimeMillis() - timestamp > CACHE_DURATION;
    }
    
    public void updateTimestamp() {
        this.timestamp = System.currentTimeMillis();
    }
}