package server;

import java.util.HashMap;
import java.util.Map;

public class UserManager {
    private Map<String, String> users; // 用户名 -> 密码
    
    public UserManager() {
        this.users = new HashMap<>();
        // 添加一些测试用户
        users.put("admin", "admin123");
        users.put("test", "test123");
    }
    
    /**
     * 用户注册
     */
    public boolean registerUser(String username, String password) {
        if (users.containsKey(username)) {
            return false; // 用户名已存在
        }
        users.put(username, password);
        System.out.println("User registered: " + username);
        return true;
    }
    
    /**
     * 用户登录
     */
    public boolean loginUser(String username, String password) {
        String storedPassword = users.get(username);
        if (storedPassword != null && storedPassword.equals(password)) {
            System.out.println("User logged in: " + username);
            return true;
        }
        System.out.println("Login failed for user: " + username);
        return false;
    }
    
    /**
     * 获取用户数量（用于测试）
     */
    public int getUserCount() {
        return users.size();
    }
}