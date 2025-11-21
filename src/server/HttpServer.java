package server;

import shared.HttpConstants;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServer {
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private boolean isRunning;
    private UserManager userManager;

    public HttpServer() {
        this.userManager = new UserManager();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(HttpConstants.SERVER_PORT);
            threadPool = Executors.newFixedThreadPool(10);
            isRunning = true;
            
            System.out.println("HTTP Server started on port " + HttpConstants.SERVER_PORT);
            System.out.println("Webroot: ./webroot");
            System.out.println("Access: http://localhost:" + HttpConstants.SERVER_PORT);
            
            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                // 设置Socket超时，支持长连接
                clientSocket.setSoTimeout(HttpConstants.KEEP_ALIVE_TIMEOUT);
                threadPool.execute(new RequestHandler(clientSocket, userManager));
            }
            
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            stop();
        }
    }

    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            if (threadPool != null) {
                threadPool.shutdown();
            }
        } catch (IOException e) {
            System.err.println("Error stopping server: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        HttpServer server = new HttpServer();
        
        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down server...");
            server.stop();
        }));
        
        server.start();
    }
}