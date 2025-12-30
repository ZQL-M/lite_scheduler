package com.tuba.schedulercore.service.impl;

import com.tuba.schedulercore.config.PersistenceProperties;
import com.tuba.schedulercore.enums.TimeUnit;
import com.tuba.schedulercore.model.TaskDefinition;
import com.tuba.schedulercore.persistence.TaskPersistenceService;
import com.tuba.schedulercore.registry.TaskRegistry;
import com.tuba.schedulercore.scheduler.Scheduler;
import com.tuba.schedulercore.service.TaskSchedulerService;
import com.tuba.schedulercore.service.TaskStatusService;
import com.tuba.schedulercore.service.TaskTriggerService;
import com.tuba.schedulercore.task.Task;
import com.tuba.schedulercore.task.TaskAdapter;
import com.tuba.schedulercore.utils.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * 任务调度服务实现类
 * <p>
 * 负责处理任务的注册、触发、暂停、恢复和取消等核心调度功能，
 * 支持多种任务类型（Task、Runnable、Callable）和调度方式（Cron表达式、固定频率、固定延迟）
 * </p>
 */
@Service
public class TaskSchedulerServiceImpl implements TaskSchedulerService {

    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(TaskSchedulerServiceImpl.class);

    /**
     * 任务适配器，用于创建任务定义
     */
    private final TaskAdapter taskAdapter;

    /**
     * 任务注册表，用于管理所有注册的任务
     */
    private final TaskRegistry taskRegistry;

    /**
     * 调度器，负责实际的任务调度执行
     */
    private final Scheduler scheduler;

    /**
     * 任务持久化服务，用于将任务保存到数据库
     */
    private final TaskPersistenceService persistenceService;

    /**
     * 持久化配置属性
     */
    private final PersistenceProperties persistenceProperties;

    /**
     * 任务触发器服务，用于管理任务触发器
     */
    private final TaskTriggerService taskTriggerService;

    /**
     * 任务状态服务，用于管理任务执行状态
     */
    private final TaskStatusService taskStatusService;

    /**
     * 构造方法
     *
     * @param taskAdapter           任务适配器，用于创建任务定义
     * @param taskRegistry          任务注册表，用于管理所有注册的任务
     * @param scheduler             调度器，负责实际的任务调度执行
     * @param persistenceService    任务持久化服务，用于将任务保存到数据库
     * @param persistenceProperties 持久化配置属性
     * @param taskTriggerService    任务触发器服务，用于管理任务触发器
     * @param taskStatusService     任务状态服务，用于管理任务执行状态
     */
    @Autowired
    public TaskSchedulerServiceImpl(TaskAdapter taskAdapter,
            TaskRegistry taskRegistry,
            Scheduler scheduler,
            TaskPersistenceService persistenceService,
            PersistenceProperties persistenceProperties,
            TaskTriggerService taskTriggerService,
            TaskStatusService taskStatusService) {
        this.taskAdapter = taskAdapter;
        this.taskRegistry = taskRegistry;
        this.scheduler = scheduler;
        this.persistenceService = persistenceService;
        this.persistenceProperties = persistenceProperties;
        this.taskTriggerService = taskTriggerService;
        this.taskStatusService = taskStatusService;
        log.info("TaskSchedulerServiceImpl初始化: persistence.type={}, persistenceService.class={}",
                persistenceProperties.getType(),
                persistenceService.getClass().getName());
    }

    /**
     * 注册Task类型任务
     *
     * @param task       Task任务实例
     * @param definition 任务定义
     * @return 注册成功的任务ID
     */
    @Override
    public String registerTask(Task task, TaskDefinition definition) {
        // 参数验证
        validateTask(task, definition);
        // 设置Task实例和方法
        if (definition.getBean() == null) {
            TaskDefinition newDefinition = taskAdapter.createTaskDefinition(task);
            definition.setBean(newDefinition.getBean());
            definition.setMethod(newDefinition.getMethod());
            // 复制beanName和methodName
            if (definition.getBeanName() == null) {
                definition.setBeanName(newDefinition.getBeanName());
            }
            if (definition.getMethodName() == null) {
                definition.setMethodName(newDefinition.getMethodName());
            }
        }
        // 注册任务核心逻辑
        return registerTaskCore(definition, true);
    }

