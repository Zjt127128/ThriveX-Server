package liuyuyang.net.common.factory;

import liuyuyang.net.common.handler.GlobalUncaughtExceptionHandler;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadFactory;

/**
 * @program: LumpSugarChat
 * @description:
 * @author: zhangjt
 * @create: 2024-10-23 16:26
 **/
@AllArgsConstructor
public class MyThreadFactory implements ThreadFactory {

    private ThreadPoolTaskExecutor executor;

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = executor.newThread(r);
        thread.setUncaughtExceptionHandler(new GlobalUncaughtExceptionHandler());
        return thread;
    }
}
