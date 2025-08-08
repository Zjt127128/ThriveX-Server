package liuyuyang.net.web.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import liuyuyang.net.common.annotation.NoTokenRequired;
import liuyuyang.net.config.ProxyConfig;
import liuyuyang.net.web.service.ProxyPoolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Api(tags = "代理管理")
@RestController
@RequestMapping("/proxy")
public class ProxyController {

    @Autowired
    private ProxyPoolService proxyPoolService;

    @Autowired
    private ProxyConfig proxyConfig;

    @ApiOperation("获取代理池状态")
    @NoTokenRequired
    @GetMapping("/status")
    public Map<String, Object> getProxyStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", proxyConfig.isEnabled());
        status.put("totalProxies", proxyPoolService.getTotalProxyCount());
        status.put("availableProxies", proxyPoolService.getAvailableProxyCount());
        status.put("usageStats", proxyPoolService.getProxyUsageStats());
        return status;
    }

    @ApiOperation("测试指定代理")
    @NoTokenRequired
    @PostMapping("/test")
    public Map<String, Object> testProxy(@RequestParam String host, @RequestParam int port,
                                        @RequestParam(defaultValue = "HTTP") String type) {
        ProxyConfig.ProxyInfo proxy = new ProxyConfig.ProxyInfo();
        proxy.setHost(host);
        proxy.setPort(port);
        proxy.setType(type);

        boolean isWorking = proxyPoolService.testProxy(proxy);

        Map<String, Object> result = new HashMap<>();
        result.put("proxy", host + ":" + port);
        result.put("type", type);
        result.put("working", isWorking);
        return result;
    }

    @ApiOperation("重置代理统计信息")
    @NoTokenRequired
    @PostMapping("/reset")
    public Map<String, String> resetStats() {
        proxyPoolService.resetStats();
        Map<String, String> result = new HashMap<>();
        result.put("message", "代理统计信息已重置");
        return result;
    }

    @ApiOperation("获取下一个可用代理")
    @NoTokenRequired
    @GetMapping("/next")
    public Map<String, Object> getNextProxy() {
        ProxyConfig.ProxyInfo proxy = proxyPoolService.getNextProxy();
        Map<String, Object> result = new HashMap<>();

        if (proxy != null) {
            result.put("host", proxy.getHost());
            result.put("port", proxy.getPort());
            result.put("type", proxy.getType());
            result.put("available", true);
        } else {
            result.put("available", false);
            result.put("message", "没有可用的代理");
        }

        return result;
    }
}

