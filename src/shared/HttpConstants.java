package shared;

import java.util.HashMap;
import java.util.Map;

public class HttpConstants {
    // 服务器配置
    public static final int SERVER_PORT = 8022;
    public static final String SERVER_HOST = "localhost";
    
    // HTTP 版本
    public static final String HTTP_VERSION = "HTTP/1.1";
    
    // 换行符
    public static final String CRLF = "\r\n";
    
    // 缓冲区大小
    public static final int BUFFER_SIZE = 8192;
    
    // 状态码
    public static final int STATUS_OK = 200;
    public static final int STATUS_MOVED_PERMANENTLY = 301;
    public static final int STATUS_FOUND = 302;
    public static final int STATUS_NOT_MODIFIED = 304;
    public static final int STATUS_BAD_REQUEST = 400;
    public static final int STATUS_UNAUTHORIZED = 401;
    public static final int STATUS_NOT_FOUND = 404;
    public static final int STATUS_METHOD_NOT_ALLOWED = 405;
    public static final int STATUS_INTERNAL_ERROR = 500;
    
    // 状态码描述
    public static final Map<Integer, String> STATUS_MESSAGES = new HashMap<>();
    static {
        STATUS_MESSAGES.put(STATUS_OK, "OK");
        STATUS_MESSAGES.put(STATUS_MOVED_PERMANENTLY, "Moved Permanently");
        STATUS_MESSAGES.put(STATUS_FOUND, "Found");
        STATUS_MESSAGES.put(STATUS_NOT_MODIFIED, "Not Modified");
        STATUS_MESSAGES.put(STATUS_BAD_REQUEST, "Bad Request");
        STATUS_MESSAGES.put(STATUS_UNAUTHORIZED, "Unauthorized");
        STATUS_MESSAGES.put(STATUS_NOT_FOUND, "Not Found");
        STATUS_MESSAGES.put(STATUS_METHOD_NOT_ALLOWED, "Method Not Allowed");
        STATUS_MESSAGES.put(STATUS_INTERNAL_ERROR, "Internal Server Error");
    }
    
    // MIME 类型
    public static final Map<String, String> MIME_TYPES = new HashMap<>();
    static {
        MIME_TYPES.put("html", "text/html");
        MIME_TYPES.put("htm", "text/html");
        MIME_TYPES.put("txt", "text/plain");
        MIME_TYPES.put("css", "text/css");
        MIME_TYPES.put("js", "application/javascript");
        MIME_TYPES.put("json", "application/json");
        MIME_TYPES.put("jpg", "image/jpeg");
        MIME_TYPES.put("jpeg", "image/jpeg");
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("gif", "image/gif");
    }
    
    // 默认编码
    public static final String DEFAULT_CHARSET = "UTF-8";
    
    // 长连接超时时间（毫秒）
    public static final int KEEP_ALIVE_TIMEOUT = 5000;
}