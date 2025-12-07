HTTP Socket 项目
一个使用纯 Java Socket 编程实现的自定义 HTTP 客户端和服务器，不使用任何现成的网络框架（如 Netty、Apache HttpClient 等）。

项目概述
这个项目完全从零开始实现了 HTTP 协议的客户端和服务器端，包括：

HTTP客户端：能够发送 HTTP 请求并处理响应，支持自动重定向

HTTP服务器：支持 GET/POST 请求、多种状态码、长连接、MIME 类型识别

用户管理：实现了用户注册和登录功能

静态文件服务：能够提供 HTML、CSS、图片等静态资源

项目结构
text
http-socket-project/
├── src/                            # 源代码文件夹
│   ├── client/                     # 客户端代码
│   │   ├── HttpClient.java         # 客户端主程序
│   │   ├── RequestBuilder.java     # 构建 HTTP 请求
│   │   └── ResponseParser.java     # 解析 HTTP 响应
│   ├── server/                     # 服务器端代码
│   │   ├── HttpServer.java         # 服务器主程序
│   │   ├── RequestHandler.java     # 处理客户端请求
│   │   ├── RequestParser.java      # 解析 HTTP 请求
│   │   ├── ResponseBuilder.java    # 构建 HTTP 响应
│   │   └── UserManager.java        # 管理用户注册登录
│   └── shared/                     # 客户端和服务器共享的代码
│       ├── HttpConstants.java      # 定义常量（状态码、MIME 类型等）
│       └── HttpUtils.java          # 工具方法
├── webroot/                        # 服务器静态文件目录
│   ├── index.html                  # 首页
│   ├── login.html                  # 登录页面
│   ├── register.html               # 注册页面
│   └── images/                     # 图片文件夹
│   └── sample.txt
├── build/                          # 编译后的 class 文件
├── README.md                       # 项目说明文档
└── run.sh                          # 运行脚本
└── kATest.md
└── MIMETest.md


HTTP 服务器特性
✅ 支持 GET 和 POST 请求方法

✅ 完整 HTTP 状态码支持（200, 301, 302, 304, 404, 405, 500）

✅ HTTP/1.1 长连接（Keep-Alive）支持

✅ 多种 MIME 类型支持（HTML、CSS、JS、JSON、图片等）

✅ 静态文件服务

✅ 用户注册和登录 API

✅ 请求解析和响应构建

HTTP 客户端特性
✅ 手动构建 HTTP 请求报文

✅ 解析 HTTP 响应报文

✅ 自动跟随重定向（301, 302）

✅ 特殊处理 304 状态码

✅ 命令行交互界面

✅ 支持 HTTP 和 HTTPS 协议


1. 编译项目
bash
# 执行运行脚本（包含编译和运行）
./run.sh



服务器将启动在 端口 8022
通过浏览器访问：

打开浏览器访问：http://localhost:8022/

注册页面：http://localhost:8022/register.html

登录页面：http://localhost:8022/login.html

长连接测试与MIME测试在kATest.md和MIMETest.md中