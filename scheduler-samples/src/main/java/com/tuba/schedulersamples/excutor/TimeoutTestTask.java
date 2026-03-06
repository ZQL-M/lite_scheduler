package com.tuba.schedulersamples.excutor;

import com.tuba.schedulercore.model.TaskContext;
import com.tuba.schedulercore.core.task.TubaTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 超时测试任务类
 */
public class TimeoutTestTask implements TubaTask {

    private static final Logger log = LoggerFactory.getLogger(TimeoutTestTask.class);

    @Override
    public void execute(TaskContext context) {
        log.info("=========================================");
        log.info("[超时测试] >>> 任务开始执行，将执行 10 秒 <<<");
        
        // 模拟长时间执行的任务
        try {
            Thread.sleep(10000); // 10 秒
        } catch (InterruptedException e) {
            log.error("任务执行被中断", e);
            Thread.currentThread().interrupt();
        }
        
        log.info("[超时测试] >>> 任务执行完成 <<<");
        log.info("=========================================");
    }
}