    /**
     * 使用Cron表达式注册Task类型任务
     *
     * @param task Task任务实例
     * @param cron Cron表达式
     * @return 注册成功的任务ID
     */
    @Override
    public String registerTask(Task task, String cron) {
        validateCron(cron);

        TaskDefinition definition = createTaskDefinition(task, "-Cron");
        String taskId = registerTask(task, definition);
        // 创建Cron触发器
        taskTriggerService.createCronTrigger(taskId, cron, null, null, null, null, null);
        return taskId;
    }

    /**
     * 使用固定频率注册Task类型任务
     *
     * @param task     Task任务实例
     * @param interval 时间间隔
     * @param timeUnit 时间单位
     * @return 注册成功的任务ID
     */
    @Override
    public String registerTask(Task task, long interval, TimeUnit timeUnit) {
        validateInterval(interval, timeUnit);

        long millisInterval = TimeUtils.convertToMillis(interval, timeUnit);
        TaskDefinition definition = createTaskDefinition(task, "-FixedRate");
        String taskId = registerTask(task, definition);
        // 创建固定频率触发器
        taskTriggerService.createFixedRateTrigger(taskId, -1, millisInterval, null, null, null, null);
        return taskId;
    }

    /**
     * 注册Runnable类型任务
     *
     * @param runnable   Runnable任务实例
     * @param definition 任务定义
     * @return 注册成功的任务ID
     */
    @Override
    @Transactional
    public String registerTask(Runnable runnable, TaskDefinition definition) {
        validateRunnable(runnable, definition);

        // 设置Runnable实例和方法
        if (definition.getBean() == null) {
            TaskDefinition newDefinition = taskAdapter.createTaskDefinition(runnable);
            definition.setBean(newDefinition.getBean());
            definition.setMethod(newDefinition.getMethod());
            // 复制beanName和methodName
            if (definition.getBeanName() == null) {
                definition.setBeanName(newDefinition.getBeanName());
            }
            if (definition.getMethodName() == null) {
                definition.setMethodName(newDefinition.getMethodName());
            }
        }

        // 注册任务核心逻辑
        return registerTaskCore(definition, false);
    }

    /**
     * 使用Cron表达式注册Runnable类型任务
     *
     * @param runnable Runnable任务实例
     * @param cron     Cron表达式
     * @return 注册成功的任务ID
     */
    @Override
    public String registerTask(Runnable runnable, String cron) {
        validateCron(cron);

        TaskDefinition definition = createTaskDefinition(runnable, "-Runnable-Cron");
        String taskId = registerTask(runnable, definition);
        // 创建Cron触发器
        taskTriggerService.createCronTrigger(taskId, cron, null, null, null, null, null);
        return taskId;
    }

    /**
     * 使用固定频率注册Runnable类型任务
     *
     * @param runnable Runnable任务实例
     * @param interval 时间间隔
     * @param timeUnit 时间单位
     * @return 注册成功的任务ID
     */
    @Override
    public String registerTask(Runnable runnable, long interval, TimeUnit timeUnit) {
        validateInterval(interval, timeUnit);

        long millisInterval = TimeUtils.convertToMillis(interval, timeUnit);
        TaskDefinition definition = createTaskDefinition(runnable, "-Runnable-FixedRate");
        String taskId = registerTask(runnable, definition);
        // 创建固定频率触发器
        taskTriggerService.createFixedRateTrigger(taskId, -1, millisInterval, null, null, null, null);
        return taskId;
    }

