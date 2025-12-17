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
import java.time.LocalDateTime;
import java.util.List;

import com.tuba.schedulercore.enums.TaskStatus;

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
     * 当Spring容器初始化完成后调用，检查并处理过期任务
     * 根据配置模式执行不同的恢复逻辑
     */
    @EventListener(ContextRefreshedEvent.class)
    public void recoverTasks() {
        log.info("开始检查过期任务，恢复模式: {}", recoveryProperties.getMode());

        try {
            // 获取当前时间
            LocalDateTime now = LocalDateTime.now();

            // 从数据库加载所有持久化任务
            List<TaskDefinition> tasks = persistenceService.findAll();
            log.info("从数据库加载到 {} 个持久化任务", tasks.size());

            int processedCount = 0;
            for (TaskDefinition task : tasks) {
                // 检查任务是否已到期（下次触发时间小于当前时间）
                if (task.getNextFireTime() != null && task.getNextFireTime().isBefore(now)) {
                    // 根据模式执行不同的恢复逻辑
                    switch (recoveryProperties.getMode()) {
                        case FAILURE:
                            processedCount += handleFailureMode(task);
                            break;
                        case COMPENSATION:
                            processedCount += handleCompensationMode(task);
                            break;
                    }
                }
            }

            log.info("过期任务检查完成，共处理 {} 个任务", processedCount);
        } catch (Exception e) {
            log.error("过期任务检查过程中发生异常", e);
        }
    }

    /**
     * 处理FAILURE模式：将过期任务状态改为失败
     */
    private int handleFailureMode(TaskDefinition task) {
        // 检查任务状态是否为待执行或未执行
        if (task.getStatus() == TaskStatus.PENDING || task.getStatus() == TaskStatus.NOT_EXECUTED) {
            // 记录过期任务信息
            log.warn("发现过期任务: {} (ID: {}), 预期执行时间: {}, 当前状态: {}",
                    task.getName(), task.getId(), task.getNextFireTime(), task.getStatus());

            // 将任务状态修改为失败
            task.setStatus(TaskStatus.FAILED);

            // 更新任务到注册表
            taskRegistry.register(task);

            // 如果持久化服务可用，更新到数据库
            if (persistenceService.isAvailable()) {
                try {
                    persistenceService.update(task);
                    log.info("已将过期任务状态修改为失败: {} (ID: {})",
                            task.getName(), task.getId());
                    return 1;
                } catch (Exception e) {
                    log.error("更新过期任务状态失败: {} (ID: {})", task.getName(), task.getId(), e);
                }
            } else {
                log.warn("持久化服务不可用，仅在内存中更新任务状态: {} (ID: {})",
                        task.getName(), task.getId());
                return 1;
            }
        }
        return 0;
    }

    /**
     * 处理COMPENSATION模式：对过期任务进行补偿执行
     */
    private int handleCompensationMode(TaskDefinition task) {
        // 检查任务状态是否未完成
        if (task.getStatus() != TaskStatus.COMPLETED) {
            // 记录过期任务信息
            log.warn("发现过期任务: {} (ID: {}), 预期执行时间: {}, 当前状态: {}",
                    task.getName(), task.getId(), task.getNextFireTime(), task.getStatus());

            try {
                // 解析bean和method
                if (parseBeanAndMethod(task)) {
                    // 更新任务状态为待执行
                    task.setStatus(TaskStatus.PENDING);

                    // 更新任务到注册表
                    taskRegistry.register(task);

                    // 立即触发执行（补偿执行）
                    scheduler.triggerNow(task);

                    // 如果持久化服务可用，更新到数据库
                    if (persistenceService.isAvailable()) {
                        try {
                            persistenceService.update(task);
                        } catch (Exception e) {
                            log.error("更新过期任务状态失败: {} (ID: {})", task.getName(), task.getId(), e);
                        }
                    }

                    log.info("已对过期任务进行补偿执行: {} (ID: {})",
                            task.getName(), task.getId());
                    return 1;
                } else {
                    log.error("补偿执行任务失败: {} (ID: {}) - 无法解析bean或method",
                            task.getName(), task.getId());
                }
            } catch (Exception e) {
                log.error("补偿执行任务失败: {} (ID: {})", task.getName(), task.getId(), e);
            }
        }
        return 0;
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
