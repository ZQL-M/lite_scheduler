package com.tuba.schedulersamples.excutor;

import com.tuba.schedulercore.model.TaskContext;
import com.tuba.schedulercore.core.task.TubaTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 测试任务类，使用依赖注入
 */
@Component
public class DependencyInjectionTestTask implements TubaTask {

    private static final Logger log = LoggerFactory.getLogger(DependencyInjectionTestTask.class);

    // 注入一个服务，演示依赖注入功能
    @Autowired
    private TestService testService;

    @Override
    public void execute(TaskContext context) {
        log.info("=========================================");
        log.info("[依赖注入测试] >>> 任务开始执行 <<<");
        log.info("使用依赖注入的服务：{}", testService.getMessage());
        
        // 模拟任务执行
        try {
            // 模拟正常任务，执行 1 秒
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.error("任务执行被中断", e);
            Thread.currentThread().interrupt();
        }
        
        log.info("[依赖注入测试] >>> 任务执行完成 <<<");
        log.info("=========================================");
    }
}