    /**
     * 注册Callable类型任务
     *
     * @param callable   Callable任务实例
     * @param definition 任务定义
     * @param <V>        任务返回值类型
     * @return 注册成功的任务ID
     */
    @Override
    @Transactional
    public <V> String registerTask(Callable<V> callable, TaskDefinition definition) {
        validateCallable(callable, definition);

        // 设置Callable实例和方法
        if (definition.getBean() == null) {
            TaskDefinition newDefinition = taskAdapter.createTaskDefinition(callable);
            definition.setBean(newDefinition.getBean());
            definition.setMethod(newDefinition.getMethod());
            // 复制beanName和methodName
            if (definition.getBeanName() == null) {
                definition.setBeanName(newDefinition.getBeanName());
            }
            if (definition.getMethodName() == null) {
                definition.setMethodName(newDefinition.getMethodName());
            }
        }

        // 注册任务核心逻辑
        return registerTaskCore(definition, false);
    }

    /**
     * 使用Cron表达式注册Callable类型任务
     *
     * @param callable Callable任务实例
     * @param cron     Cron表达式
     * @param <V>      任务返回值类型
     * @return 注册成功的任务ID
     */
    @Override
    public <V> String registerTask(Callable<V> callable, String cron) {
        validateCron(cron);

        TaskDefinition definition = createTaskDefinition(callable, "-Callable-Cron");
        String taskId = registerTask(callable, definition);
        // 创建Cron触发器
        taskTriggerService.createCronTrigger(taskId, cron, null, null, null, null, null);
        return taskId;
    }

    /**
     * 使用固定频率注册Callable类型任务
     *
     * @param callable Callable任务实例
     * @param interval 时间间隔
     * @param timeUnit 时间单位
     * @param <V>      任务返回值类型
     * @return 注册成功的任务ID
     */
    @Override
    public <V> String registerTask(Callable<V> callable, long interval, TimeUnit timeUnit) {
        validateInterval(interval, timeUnit);

        long millisInterval = TimeUtils.convertToMillis(interval, timeUnit);
        TaskDefinition definition = createTaskDefinition(callable, "-Callable-FixedRate");
        String taskId = registerTask(callable, definition);
        // 创建固定频率触发器
        taskTriggerService.createFixedRateTrigger(taskId, -1, millisInterval, null, null, null, null);
        return taskId;
    }

    /**
     * 立即触发任务执行
     *
     * @param taskId 任务ID
     */
    @Override
    public void triggerTask(String taskId) {
        // 参数验证
        validateTaskId(taskId);

        // 获取任务定义
        TaskDefinition definition = getTaskDefinitionInternal(taskId);

        // 立即触发任务执行
        scheduler.triggerNow(definition);
        log.info("成功触发任务执行: {} (ID: {})", definition.getName(), taskId);
    }

    /**
     * 暂停任务
     * <p>
     * 取消任务调度，更新任务状态为禁用，并持久化更新
     * </p>
     *
     * @param taskId 任务ID
     */
    @Override
    @Transactional
    public void pauseTask(String taskId) {
        // 参数验证
        validateTaskId(taskId);

        // 获取任务定义
        TaskDefinition definition = getTaskDefinitionInternal(taskId);

        // 取消任务调度
        scheduler.unschedule(taskId);

        // 更新任务状态
        definition.setEnabled(false);
        taskRegistry.register(definition);

        // 持久化更新
        if (definition.isPersistent()) {
            persistenceService.update(definition);
        }

        log.info("成功暂停任务: {} (ID: {})", definition.getName(), taskId);
    }

    /**
     * 恢复任务
     * <p>
     * 更新任务状态为启用，持久化更新，并重新调度任务
     * </p>
     *
     * @param taskId 任务ID
     */
    @Override
    @Transactional
    public void resumeTask(String taskId) {
        // 参数验证
        validateTaskId(taskId);

        // 获取任务定义
        TaskDefinition definition = getTaskDefinitionInternal(taskId);

        // 更新任务状态
        definition.setEnabled(true);
        taskRegistry.register(definition);

        // 持久化更新
        if (definition.isPersistent()) {
            persistenceService.update(definition);
        }

        // 重新调度任务
        scheduler.schedule(definition);

        log.info("成功恢复任务: {} (ID: {})", definition.getName(), taskId);
    }

