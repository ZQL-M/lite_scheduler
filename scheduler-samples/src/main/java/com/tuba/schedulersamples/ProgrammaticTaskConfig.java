package com.tuba.schedulersamples;

import com.tuba.schedulercore.enums.TimeUnit;
import com.tuba.schedulercore.service.TaskSchedulerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

/**
 * 编程式任务注册配置类
 */
@Configuration
public class ProgrammaticTaskConfig {
    
    private static final Logger log = LoggerFactory.getLogger(ProgrammaticTaskConfig.class);
    
    @Autowired
    private TaskSchedulerService taskSchedulerService;
    
    /**
     * 当Spring容器初始化完成后，注册示例任务
     */
    @EventListener(ContextRefreshedEvent.class)
    public void registerSampleTasks() {
        log.info("开始注册示例任务...");
        
        // 示例1：使用固定频率注册任务（每5秒执行一次）
        ProgrammaticTask task1 = new ProgrammaticTask("FixedRateTask");
        String taskId1 = taskSchedulerService.registerTask(task1, 5, TimeUnit.SECONDS);
        log.info("成功注册固定频率任务，ID: {}", taskId1);
        
        // 示例2：使用cron表达式注册任务（每分钟的第10秒和第40秒执行）
        ProgrammaticTask task2 = new ProgrammaticTask("CronTask");
        String taskId2 = taskSchedulerService.registerTask(task2, "10,40 * * * * ?");
        log.info("成功注册Cron任务，ID: {}", taskId2);
        
        // 示例3：使用固定频率注册任务（每1分钟执行一次）
        ProgrammaticTask task3 = new ProgrammaticTask("MinuteTask");
        String taskId3 = taskSchedulerService.registerTask(task3, 1, TimeUnit.MINUTES);
        log.info("成功注册分钟级任务，ID: {}", taskId3);
        
        log.info("示例任务注册完成！");
    }
}
