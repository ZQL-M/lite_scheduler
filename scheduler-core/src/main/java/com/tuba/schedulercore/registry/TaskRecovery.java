package com.tuba.schedulercore.registry;

import com.tuba.schedulercore.model.TaskDefinition;
import com.tuba.schedulercore.persistence.TaskPersistenceService;
import com.tuba.schedulercore.scheduler.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 任务恢复器，在应用启动时从数据库加载任务
 */
@Component
public class TaskRecovery {

    private static final Logger log = LoggerFactory.getLogger(TaskRecovery.class);

    @Autowired
    private TaskPersistenceService persistenceService;

    @Autowired
    private TaskRegistry taskRegistry;

    @Autowired
    private Scheduler scheduler;

    /**
     * 当Spring容器初始化完成后调用，从数据库加载并恢复任务
     * 
     * @param event 上下文刷新事件
     */
    @EventListener(ContextRefreshedEvent.class)
    public void recoverTasks() {
        log.info("开始恢复持久化任务...");

        try {
            // 检查持久化服务是否可用
            if (!persistenceService.isAvailable()) {
                log.warn("持久化服务不可用，跳过任务恢复");
                return;
            }

            // 从数据库加载所有启用的持久化任务
            List<TaskDefinition> tasks = persistenceService.findEnabledTasks();
            log.info("从数据库加载到 {} 个启用的持久化任务", tasks.size());

            int recoveredCount = 0;
            for (TaskDefinition task : tasks) {
                try {
                    // 注册任务到注册表
                    taskRegistry.register(task);
                    // 调度任务
                    scheduler.schedule(task);
                    recoveredCount++;
                    log.info("成功恢复任务: {} (ID: {})", task.getName(), task.getId());
                } catch (Exception e) {
                    log.error("恢复任务失败: {} (ID: {})", task.getName(), task.getId(), e);
                }
            }

            log.info("任务恢复完成，成功恢复 {} 个任务", recoveredCount);
        } catch (Exception e) {
            log.error("任务恢复过程中发生异常", e);
        }
    }
}
