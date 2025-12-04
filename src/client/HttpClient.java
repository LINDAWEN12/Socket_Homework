package client;

import shared.HttpConstants;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.util.Scanner;

public class HttpClient {
    private String host;
    private int port;
    private boolean followRedirects;
    private Socket connection;  // 保持长连接
    private OutputStream out;
    private InputStream in;
    private boolean connected = false;

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

        // 6. 测试长链接
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