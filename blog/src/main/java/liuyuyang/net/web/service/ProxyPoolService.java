package liuyuyang.net.web.service;

import liuyuyang.net.config.ProxyConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class ProxyPoolService {

    @Autowired
    private ProxyConfig proxyConfig;

    // 代理使用计数器
    private final ConcurrentHashMap<String, AtomicInteger> proxyUsageCount = new ConcurrentHashMap<>();

    // 失效代理列表
    private final List<String> invalidProxies = new ArrayList<>();

    /**
     * 获取下一个可用代理
     * 使用轮询策略，避免某个代理被过度使用
     */
    public ProxyConfig.ProxyInfo getNextProxy() {
        if (!proxyConfig.isEnabled() || proxyConfig.getProxies().isEmpty()) {
            return null;
        }

        List<ProxyConfig.ProxyInfo> availableProxies = getAvailableProxies();
        if (availableProxies.isEmpty()) {
            return null;
        }

        // 找到使用次数最少的代理
        ProxyConfig.ProxyInfo selectedProxy = null;
        int minUsage = Integer.MAX_VALUE;

        for (ProxyConfig.ProxyInfo proxy : availableProxies) {
            String proxyKey = proxy.getHost() + ":" + proxy.getPort();
            int usage = proxyUsageCount.computeIfAbsent(proxyKey, k -> new AtomicInteger(0)).get();

            if (usage < minUsage) {
                minUsage = usage;
                selectedProxy = proxy;
            }
        }

        if (selectedProxy != null) {
            String proxyKey = selectedProxy.getHost() + ":" + selectedProxy.getPort();
            proxyUsageCount.get(proxyKey).incrementAndGet();
            log.debug("Selected proxy: {}:{}, usage count: {}",
                    selectedProxy.getHost(), selectedProxy.getPort(),
                    proxyUsageCount.get(proxyKey).get());
        }

        return selectedProxy;
    }

    /**
     * 获取可用代理列表（排除失效代理）
     */
    private List<ProxyConfig.ProxyInfo> getAvailableProxies() {
        List<ProxyConfig.ProxyInfo> available = new ArrayList<>();

        for (ProxyConfig.ProxyInfo proxy : proxyConfig.getProxies()) {
            String proxyKey = proxy.getHost() + ":" + proxy.getPort();
            if (!invalidProxies.contains(proxyKey)) {
                available.add(proxy);
            }
        }

        return available;
    }

    /**
     * 标记代理为失效
     */
    public void markProxyAsInvalid(ProxyConfig.ProxyInfo proxy) {
        if (proxy != null) {
            String proxyKey = proxy.getHost() + ":" + proxy.getPort();
            if (!invalidProxies.contains(proxyKey)) {
                invalidProxies.add(proxyKey);
                log.warn("Marked proxy as invalid: {}", proxyKey);
            }
        }
    }

    /**
     * 测试代理是否可用
     */
    public boolean testProxy(ProxyConfig.ProxyInfo proxy) {
        try {
            URL testUrl = new URL("http://httpbin.org/ip");
            HttpURLConnection connection = (HttpURLConnection) testUrl.openConnection(proxy.toProxy());
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            connection.disconnect();

            return responseCode == 200;
        } catch (IOException e) {
            log.debug("Proxy test failed for {}:{} - {}", proxy.getHost(), proxy.getPort(), e.getMessage());
            return false;
        }
    }

    /**
     * 获取代理使用统计信息
     */
    public String getProxyUsageStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("代理使用统计:\n");

        for (ProxyConfig.ProxyInfo proxy : proxyConfig.getProxies()) {
            String proxyKey = proxy.getHost() + ":" + proxy.getPort();
            int usage = proxyUsageCount.getOrDefault(proxyKey, new AtomicInteger(0)).get();
            boolean isInvalid = invalidProxies.contains(proxyKey);

            stats.append(String.format("  %s - 使用次数: %d, 状态: %s\n",
                proxyKey, usage, isInvalid ? "失效" : "正常"));
        }

        return stats.toString();
    }

    /**
     * 重置所有统计信息
     */
    public void resetStats() {
        proxyUsageCount.clear();
        invalidProxies.clear();
        log.info("代理统计信息已重置");
    }

    /**
     * 获取可用代理数量
     */
    public int getAvailableProxyCount() {
        return getAvailableProxies().size();
    }

    /**
     * 获取总代理数量
     */
    public int getTotalProxyCount() {
        return proxyConfig.getProxies().size();
    }
}