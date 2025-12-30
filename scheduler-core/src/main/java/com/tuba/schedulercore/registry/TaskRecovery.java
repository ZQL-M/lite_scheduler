package com.tuba.schedulercore.registry;

import com.tuba.schedulercore.config.RecoveryProperties;
import com.tuba.schedulercore.model.TaskDefinition;
import com.tuba.schedulercore.persistence.TaskPersistenceService;
import com.tuba.schedulercore.scheduler.Scheduler;
import com.tuba.schedulercore.utils.TaskUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 任务恢复器，在应用启动时从数据库加载任务
 */
@Component
public class TaskRecovery implements ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(TaskRecovery.class);

    private ApplicationContext applicationContext;

    @Resource
    private TaskPersistenceService persistenceService;

    @Resource
    private TaskRegistry taskRegistry;

    @Resource
    private Scheduler scheduler;

    @Resource
    private RecoveryProperties recoveryProperties;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 当Spring容器初始化完成后调用，从数据库加载任务并注册到注册表
     */
    @EventListener(ContextRefreshedEvent.class)
    public void recoverTasks() {
        log.info("开始从数据库恢复任务");

        try {
            // 从数据库加载所有持久化任务
            List<TaskDefinition> tasks = persistenceService.findAll();
            log.info("从数据库加载到 {} 个持久化任务", tasks.size());

            int recoveredCount = 0;
            for (TaskDefinition task : tasks) {
                try {
                    // 解析bean和method
                    if (parseBeanAndMethod(task)) {
                        // 注册任务到注册表
                        taskRegistry.register(task);
                        
                        // 调度任务
                        scheduler.schedule(task);
                        
                        recoveredCount++;
                        log.info("已恢复任务: {} (ID: {})",
                                task.getName(), task.getId());
                    } else {
                        log.error("恢复任务失败: {} (ID: {}) - 无法解析bean或method",
                                task.getName(), task.getId());
                    }
                } catch (Exception e) {
                    log.error("恢复任务失败: {} (ID: {})");
                }
            }

            log.info("任务恢复完成，共恢复 {} 个任务", recoveredCount);
        } catch (Exception e) {
            log.error("任务恢复过程中发生异常", e);
        }
    }

    /**
     * 解析bean和method对象
     * 
     * @param task 任务定义
     * @return 是否解析成功
     */
    private boolean parseBeanAndMethod(TaskDefinition task) {
        return TaskUtils.parseBeanAndMethod(applicationContext, task);
    }
}
