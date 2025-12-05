# HTTP服务器MIME类型支持测试说明

## 一、测试文件建立说明

### 1.1 创建测试目录结构
在项目根目录下创建 `webroot` 文件夹，用于存放以下测试文件：
webroot/
├── test.html           # 测试HTML MIME类型
├── style.css          # 测试CSS MIME类型
├── data.json          # 测试JSON MIME类型
├── sample.txt         # 测试纯文本 MIME类型
└── images/
└── test.png       # 测试图片 MIME类型
## 二、测试用例说明

### 2.1 测试矩阵

| 序号 | 文件类型 | 文件路径 | 预期Content-Type | 测试重点 |
|------|----------|----------|-----------------|----------|
| 1 | HTML | `/index.html` | `text/html` | 验证HTML渲染和字符集 |
| 2 | CSS | `/style.css` | `text/css` | 验证样式表解析 |
| 3 | JSON | `/data.json` | `application/json` | 验证JSON格式和自动格式化 |
| 4 | 纯文本 | `/sample.txt` | `text/plain` | 验证纯文本显示 |
| 5 | PNG图片 | `/test.png` | `image/png` | 验证二进制文件传输 |

### 2.2 测试环境要求
- 服务器运行在 `localhost:8080`
- Postman 已安装并可用
- 服务器已正确启动并监听端口

## 三、详细测试步骤

### 3.1 准备工作
1. 确保服务器已启动
2. 打开Postman应用程序
3. 准备记录测试结果

### 3.2 HTML文件测试

**步骤：**
1. 在Postman中新建一个请求
2. 设置请求方法为 **GET**
3. 输入URL: `http://localhost:8080/index.html`
4. 点击 **Send** 按钮

**验证要点：**
- 查看响应状态码应为 **200**
- 查看Headers中的 **Content-Type** 应为 `text/html`
- 查看Body部分，HTML应被格式化显示（非原始文本）

### 3.3 CSS文件测试

**步骤：**
1. 新建GET请求
2. URL: `http://localhost:8080/style.css`
3. 发送请求

**验证要点：**
- Content-Type: `text/css`
- 响应体应显示CSS代码

### 3.4 JSON文件测试

**步骤：**
1. 新建GET请求
2. URL: `http://localhost:8080/data.json`
3. 发送请求

**验证要点：**
- Content-Type: `application/json`

### 3.5 纯文本文件测试

**步骤：**
1. 新建GET请求
2. URL: `http://localhost:8080/sample.txt`
3. 发送请求

**验证要点：**
- Content-Type: `text/plain; charset=UTF-8`
- 响应体应显示原始文本
- 空行和特殊字符应正确显示
- 中文字符无乱码

### 3.6 PNG图片测试

**步骤：**
1. 新建GET请求
2. URL: `http://localhost:8080/test.png`
3. 发送请求

**验证要点：**
- Content-Type: `image/png`
- Postman可能显示二进制数据或预览图片
- 可以点击 **Send and Download** 下载图片验证完整性