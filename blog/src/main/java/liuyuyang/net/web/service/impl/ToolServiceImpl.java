package liuyuyang.net.web.service.impl;

import liuyuyang.net.enums.UrlValidationStatus;
import liuyuyang.net.vo.TooLVo;
import liuyuyang.net.web.service.ToolService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ToolServiceImpl implements ToolService {

    // 用户代理池，模拟不同浏览器访问
    private static final String[] USER_AGENTS = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.1 Safari/605.1.15",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:89.0) Gecko/20100101 Firefox/89.0",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.107 Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 14_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.1 Mobile/15E148 Safari/604.1"
    };

    // 超时设置（毫秒）
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 5000;

    // 请求间隔设置（毫秒）
    private static final int REQUEST_DELAY_MIN = 1000; // 最小间隔1秒
    private static final int REQUEST_DELAY_MAX = 3000; // 最大间隔3秒

    @Autowired
    private ThreadPoolTaskExecutor thriveXExecutor;

    @Autowired
    private liuyuyang.net.config.ProxyConfig proxyConfig;

    @Autowired
    private liuyuyang.net.web.service.ProxyPoolService proxyPoolService;

    @Override
    public Map<String, List<Map<String, Object>>> checkUrl(List<String> urls) {
        if (CollectionUtils.isEmpty(urls)) {
            return Collections.emptyMap();
        }
        // 创建线程安全的不可用URL集合Map
        List<Map<String,Object>> unavailableUrls = Collections.synchronizedList(new ArrayList<>());
        // 创建Future集合
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (String url : urls) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                check(url, unavailableUrls);
            }, thriveXExecutor);

            futures.add(future);
        }

        // 等待所有任务完成
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(60, TimeUnit.SECONDS); // 30秒超时
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException e) {
            // 处理异常（可选）
        }

        // 按照 responseCode_status 进行分组
        Map<String, List<Map<String, Object>>> groupedResults = unavailableUrls.stream()
                .collect(Collectors.groupingBy(result -> {
                    String responseCode = String.valueOf(result.get("responseCode"));
                    String status = String.valueOf(result.get("status"));
                    return responseCode + "_" + status;
                }));
        log.info("unavailableUrls.size: {}", unavailableUrls.size());

        // 输出代理使用统计
        if (proxyConfig.isEnabled()) {
            log.info("代理池状态 - 可用代理: {}/{}, 统计信息:\n{}",
                proxyPoolService.getAvailableProxyCount(),
                proxyPoolService.getTotalProxyCount(),
                proxyPoolService.getProxyUsageStats());
        }

        return groupedResults;
    }

    @Override
    public Map<String, List<Map<String, Object>>> checkInfo(TooLVo tooLVo) {
        if (Objects.isNull(tooLVo) || CollectionUtils.isEmpty(tooLVo.getBookmarks())) {
            return Collections.emptyMap();
        }
        // 获取tooLVo中所有的url，组成一个新的list
        List<String> urls = new ArrayList<>();
        extractUrlsRecursively(tooLVo.getBookmarks(), urls);
        return checkUrl(urls.stream().distinct().collect(Collectors.toList()));
    }

    /**
     * 递归提取所有书签中的URL
     * @param bookmarks 书签列表
     * @param urls 用于收集URL的列表
     */
    private void extractUrlsRecursively(List<liuyuyang.net.vo.tool.BookMarksInfo> bookmarks, List<String> urls) {
        if (CollectionUtils.isEmpty(bookmarks)) {
            return;
        }

        for (liuyuyang.net.vo.tool.BookMarksInfo bookmark : bookmarks) {
            // 如果当前书签有URL，添加到列表中
            if (bookmark.getUrl() != null && !bookmark.getUrl().trim().isEmpty()) {
                urls.add(bookmark.getUrl());
            }

            // 递归处理子书签
            if (!CollectionUtils.isEmpty(bookmark.getChildren())) {
                extractUrlsRecursively(bookmark.getChildren(), urls);
            }
        }
    }

    private void check(String urlStr, List<Map<String,Object>> unavailableUrls) {
        try {
            // 添加随机延迟，避免被识别为爬虫
            Random random = new Random();
            int delay = random.nextInt(REQUEST_DELAY_MAX - REQUEST_DELAY_MIN + 1) + REQUEST_DELAY_MIN;
            Thread.sleep(delay);

            log.info("check url: {}", urlStr);
            Map<String, Object> result = validateUrl(urlStr);
            if (!result.get("valid").equals(true)) {
                unavailableUrls.add(result);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while checking URL: {}", urlStr, e);
        }
    }

    public Map<String, Object> validateUrl(String urlString) {
        Map<String, Object> result = new HashMap<>();
        result.put("url", urlString);

        // 初始化为未验证状态
        UrlValidationStatus status = UrlValidationStatus.NOT_VERIFIED;
        int responseCode = -1;

        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);

            // 使用代理池获取代理（如果配置了）
            liuyuyang.net.config.ProxyConfig.ProxyInfo proxyInfo = null;
            if (proxyConfig.isEnabled()) {
                proxyInfo = proxyPoolService.getNextProxy();
                if (proxyInfo != null) {
                    try {
                        connection = (HttpURLConnection) url.openConnection(proxyInfo.toProxy());
                        log.debug("Using proxy: {}:{}", proxyInfo.getHost(), proxyInfo.getPort());
                    } catch (Exception e) {
                        log.warn("Failed to connect through proxy {}:{}, marking as invalid",
                            proxyInfo.getHost(), proxyInfo.getPort());
                        proxyPoolService.markProxyAsInvalid(proxyInfo);
                        // 回退到直连
                        connection = (HttpURLConnection) url.openConnection();
                        proxyInfo = null; // 重置代理信息
                    }
                } else {
                    connection = (HttpURLConnection) url.openConnection();
                }
            } else {
                connection = (HttpURLConnection) url.openConnection();
            }

            // 随机选择用户代理
            Random random = new Random();
            String userAgent = USER_AGENTS[random.nextInt(USER_AGENTS.length)];
            connection.setRequestProperty("User-Agent", userAgent);

            // 添加更多请求头，模拟真实浏览器
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            connection.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
            connection.setRequestProperty("Connection", "keep-alive");
            connection.setRequestProperty("Upgrade-Insecure-Requests", "1");
            connection.setRequestProperty("Cache-Control", "max-age=0");

            // 设置超时
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);

            // 发送HEAD请求（更高效）
            connection.setRequestMethod("HEAD");

            // 获取响应码
            responseCode = connection.getResponseCode();

            // 根据响应码确定状态
            if (responseCode >= 200 && responseCode < 400) {
                // 2xx和3xx状态码通常表示成功
                status = UrlValidationStatus.VALID_AND_ACCESSIBLE;
            } else if (responseCode == 401 || responseCode == 403) {
                // 401/403可能是反爬机制，需要进一步验证
                return handleAntiScrapingCase(urlString);
            } else if (responseCode == 429) {
                // 请求过多，稍后重试
                status = UrlValidationStatus.RATE_LIMITED;
                TimeUnit.SECONDS.sleep(2);
                return validateUrl(urlString); // 递归重试
            } else {
                // 其他4xx/5xx状态码，根据具体码值确定状态
                status = UrlValidationStatus.fromResponseCode(responseCode);
            }

        } catch (IOException | InterruptedException e) {
            // 根据异常类型确定状态
            status = UrlValidationStatus.fromException(e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        // 设置返回结果
        result.put("valid", status.isValid());
        result.put("status", status.getMessage());
        result.put("responseCode", responseCode);

        return result;
    }

    // 处理反爬机制的情况
    private Map<String, Object> handleAntiScrapingCase(String urlString) {
        Map<String, Object> result = new HashMap<>();
        result.put("url", urlString);
        result.put("responseCode", 401); // 假设是401

        try {
            // 使用GET请求获取完整内容
            URL url = new URL(urlString);
            HttpURLConnection connection = null;

            // 使用代理池获取代理（如果配置了）
            liuyuyang.net.config.ProxyConfig.ProxyInfo proxyInfo = null;
            if (proxyConfig.isEnabled()) {
                proxyInfo = proxyPoolService.getNextProxy();
                if (proxyInfo != null) {
                    try {
                        connection = (HttpURLConnection) url.openConnection(proxyInfo.toProxy());
                        log.debug("Using proxy for anti-scraping case: {}:{}", proxyInfo.getHost(), proxyInfo.getPort());
                    } catch (Exception e) {
                        log.warn("Failed to connect through proxy {}:{} for anti-scraping case",
                            proxyInfo.getHost(), proxyInfo.getPort());
                        proxyPoolService.markProxyAsInvalid(proxyInfo);
                        connection = (HttpURLConnection) url.openConnection();
                    }
                } else {
                    connection = (HttpURLConnection) url.openConnection();
                }
            } else {
                connection = (HttpURLConnection) url.openConnection();
            }

            // 设置随机用户代理
            Random random = new Random();
            String userAgent = USER_AGENTS[random.nextInt(USER_AGENTS.length)];
            connection.setRequestProperty("User-Agent", userAgent);

            // 设置超时
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);

            // 获取响应内容
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()))) {

                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }

                // 分析内容以确定是否是真正的错误
                String htmlContent = content.toString();
                UrlValidationStatus status;
                if (isValidContent(htmlContent)) {
                    status = UrlValidationStatus.VALID_WITH_ANTI_SCRAPING;
                } else {
                    status = UrlValidationStatus.INVALID_WITH_ANTI_SCRAPING;
                }

                result.put("valid", status.isValid());
                result.put("status", status.getMessage());
            }

        } catch (IOException e) {
            UrlValidationStatus status = UrlValidationStatus.fromException(e);
            result.put("valid", status.isValid());
            result.put("status", status.getMessage());
        }

        return result;
    }

    // 检查内容是否有效（简单实现）
    private static boolean isValidContent(String htmlContent) {
        // 检查常见错误页面特征
        if (htmlContent.contains("Access Denied") ||
                htmlContent.contains("Forbidden") ||
                htmlContent.contains("Unauthorized")) {
            return false;
        }

        // 检查有效页面特征
        if (htmlContent.contains("<html") ||
                htmlContent.contains("<title") ||
                htmlContent.contains("<body")) {
            return true;
        }

        // 检查内容长度（空页面通常无效）
        return htmlContent.length() > 500;
    }
}
