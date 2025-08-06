package liuyuyang.net.web.service.impl;

import liuyuyang.net.web.service.ToolService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class ToolServiceImpl implements ToolService {
    @Autowired
    private ThreadPoolTaskExecutor thriveXExecutor;

    @Override
    public List<String> checkUrl(List<String> urls) {
        if (CollectionUtils.isEmpty(urls)) {
            return Collections.emptyList();
        }
        // 创建线程安全的不可用URL列表
        List<String> unavailableUrls = Collections.synchronizedList(new ArrayList<>());
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
                    .get(10, TimeUnit.SECONDS); // 30秒超时
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException e) {
            // 处理异常（可选）
        }
        return unavailableUrls;
    }

    private void check(String urlStr, List<String> unavailableUrls) {
        try {
            log.info("check url: {}", urlStr);
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // 设置请求参数
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);  // 5秒连接超时
            connection.setReadTimeout(5000);     // 5秒读取超时

            int responseCode = connection.getResponseCode();

            // 判断响应码是否成功（2xx或3xx视为可用）
            if (responseCode >= 400) {
                unavailableUrls.add(urlStr);
            }
        } catch (IOException e) {
            unavailableUrls.add(urlStr);
        }
    }
}