    /**
     * 取消任务
     * <p>
     * 取消任务调度，从注册表中移除，并从数据库中删除（如果持久化）
     * </p>
     *
     * @param taskId 任务ID
     */
    @Override
    @Transactional
    public void cancelTask(String taskId) {
        // 参数验证
        validateTaskId(taskId);

        // 获取任务定义
        TaskDefinition definition = getTaskDefinitionInternal(taskId);

        // 取消任务调度
        scheduler.unschedule(taskId);

        // 从注册表中移除
        taskRegistry.unregister(taskId);

        // 从数据库中删除
        if (definition.isPersistent()) {
            persistenceService.delete(taskId);
        }

        log.info("成功取消任务: {} (ID: {})", definition.getName(), taskId);
    }

    /**
     * 获取任务定义
     *
     * @param taskId 任务ID
     * @return 任务定义对象
     */
    @Override
    public TaskDefinition getTaskDefinition(String taskId) {
        validateTaskId(taskId);
        return taskRegistry.get(taskId);
    }

    /**
     * 注册任务核心逻辑，提取重复代码
     * <p>
     * 处理任务注册的通用逻辑，包括ID生成、默认值设置、
     * 注册表注册、持久化和调度等操作
     * </p>
     *
     * @param definition 任务定义
     * @param isTaskType 是否为Task类型任务
     * @return 注册成功的任务ID
     */
    private String registerTaskCore(TaskDefinition definition, boolean isTaskType) {
        // 如果任务ID为空，生成一个新的UUID
        if (definition.getId() == null) {
            definition.setId(UUID.randomUUID().toString());
        }

        // 设置默认值
        setTaskDefaultValues(definition, isTaskType);

        // 注册任务到注册表
        taskRegistry.register(definition);

        // 持久化任务到数据库
        persistTask(definition);

        // 记录日志
        String taskType = isTaskType ? "任务" : (definition.getBean() instanceof Callable ? "Callable任务" : "Runnable任务");
        log.info("成功注册{}: {} (ID: {})", taskType, definition.getName(), definition.getId());

        return definition.getId();
    }

    /**
     * 创建TaskDefinition实例
     *
     * @param task   任务对象
     * @param suffix 任务名称后缀
     * @return TaskDefinition实例
     */
    private TaskDefinition createTaskDefinition(Object task, String suffix) {
        TaskDefinition definition = new TaskDefinition();
        definition.setName(task.getClass().getSimpleName() + suffix);
        return definition;
    }

    /**
     * 设置任务默认值
     * <p>
     * 设置任务的默认分组和持久化属性
     * </p>
     *
     * @param definition 任务定义
     * @param isTaskType 是否为Task类型任务
     */
    private void setTaskDefaultValues(TaskDefinition definition, boolean isTaskType) {
        // 设置默认分组
        if (definition.getGroupName() == null) {
            definition.setGroupName("default");
        }

        // 根据配置自动设置持久化属性
        String persistenceType = persistenceProperties.getType();
        boolean isDatabase = "database".equalsIgnoreCase(persistenceType);

        if (isTaskType) {
            // 对于Task类型，尊重传入的persistent值
            if (!definition.isPersistent() && isDatabase) {
                definition.setPersistent(true);
            }
        } else {
            // 对于Runnable和Callable类型，直接根据配置设置
            if (isDatabase) {
                definition.setPersistent(true);
            }
        }

        log.info("任务持久化配置: taskName={}, persistence.type={}, isDatabase={}, persistent={}",
                definition.getName(), persistenceType, isDatabase, definition.isPersistent());
    }

