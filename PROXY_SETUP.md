# 代理设置指南

## 概述

为了避免在批量URL检测时被目标服务器识别为爬虫，系统支持使用HTTP/SOCKS代理池来隐藏真实IP地址。

## 配置方法

### 1. 在 application.yml 中配置代理

```yaml
proxy:
  enabled: true  # 启用代理
  proxies:
    # HTTP代理
    - host: 127.0.0.1
      port: 8080
      type: HTTP
      username: ""  # 可选：代理用户名
      password: ""  # 可选：代理密码

    # SOCKS代理
    - host: 127.0.0.1
      port: 1080
      type: SOCKS
      username: ""
      password: ""

    # 可以添加更多代理
    - host: proxy.example.com
      port: 3128
      type: HTTP
```

### 2. 禁用代理

```yaml
proxy:
  enabled: false
  proxies: []
```

## 代理获取方式

### 免费代理
1. **ProxyList**: https://www.proxy-list.download/
2. **FreeProxyList**: https://free-proxy-list.net/
3. **ProxyScrape**: https://proxyscrape.com/

### 付费代理服务
1. **Bright Data** (原Luminati)
2. **Oxylabs**
3. **Smartproxy**
4. **ProxyMesh**

### 本地代理工具
1. **Shadowsocks**: 搭建SOCKS5代理
2. **V2Ray**: 多协议代理工具
3. **Clash**: 代理客户端

## 反爬虫措施

系统已实现以下反爬虫措施：

### 1. 随机User-Agent
- 模拟不同浏览器访问
- 包含Chrome、Safari、Firefox等主流浏览器

### 2. 请求间隔控制
- 每个请求间隔1-3秒随机延迟
- 避免请求过于频繁

### 3. 完整浏览器头部
- Accept、Accept-Language、Accept-Encoding等
- 模拟真实浏览器行为

### 4. 代理轮换
- 随机选择代理服务器
- 分散请求来源IP

## 使用建议

### 1. 代理选择
- 优先使用付费代理，稳定性更好
- 免费代理可能不稳定，建议多配置几个
- 选择地理位置分散的代理

### 2. 请求频率
- 不要设置过高的并发数
- 建议单个域名请求间隔至少5秒

### 3. 监控和日志
- 关注日志中的代理使用情况
- 及时更换失效的代理

## 示例配置

### 基础配置（使用本地代理）
```yaml
proxy:
  enabled: true
  proxies:
    - host: 127.0.0.1
      port: 1080
      type: SOCKS
```

### 多代理配置
```yaml
proxy:
  enabled: true
  proxies:
    - host: proxy1.example.com
      port: 8080
      type: HTTP
    - host: proxy2.example.com
      port: 3128
      type: HTTP
    - host: 127.0.0.1
      port: 1080
      type: SOCKS
```

## 注意事项

1. **合法性**: 确保代理使用符合相关法律法规
2. **隐私**: 不要通过不可信的代理发送敏感信息
3. **性能**: 使用代理会增加请求延迟
4. **稳定性**: 定期检查和更新代理列表

## 故障排除

### 代理连接失败
1. 检查代理服务器是否可用
2. 确认防火墙设置
3. 验证代理类型（HTTP/SOCKS）

### 请求被拒绝
1. 更换不同的代理服务器
2. 增加请求间隔时间
3. 检查User-Agent设置

### 性能问题
1. 选择地理位置较近的代理
2. 使用付费代理服务
3. 减少并发请求数量

