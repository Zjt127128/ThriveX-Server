package liuyuyang.net.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@ConfigurationProperties(prefix = "proxy")
@Data
public class ProxyConfig {

    private List<ProxyInfo> proxies = new ArrayList<>();
    private boolean enabled = false;

    @Data
    public static class ProxyInfo {
        private String host;
        private int port;
        private String username;
        private String password;
        private String type = "HTTP"; // HTTP, SOCKS

        public Proxy toProxy() {
            Proxy.Type proxyType = "SOCKS".equalsIgnoreCase(type) ?
                Proxy.Type.SOCKS : Proxy.Type.HTTP;
            return new Proxy(proxyType, new InetSocketAddress(host, port));
        }
    }

    /**
     * 随机获取一个代理
     */
    public ProxyInfo getRandomProxy() {
        if (proxies.isEmpty()) {
            return null;
        }
        Random random = new Random();
        return proxies.get(random.nextInt(proxies.size()));
    }

    /**
     * 获取所有可用代理
     */
    public List<ProxyInfo> getAllProxies() {
        return new ArrayList<>(proxies);
    }
}

