package com.tuba.schedulercore.service.impl;

import com.tuba.schedulercore.config.PersistenceProperties;
import com.tuba.schedulercore.enums.TimeUnit;
import com.tuba.schedulercore.model.TaskDefinition;
import com.tuba.schedulercore.persistence.TaskPersistenceService;
import com.tuba.schedulercore.registry.TaskRegistry;
import com.tuba.schedulercore.scheduler.Scheduler;
import com.tuba.schedulercore.service.TaskSchedulerService;
import com.tuba.schedulercore.task.Task;
import com.tuba.schedulercore.task.TaskAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * 任务调度服务实现
 */
@Service
public class TaskSchedulerServiceImpl implements TaskSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(TaskSchedulerServiceImpl.class);

    private final TaskAdapter taskAdapter;
    private final TaskRegistry taskRegistry;
    private final Scheduler scheduler;
    private final TaskPersistenceService persistenceService;
    private final PersistenceProperties persistenceProperties;

    @Autowired
    public TaskSchedulerServiceImpl(TaskAdapter taskAdapter,
            TaskRegistry taskRegistry,
            Scheduler scheduler,
            TaskPersistenceService persistenceService,
            PersistenceProperties persistenceProperties) {
        this.taskAdapter = taskAdapter;
        this.taskRegistry = taskRegistry;
        this.scheduler = scheduler;
        this.persistenceService = persistenceService;
        this.persistenceProperties = persistenceProperties;
        log.info("TaskSchedulerServiceImpl初始化: persistence.type={}, persistenceService.class={}",
                persistenceProperties.getType(),
                persistenceService.getClass().getName());
    }

    @Override
    @Transactional
    public String registerTask(Task task, TaskDefinition definition) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        if (definition == null) {
            throw new IllegalArgumentException("TaskDefinition cannot be null");
        }

        // 如果任务ID为空，生成一个新的UUID
        if (definition.getId() == null) {
            definition.setId(UUID.randomUUID().toString());
        }

        // 设置Task实例和方法
        if (definition.getBean() == null) {
            TaskDefinition newDefinition = taskAdapter.createTaskDefinition(task);
            definition.setBean(newDefinition.getBean());
            definition.setMethod(newDefinition.getMethod());
        }

        // 设置默认值
        if (definition.getGroup() == null) {
            definition.setGroup("default");
        }
        // 根据配置自动设置持久化属性，但尊重传入的persistent值
        String persistenceType = persistenceProperties.getType();
        boolean isDatabase = "database".equalsIgnoreCase(persistenceType);
        // 如果persistent为true，则尊重这个设置；否则根据配置自动设置
        if (!definition.isPersistent() && isDatabase) {
            definition.setPersistent(true);
        }
        log.info("任务持久化配置: taskName={}, persistence.type={}, isDatabase={}, persistent={}, repeatCount={}",
                definition.getName(), persistenceType, isDatabase, definition.isPersistent(),
                definition.getRepeatCount());
        // async是boolean类型，不需要空检查
        // enabled是boolean类型，不需要空检查
        // repeatCount是int类型，不需要空检查

        // 计算初始nextFireTime
        LocalDateTime nextFireTime = calculateInitialNextFireTime(definition);
        definition.setNextFireTime(nextFireTime);
        log.info("任务初始nextFireTime: taskName={}, nextFireTime={}", definition.getName(), nextFireTime);

        // 注册任务到注册表
        taskRegistry.register(definition);

        // 持久化任务到数据库
        if (definition.isPersistent()) {
            log.info("开始持久化任务到数据库: taskId={}, taskName={}", definition.getId(), definition.getName());
            persistenceService.save(definition);
            log.info("完成持久化任务到数据库: taskId={}, taskName={}", definition.getId(), definition.getName());
        } else {
            log.info("跳过任务持久化: taskName={}, persistent={}", definition.getName(), definition.isPersistent());
        }

        // 调度任务
        if (definition.isEnabled()) {
            scheduler.schedule(definition);
        }

        log.info("成功注册任务: {} (ID: {})", definition.getName(), definition.getId());
        return definition.getId();
    }

    @Override
    public String registerTask(Task task, String cron) {
        if (cron == null || cron.trim().isEmpty()) {
            throw new IllegalArgumentException("Cron expression cannot be empty");
        }

        TaskDefinition definition = new TaskDefinition();
        definition.setCron(cron);
        definition.setName(task.getClass().getSimpleName() + "-Cron");

        return registerTask(task, definition);
    }

    @Override
    public String registerTask(Task task, long interval, TimeUnit timeUnit) {
        if (interval <= 0) {
            throw new IllegalArgumentException("Interval must be greater than 0");
        }
        if (timeUnit == null) {
            throw new IllegalArgumentException("TimeUnit cannot be null");
        }

        // 转换为毫秒
        long millisInterval = convertToMillis(interval, timeUnit);

        TaskDefinition definition = new TaskDefinition();
        definition.setFixedRate(millisInterval);
        definition.setName(task.getClass().getSimpleName() + "-FixedRate");

        return registerTask(task, definition);
    }

    @Override
    @Transactional
    public String registerTask(Runnable runnable, TaskDefinition definition) {
        if (runnable == null) {
            throw new IllegalArgumentException("Runnable cannot be null");
        }
        if (definition == null) {
            throw new IllegalArgumentException("TaskDefinition cannot be null");
        }

        // 如果任务ID为空，生成一个新的UUID
        if (definition.getId() == null) {
            definition.setId(UUID.randomUUID().toString());
        }

        // 设置Runnable实例和方法
        if (definition.getBean() == null) {
            TaskDefinition newDefinition = taskAdapter.createTaskDefinition(runnable);
            definition.setBean(newDefinition.getBean());
            definition.setMethod(newDefinition.getMethod());
        }

        // 设置默认值
        if (definition.getGroup() == null) {
            definition.setGroup("default");
        }
        // 根据配置自动设置持久化属性
        if ("database".equalsIgnoreCase(persistenceProperties.getType())) {
            definition.setPersistent(true);
        }

        // 计算初始nextFireTime
        LocalDateTime nextFireTime = calculateInitialNextFireTime(definition);
        definition.setNextFireTime(nextFireTime);
        log.info("任务初始nextFireTime: taskName={}, nextFireTime={}", definition.getName(), nextFireTime);

        // 注册任务到注册表
        taskRegistry.register(definition);

        // 持久化任务到数据库
        if (definition.isPersistent()) {
            persistenceService.save(definition);
        }

        // 调度任务
        if (definition.isEnabled()) {
            scheduler.schedule(definition);
        }

        log.info("成功注册Runnable任务: {} (ID: {})", definition.getName(), definition.getId());
        return definition.getId();
    }

    @Override
    public String registerTask(Runnable runnable, String cron) {
        if (cron == null || cron.trim().isEmpty()) {
            throw new IllegalArgumentException("Cron expression cannot be empty");
        }

        TaskDefinition definition = new TaskDefinition();
        definition.setCron(cron);
        definition.setName(runnable.getClass().getSimpleName() + "-Runnable-Cron");

        return registerTask(runnable, definition);
    }

    @Override
    public String registerTask(Runnable runnable, long interval, TimeUnit timeUnit) {
        if (interval <= 0) {
            throw new IllegalArgumentException("Interval must be greater than 0");
        }
        if (timeUnit == null) {
            throw new IllegalArgumentException("TimeUnit cannot be null");
        }

        // 转换为毫秒
        long millisInterval = convertToMillis(interval, timeUnit);

        TaskDefinition definition = new TaskDefinition();
        definition.setFixedRate(millisInterval);
        definition.setName(runnable.getClass().getSimpleName() + "-Runnable-FixedRate");

        return registerTask(runnable, definition);
    }

    @Override
    @Transactional
    public <V> String registerTask(Callable<V> callable, TaskDefinition definition) {
        if (callable == null) {
            throw new IllegalArgumentException("Callable cannot be null");
        }
        if (definition == null) {
            throw new IllegalArgumentException("TaskDefinition cannot be null");
        }

        // 如果任务ID为空，生成一个新的UUID
        if (definition.getId() == null) {
            definition.setId(UUID.randomUUID().toString());
        }

        // 设置Callable实例和方法
        if (definition.getBean() == null) {
            TaskDefinition newDefinition = taskAdapter.createTaskDefinition(callable);
            definition.setBean(newDefinition.getBean());
            definition.setMethod(newDefinition.getMethod());
        }

        // 设置默认值
        if (definition.getGroup() == null) {
            definition.setGroup("default");
        }
        // 根据配置自动设置持久化属性
        if ("database".equalsIgnoreCase(persistenceProperties.getType())) {
            definition.setPersistent(true);
        }

        // 计算初始nextFireTime
        LocalDateTime nextFireTime = calculateInitialNextFireTime(definition);
        definition.setNextFireTime(nextFireTime);
        log.info("任务初始nextFireTime: taskName={}, nextFireTime={}", definition.getName(), nextFireTime);

        // 注册任务到注册表
        taskRegistry.register(definition);

        // 持久化任务到数据库
        if (definition.isPersistent()) {
            persistenceService.save(definition);
        }

        // 调度任务
        if (definition.isEnabled()) {
            scheduler.schedule(definition);
        }

        log.info("成功注册Callable任务: {} (ID: {})", definition.getName(), definition.getId());
        return definition.getId();
    }

    @Override
    public <V> String registerTask(Callable<V> callable, String cron) {
        if (cron == null || cron.trim().isEmpty()) {
            throw new IllegalArgumentException("Cron expression cannot be empty");
        }

        TaskDefinition definition = new TaskDefinition();
        definition.setCron(cron);
        definition.setName(callable.getClass().getSimpleName() + "-Callable-Cron");

        return registerTask(callable, definition);
    }

    @Override
    public <V> String registerTask(Callable<V> callable, long interval, TimeUnit timeUnit) {
        if (interval <= 0) {
            throw new IllegalArgumentException("Interval must be greater than 0");
        }
        if (timeUnit == null) {
            throw new IllegalArgumentException("TimeUnit cannot be null");
        }

        // 转换为毫秒
        long millisInterval = convertToMillis(interval, timeUnit);

        TaskDefinition definition = new TaskDefinition();
        definition.setFixedRate(millisInterval);
        definition.setName(callable.getClass().getSimpleName() + "-Callable-FixedRate");

        return registerTask(callable, definition);
    }

    @Override
    public void triggerTask(String taskId) {
        if (taskId == null || taskId.isEmpty()) {
            throw new IllegalArgumentException("Task ID cannot be empty");
        }

        TaskDefinition definition = taskRegistry.get(taskId);
        if (definition == null) {
            throw new IllegalArgumentException("Task not found with ID: " + taskId);
        }

        // 立即触发任务执行
        scheduler.triggerNow(definition);
        log.info("成功触发任务执行: {} (ID: {})", definition.getName(), taskId);
    }

    @Override
    @Transactional
    public void pauseTask(String taskId) {
        if (taskId == null || taskId.isEmpty()) {
            throw new IllegalArgumentException("Task ID cannot be empty");
        }

        TaskDefinition definition = taskRegistry.get(taskId);
        if (definition == null) {
            throw new IllegalArgumentException("Task not found with ID: " + taskId);
        }

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

    @Override
    @Transactional
    public void resumeTask(String taskId) {
        if (taskId == null || taskId.isEmpty()) {
            throw new IllegalArgumentException("Task ID cannot be empty");
        }

        TaskDefinition definition = taskRegistry.get(taskId);
        if (definition == null) {
            throw new IllegalArgumentException("Task not found with ID: " + taskId);
        }

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

    @Override
    @Transactional
    public void cancelTask(String taskId) {
        if (taskId == null || taskId.isEmpty()) {
            throw new IllegalArgumentException("Task ID cannot be empty");
        }

        TaskDefinition definition = taskRegistry.get(taskId);
        if (definition == null) {
            throw new IllegalArgumentException("Task not found with ID: " + taskId);
        }

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

    @Override
    public TaskDefinition getTaskDefinition(String taskId) {
        if (taskId == null || taskId.isEmpty()) {
            throw new IllegalArgumentException("Task ID cannot be empty");
        }

        return taskRegistry.get(taskId);
    }

    /**
     * 计算任务的初始下次触发时间
     * 
     * @param definition 任务定义
     * @return 下次触发时间
     */
    private LocalDateTime calculateInitialNextFireTime(TaskDefinition definition) {
        LocalDateTime now = LocalDateTime.now();

        // 1. 检查Cron表达式配置
        String cron = definition.getCron();
        if (cron != null && !cron.trim().isEmpty()) {
            try {
                // 使用Spring的CronExpression解析器计算下次触发时间
                org.springframework.scheduling.support.CronExpression cronExpression = org.springframework.scheduling.support.CronExpression
                        .parse(cron);
                return cronExpression.next(now);
            } catch (IllegalArgumentException e) {
                log.error("无效的Cron表达式: {}, 使用默认时间", cron, e);
                return now.plusSeconds(1); // 默认1秒后执行
            }
        }

        // 2. 检查固定频率配置
        long fixedRate = definition.getFixedRate();
        if (fixedRate > 0) {
            return now.plusNanos(fixedRate * 1000000); // 转换为纳秒
        }

        // 3. 检查固定延迟配置
        long fixedDelay = definition.getFixedDelay();
        if (fixedDelay > 0) {
            return now.plusNanos(fixedDelay * 1000000); // 转换为纳秒
        }

        // 默认立即执行
        return now;
    }

    /**
     * 将时间间隔转换为毫秒
     * 
     * @param interval 时间间隔
     * @param timeUnit 时间单位
     * @return 毫秒数
     */
    private long convertToMillis(long interval, TimeUnit timeUnit) {
        switch (timeUnit) {
            case SECONDS:
                return interval * 1000;
            case MINUTES:
                return interval * 1000 * 60;
            case HOURS:
                return interval * 1000 * 60 * 60;
            case DAYS:
                return interval * 1000 * 60 * 60 * 24;
            case MILLISECONDS:
            default:
                return interval;
        }
    }
}
