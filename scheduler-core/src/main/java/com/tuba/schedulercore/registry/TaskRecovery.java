package com.tuba.schedulercore.registry;

import com.tuba.schedulercore.model.TaskDefinition;
import com.tuba.schedulercore.persistence.TaskPersistenceService;
import com.tuba.schedulercore.scheduler.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Method;
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

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 当Spring容器初始化完成后调用，从数据库加载并恢复任务
     *
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
                    // 解析bean和method
                    if (parseBeanAndMethod(task)) {
                        // 注册任务到注册表
                        taskRegistry.register(task);
                        // 调度任务
                        scheduler.schedule(task);
                        recoveredCount++;
                        log.info("成功恢复任务: {} (ID: {})", task.getName(), task.getId());
                    } else {
                        log.error("恢复任务失败: {} (ID: {}) - 无法解析bean或method", task.getName(), task.getId());
                    }
                } catch (Exception e) {
                    log.error("恢复任务失败: {} (ID: {})", task.getName(), task.getId(), e);
                }
            }

            log.info("任务恢复完成，成功恢复 {} 个任务", recoveredCount);
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
        try {
            if (task.getBeanName() == null || task.getBeanName().isEmpty()) {
                log.error("任务 {} (ID: {}) 的beanName为空", task.getName(), task.getId());
                return false;
            }

            if (task.getMethodName() == null || task.getMethodName().isEmpty()) {
                log.error("任务 {} (ID: {}) 的methodName为空", task.getName(), task.getId());
                return false;
            }

            // 从ApplicationContext获取bean实例
            Object bean = applicationContext.getBean(task.getBeanName());
            if (bean == null) {
                log.error("无法从ApplicationContext获取bean: {}", task.getBeanName());
                return false;
            }

            // 解析method对象
            Method[] methods = bean.getClass().getDeclaredMethods();
            Method targetMethod = null;
            for (Method method : methods) {
                if (method.getName().equals(task.getMethodName())) {
                    // 检查方法签名是否匹配（这里简化处理，实际应该检查参数类型）
                    if (method.getParameterCount() == 0 ||
                            (method.getParameterCount() == 1 &&
                                    com.tuba.schedulercore.model.TaskContext.class
                                            .isAssignableFrom(method.getParameterTypes()[0]))) {
                        targetMethod = method;
                        break;
                    }
                }
            }

            if (targetMethod == null) {
                log.error("无法找到匹配的方法: {} 上的 {}", task.getBeanName(), task.getMethodName());
                return false;
            }

            // 设置bean和method
            task.setBean(bean);
            task.setMethod(targetMethod);
            return true;
        } catch (Exception e) {
            log.error("解析bean和method失败: {}", e.getMessage(), e);
            return false;
        }
    }
}
