package shared;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class HttpUtils {
    
    /**
     * 解析查询字符串
     */
    public static Map<String, String> parseQueryString(String queryString) {
        Map<String, String> params = new HashMap<>();
        if (queryString == null || queryString.isEmpty()) {
            return params;
        }
        
        try {
            String[] pairs = queryString.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                String key = URLDecoder.decode(keyValue[0], HttpConstants.DEFAULT_CHARSET);
                String value = keyValue.length > 1 ? 
                    URLDecoder.decode(keyValue[1], HttpConstants.DEFAULT_CHARSET) : "";
                params.put(key, value);
            }
        } catch (UnsupportedEncodingException e) {
            // 使用默认字符集
            System.err.println("Error parsing query string: " + e.getMessage());
        }
        
        return params;
    }
    
    /**
     * 构建查询字符串
     */
    public static String buildQueryString(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        try {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (sb.length() > 0) {
                    sb.append("&");
                }
                sb.append(URLEncoder.encode(entry.getKey(), HttpConstants.DEFAULT_CHARSET))
                  .append("=")
                  .append(URLEncoder.encode(entry.getValue(), HttpConstants.DEFAULT_CHARSET));
            }
        } catch (UnsupportedEncodingException e) {
            System.err.println("Error building query string: " + e.getMessage());
        }
        
        return sb.toString();
    }
    
    /**
     * 获取文件扩展名
     */
    public static String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
    
    /**
     * 获取MIME类型
     */
    public static String getMimeType(String filename) {
        String ext = getFileExtension(filename);
        return HttpConstants.MIME_TYPES.getOrDefault(ext, "application/octet-stream");
    }
    
    /**
     * 规范化路径，防止目录遍历攻击
     */
    public static String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        
        // 移除查询参数
        int queryIndex = path.indexOf('?');
        if (queryIndex != -1) {
            path = path.substring(0, queryIndex);
        }
        
        // 确保以/开头
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        
        // 防止目录遍历
        if (path.contains("../") || path.contains("..\\")) {
            return "/";
        }
        
        return path;
    }
}