    /**
     * 持久化任务到数据库
     * <p>
     * 如果任务配置为持久化，则将任务保存到数据库
     * </p>
     *
     * @param definition 任务定义
     */
    private void persistTask(TaskDefinition definition) {
        if (definition.isPersistent()) {
            log.info("开始持久化任务到数据库: taskId={}, taskName={}", definition.getId(), definition.getName());
            persistenceService.save(definition);
            log.info("完成持久化任务到数据库: taskId={}, taskName={}", definition.getId(), definition.getName());
        } else {
            log.info("跳过任务持久化: taskName={}, persistent={}", definition.getName(), definition.isPersistent());
        }
    }

    /**
     * 调度任务
     * <p>
     * 如果任务已启用，则将任务提交给调度器进行调度
     * </p>
     *
     * @param definition 任务定义
     */
    private void scheduleTask(TaskDefinition definition) {
        if (definition.isEnabled()) {
            scheduler.schedule(definition);
        }
    }

    /**
     * 参数验证：Task类型
     *
     * @param task       Task任务实例
     * @param definition 任务定义
     * @throws IllegalArgumentException 如果参数无效
     */
    private void validateTask(Task task, TaskDefinition definition) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        validateDefinition(definition);
    }

    /**
     * 参数验证：Runnable类型
     *
     * @param runnable   Runnable任务实例
     * @param definition 任务定义
     * @throws IllegalArgumentException 如果参数无效
     */
    private void validateRunnable(Runnable runnable, TaskDefinition definition) {
        if (runnable == null) {
            throw new IllegalArgumentException("Runnable cannot be null");
        }
        validateDefinition(definition);
    }

    /**
     * 参数验证：Callable类型
     *
     * @param callable   Callable任务实例
     * @param definition 任务定义
     * @throws IllegalArgumentException 如果参数无效
     */
    private void validateCallable(Callable<?> callable, TaskDefinition definition) {
        if (callable == null) {
            throw new IllegalArgumentException("Callable cannot be null");
        }
        validateDefinition(definition);
    }

    /**
     * 参数验证：TaskDefinition类型
     *
     * @param definition 任务定义
     * @throws IllegalArgumentException 如果参数无效
     */
    private void validateDefinition(TaskDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("TaskDefinition cannot be null");
        }
    }

    /**
     * 参数验证：Cron表达式
     *
     * @param cron Cron表达式
     * @throws IllegalArgumentException 如果参数无效
     */
    private void validateCron(String cron) {
        if (cron == null || cron.trim().isEmpty()) {
            throw new IllegalArgumentException("Cron expression cannot be empty");
        }
    }

    /**
     * 参数验证：时间间隔
     *
     * @param interval 时间间隔
     * @param timeUnit 时间单位
     * @throws IllegalArgumentException 如果参数无效
     */
    private void validateInterval(long interval, TimeUnit timeUnit) {
        if (interval <= 0) {
            throw new IllegalArgumentException("Interval must be greater than 0");
        }
        if (timeUnit == null) {
            throw new IllegalArgumentException("TimeUnit cannot be null");
        }
    }

    /**
     * 参数验证：任务ID
     *
     * @param taskId 任务ID
     * @throws IllegalArgumentException 如果参数无效
     */
    private void validateTaskId(String taskId) {
        if (taskId == null || taskId.isEmpty()) {
            throw new IllegalArgumentException("Task ID cannot be empty");
        }
    }

    /**
     * 获取任务定义（内部使用，包含空值检查）
     * <p>
     * 从注册表获取任务定义，如果任务不存在则抛出异常
     * </p>
     *
     * @param taskId 任务ID
     * @return 任务定义
     * @throws IllegalArgumentException 如果任务不存在
     */
    private TaskDefinition getTaskDefinitionInternal(String taskId) {
        TaskDefinition definition = taskRegistry.get(taskId);
        if (definition == null) {
            throw new IllegalArgumentException("Task not found with ID: " + taskId);
        }
        return definition;
    }

}
