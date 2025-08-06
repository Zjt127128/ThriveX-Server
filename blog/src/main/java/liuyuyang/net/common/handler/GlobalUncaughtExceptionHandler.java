package liuyuyang.net.common.handler;

import lombok.extern.slf4j.Slf4j;

/**
 * @program: LumpSugarChat
 * @description:
 * @author: zhangjt
 * @create: 2024-10-23 16:30
 **/
@Slf4j
public class GlobalUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        log.error("Exception in thread {} ", t.getName(), e);
    }
}
