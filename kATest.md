# 测试文件

# 代码修改

## RequestHandler.java
增加了两个成员属性`requestCount`和`MAX_REQUESTS_PER_CONNECTION`

作用是设置了一个连接请求数的上限，即每次连接最多处理`MAX_REQUESTS_PER_CONNECTION`个请求。

修改了`run`方法，主要是使得服务端更好的实现长连接，以及增加了一些输出来检测状态，可以选择删掉这些输出。

增加了`shouldKeepAlive`方法，用于在`run`方法中根据请求的头部来判断是否需要保持长连接。

修改了`closeConnection`方法，在关闭资源时增加了一些判断，不是必须的，可以不采用。

对`processRequest`方法做了一点修改，如果遇到不支持的请求将不会保持长连接，如果认为仍然需要保持那么不要应用这里的修改。

## HttpClient.java

增加了一些成员属性：

- `private Socket connection` 建立了长连接后的socket
- `private OutputStream out` 用于向connection写入
- `private InputStream in` 用于从connection读取
- `private boolean connected = false` 标记是否建立了长连接

这些成员变量我写了两个方法来管理:

- `public void connect() throws IOException` 建立长连接
- `public void disconnect() throws IOException` 关闭长连接

此外，我增加了一个用于长连接的发送请求的方法:
`private HttpResponse sendRequestWithKeepAlive(HttpRequest request) throws IOException`

使用这个方法发送请求时会复用`Socket connection`，因此使用前需要先建立长连接。

之后就是用于测试长连接的方法`private static void keepTestClient(HttpClient client)`

在我本地环境下测试输出如下：

```
6. test keep alive
6.1 等待一段时间(应在长链接保持时间内)再发送请求
第一次请求，建立长连接：
花费的时间: 2ms
  Status: 200 OK
     <meta name="viewport" con...
  Body length: 2696 characters
接下来等待一段时间(3s)
发送第二次请求，时间应该较短
花费的时间: 1ms
  Status: 200 OK
     <meta name="viewport" con...
  Body length: 2696 characters
6.2 尝试在一个长连接上连续发送10个请求
第0次请求
花费的时间: 1ms
第1次请求
花费的时间: 1ms
第2次请求
花费的时间: 1ms
第3次请求
花费的时间: 1ms
第4次请求
花费的时间: 1ms
第5次请求
花费的时间: 1ms
第6次请求
花费的时间: 1ms
第7次请求
花费的时间: 1ms
第8次请求
花费的时间: 1ms
第9次请求
花费的时间: 1ms
总时间: 10ms
6.3 与短链接测试对比
进行10次短链接(GET)
第0次请求
花费的时间: 2ms
第1次请求
花费的时间: 3ms
第2次请求
花费的时间: 2ms
第3次请求
花费的时间: 2ms
第4次请求
花费的时间: 1ms
第5次请求
花费的时间: 1ms
第6次请求
花费的时间: 1ms
第7次请求
花费的时间: 2ms
第8次请求
花费的时间: 2ms
第9次请求
花费的时间: 1ms
总时间: 17ms
长连接用时比短连接少，效率提高：41.17647058823529%
6.4 长连接超时测试
等待一段时间(应在大于长链接保持时间)再发送请求
接下来等待一段时间(6s)
超时后发送请求
捕获错误信息：你的主机中的软件中止了一个已建立的连接。
如果连接断开则符合预期
```

从结果上来看已经较好的实现了长连接。
