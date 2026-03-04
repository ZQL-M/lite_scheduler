package com.tuba.schedulercore.core.registry;

import com.tuba.schedulercore.config.properties.RecoveryProperties;
import com.tuba.schedulercore.model.TaskDefinition;
import com.tuba.schedulercore.core.persistence.TaskPersistenceService;
import com.tuba.schedulercore.core.scheduler.Scheduler;
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
        log.info("开始从数据库恢复任务，恢复模式: {}", recoveryProperties.getMode());

        try {
            // 从数据库加载所有持久化任务
            List<TaskDefinition> tasks = persistenceService.findAll();
            log.info("从数据库加载到 {} 个持久化任务", tasks.size());

            int recoveredCount = 0;
            for (TaskDefinition task : tasks) {
                // 只恢复未删除的任务
                if (!task.isDeleted() && task.isEnabled()) {
                    // 尝试验证任务类（如果需要）
                    try {
                        TaskUtils.validateTaskClass(task);
                    } catch (Exception e) {
                        log.warn("验证任务 {} (ID: {}) 的任务类时出错: {}",
                                task.getName(), task.getId(), e.getMessage());
                    }

                    // 注册任务到注册表
                    taskRegistry.register(task);
                    recoveredCount++;
                    log.debug("已恢复任务: {} (ID: {})", task.getName(), task.getId());
                }
            }

            log.info("任务恢复完成，共恢复 {} 个任务", recoveredCount);
        } catch (Exception e) {
            log.error("任务恢复过程中发生异常", e);
        }
    }
}
