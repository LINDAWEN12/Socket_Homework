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

    public RequestHandler(Socket clientSocket, UserManager userManager) {
        this.clientSocket = clientSocket;
        this.userManager = userManager;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = clientSocket.getOutputStream();
            
            // 处理多个请求（长连接）
            while (!clientSocket.isClosed()) {
                HttpRequest request = RequestParser.parse(in);
                if (request == null) {
                    break; // 客户端关闭连接或发生错误
                }
                
                System.out.println("Request: " + request.getMethod() + " " + request.getPath() + 
                                 " from " + clientSocket.getInetAddress().getHostAddress());
                
                HttpResponse response = processRequest(request);
                ResponseBuilder.build(response, out);
                
                // 如果不是长连接，则退出循环
                if (!"keep-alive".equalsIgnoreCase(request.getHeader("Connection"))) {
                    break;
                }
            }
            
        } catch (SocketTimeoutException e) {
            // 长连接超时，正常关闭
            System.out.println("Connection timeout, closing: " + clientSocket.getInetAddress().getHostAddress());
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
            return ResponseBuilder.buildErrorResponse(HttpConstants.STATUS_METHOD_NOT_ALLOWED);
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
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
}