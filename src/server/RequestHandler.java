package server;

import shared.HttpConstants;
import shared.HttpUtils;
import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Map;

public class RequestHandler implements Runnable {
    private Socket clientSocket;
    private UserManager userManager;
    private BufferedReader in;
    private OutputStream out;
    private int requestCount = 0; // 跟踪当前连接处理的请求数量
    private final int MAX_REQUESTS_PER_CONNECTION = 100; // 每个连接最多处理100个请求

    public RequestHandler(Socket clientSocket, UserManager userManager) {
        this.clientSocket = clientSocket;
        this.userManager = userManager;
    }

    @Override
    public void run() {
        String clientAddress = clientSocket.getInetAddress().getHostAddress();
        System.out.println("开始处理新连接，客户端: " + clientAddress);

        try {
            clientSocket.setSoTimeout(HttpConstants.KEEP_ALIVE_TIMEOUT);

            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = clientSocket.getOutputStream();
            
            // 处理多个请求（长连接）
            while (!clientSocket.isClosed() && requestCount < MAX_REQUESTS_PER_CONNECTION) {
                System.out.println("等待下一个请求... 当前请求数: " + requestCount + "，客户端: " + clientAddress);

                try {
                    HttpRequest request = RequestParser.parse(in);

                    if (request == null) {
                        System.out.println("请求解析为null，结束处理循环。客户端: " + clientAddress);
                        break; // 解析失败，可能是格式错误
                    }

                    requestCount++;
                    System.out.println("处理第 " + requestCount + " 个请求，方法: " + request.getMethod() +
                            "，路径: " + request.getPath() + "，客户端: " + clientAddress);

                    HttpResponse response = processRequest(request);
                    boolean keepAlive = shouldKeepAlive(request);

                    if (keepAlive) {
                        response.setHeader("Connection", "keep-alive");
                        response.setHeader("Keep-Alive", "timeout=" + (HttpConstants.KEEP_ALIVE_TIMEOUT/1000));
                    } else {
                        response.setHeader("Connection", "close");
                    }

                    ResponseBuilder.build(response, out);

                    // 如果不保持连接，则退出循环
                    if (!keepAlive) {
                        System.out.println("不再保持连接，准备关闭。客户端: " + clientAddress);
                        break;
                    }

                } catch (SocketTimeoutException e) {
                    System.out.println("读取请求超时，关闭空闲连接。客户端: " + clientAddress);
                    break;
                } catch (IOException e) {
                    if (e.getMessage() != null && e.getMessage().contains("Connection reset")) {
                        System.out.println("连接被重置，客户端可能异常关闭。客户端: " + clientAddress);
                        break;
                    }
                    System.err.println("处理请求时发生I/O错误: " + e.getMessage() + "，客户端: " + clientAddress);
                    e.printStackTrace();
                    break;
                }
            }

            System.out.println("连接处理完成，共处理 " + requestCount + " 个请求，客户端: " + clientAddress);
        } catch (IOException e) {
            System.err.println("Error handling request: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }
    
    private HttpResponse processRequest(HttpRequest request) {
        String method = request.getMethod();
        String path = request.getPath();
        
        // 检查支持的HTTP方法
        if (!"GET".equals(method) && !"POST".equals(method)) {
            HttpResponse errorResponse = ResponseBuilder.buildErrorResponse(HttpConstants.STATUS_METHOD_NOT_ALLOWED);
            errorResponse.setHeader("Connection", "close"); // 不支持的方法，关闭连接
            return errorResponse;
        }
        
        // 处理API请求
        if ("/api/register".equals(path) && "POST".equals(method)) {
            return handleRegister(request);
        } else if ("/api/login".equals(path) && "POST".equals(method)) {
            return handleLogin(request);
        }
        
        // 处理静态文件请求
        return handleStaticFile(request);
    }
    
    private HttpResponse handleRegister(HttpRequest request) {
        try {
            Map<String, String> params = request.getBodyParams();
            String username = params.get("username");
            String password = params.get("password");
            
            if (username == null || username.trim().isEmpty() || 
                password == null || password.trim().isEmpty()) {
                return ResponseBuilder.buildJsonResponse(
                    HttpConstants.STATUS_BAD_REQUEST, 
                    "{\"error\": \"Username and password are required\"}"
                );
            }
            
            boolean success = userManager.registerUser(username.trim(), password.trim());
            if (success) {
                return ResponseBuilder.buildJsonResponse(
                    HttpConstants.STATUS_OK,
                    "{\"message\": \"User registered successfully\"}"
                );
            } else {
                return ResponseBuilder.buildJsonResponse(
                    HttpConstants.STATUS_BAD_REQUEST,
                    "{\"error\": \"Username already exists\"}"
                );
            }
        } catch (Exception e) {
            return ResponseBuilder.buildErrorResponse(HttpConstants.STATUS_INTERNAL_ERROR);
        }
    }
    
    private HttpResponse handleLogin(HttpRequest request) {
        try {
            Map<String, String> params = request.getBodyParams();
            String username = params.get("username");
            String password = params.get("password");
            
            if (username == null || password == null) {
                return ResponseBuilder.buildJsonResponse(
                    HttpConstants.STATUS_BAD_REQUEST,
                    "{\"error\": \"Username and password are required\"}"
                );
            }
            
            boolean success = userManager.loginUser(username, password);
            if (success) {
                return ResponseBuilder.buildJsonResponse(
                    HttpConstants.STATUS_OK,
                    "{\"message\": \"Login successful\"}"
                );
            } else {
                return ResponseBuilder.buildJsonResponse(
                    HttpConstants.STATUS_UNAUTHORIZED,
                    "{\"error\": \"Invalid username or password\"}"
                );
            }
        } catch (Exception e) {
            return ResponseBuilder.buildErrorResponse(HttpConstants.STATUS_INTERNAL_ERROR);
        }
    }
    
    private HttpResponse handleStaticFile(HttpRequest request) {
        String path = request.getPath();
        
        // 重定向根路径到index.html - 修复这里！
        if ("/".equals(path)) {
            System.out.println("Redirecting / to /index.html");
            return ResponseBuilder.buildRedirectResponse("/index.html");
        }
        
        // 处理其他静态文件
        return ResponseBuilder.buildFileResponse(path);
    }
    
    private void closeConnection() {
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            System.err.println("关闭输入流时出错: " + e.getMessage());
        }

        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            System.err.println("关闭输出流时出错: " + e.getMessage());
        }

        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
                System.out.println("Socket已关闭");
            }
        } catch (IOException e) {
            System.err.println("关闭Socket时出错: " + e.getMessage());
        }
    }

    private boolean shouldKeepAlive(HttpRequest request) {
        // 1. 检查请求的连接头部
        String requestConnection = request.getHeader("Connection");

        // 2. 根据HTTP版本决定
        String httpVersion = request.getVersion();
        boolean isHttp11 = "HTTP/1.1".equals(httpVersion);

        if (isHttp11) {
            // HTTP/1.1默认保持连接，除非明确指定Connection: close
            if (requestConnection != null && "close".equalsIgnoreCase(requestConnection.trim())) {
                System.out.println("HTTP/1.1请求明确要求关闭连接");
                return false;
            }
            System.out.println("HTTP/1.1请求，保持连接");
            return true;
        } else {
            // HTTP/1.0默认关闭连接，除非明确指定Connection: keep-alive
            if (requestConnection != null && "keep-alive".equalsIgnoreCase(requestConnection.trim())) {
                System.out.println("HTTP/1.0请求明确要求保持连接");
                return true;
            }
            System.out.println("HTTP/1.0请求，关闭连接");
            return false;
        }
    }
}