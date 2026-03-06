package com.tuba.schedulersamples.excutor;

import com.tuba.schedulercore.model.TaskContext;
import com.tuba.schedulercore.core.task.TubaTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 重试测试任务类
 */
public class RetryTestTask implements TubaTask {

    private static final Logger log = LoggerFactory.getLogger(RetryTestTask.class);
    private int executeCount = 0;

    @Override
    public void execute(TaskContext context) {
        executeCount++;
        log.info("=========================================");
        log.info("[重试测试] >>> 任务开始执行，第{}次执行 <<<", executeCount);
        
        // 前 2 次执行失败，第 3 次执行成功
        if (executeCount < 3) {
            log.error("[重试测试] 任务执行失败，将触发重试");
            throw new RuntimeException("模拟任务执行失败 - 第" + executeCount + "次");
        }
        
        log.info("[重试测试] >>> 任务执行成功 <<<");
        log.info("=========================================");
    }
}
