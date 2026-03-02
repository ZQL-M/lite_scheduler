package com.tuba.schedulercore.scheduler;

import com.tuba.schedulercore.config.ExecutorProperties;
import com.tuba.schedulercore.executor.TaskExecutor;
import com.tuba.schedulercore.model.TaskContext;
import com.tuba.schedulercore.model.TaskDefinition;
import com.tuba.schedulercore.model.TaskTrigger;
import com.tuba.schedulercore.model.SimpleTrigger;
import com.tuba.schedulercore.service.TaskTriggerService;
import com.tuba.schedulercore.service.TaskStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 使用 ThreadPoolTaskScheduler 调度任务
 * 支持 Cron 表达式和数字秒（固定频率）两种调度模式
 */
@Component
public class ThreadPoolScheduler implements Scheduler, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(ThreadPoolScheduler.class);

    // 常量定义
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 30;
    private static final String DEFAULT_THREAD_NAME_PREFIX = "lite-scheduler-";
    private static final int INFINITE_REPEAT = -1;
    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();
    private static final long DEFAULT_DELAY_SECONDS = 1;

    private final ThreadPoolTaskScheduler taskScheduler;
    private final TaskExecutor taskExecutor;
    private final TaskTriggerService taskTriggerService;
    private final TaskStatusService taskStatusService;
    // 保存 ScheduledFuture 以便取消任务
    private final Map<String, ScheduledFuture<?>> scheduledFutures = new ConcurrentHashMap<>();
    // 保存任务执行次数，使用AtomicInteger保证线程安全
    private final Map<String, AtomicInteger> taskExecutionCounts = new ConcurrentHashMap<>();

    public ThreadPoolScheduler(TaskExecutor taskExecutor, ExecutorProperties properties,
            TaskTriggerService taskTriggerService, TaskStatusService taskStatusService) {
        this.taskExecutor = taskExecutor;
        this.taskTriggerService = taskTriggerService;
        this.taskStatusService = taskStatusService;
        this.taskScheduler = new ThreadPoolTaskScheduler();

        // 使用Optional简化null检查
        int poolSize = Optional.ofNullable(properties)
                .map(ExecutorProperties::getSchedulerPoolSize)
                .orElse(Math.max(2, Runtime.getRuntime().availableProcessors()));

        String threadNamePrefix = Optional.ofNullable(properties)
                .map(ExecutorProperties::getThreadNamePrefix)
                .orElse(DEFAULT_THREAD_NAME_PREFIX);

        this.taskScheduler.setPoolSize(poolSize);
        this.taskScheduler.setThreadNamePrefix(threadNamePrefix);
        // 设置拒绝策略：使用 CallerRunsPolicy 避免丢任务
        this.taskScheduler.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        this.taskScheduler.initialize();

        log.info("ThreadPoolScheduler initialized with pool size: {}, thread name prefix: {}",
                poolSize, threadNamePrefix);
    }

    @Override
    public void schedule(TaskDefinition definition) {
        // 参数验证
        if (definition == null) {
            log.warn("Cannot schedule null TaskDefinition.");
            return;
        }
        if (!definition.isEnabled()) {
            log.debug("Task {} is disabled, skipping schedule.", definition.getId());
            return;
        }

        // 初始化任务执行次数
        taskExecutionCounts.putIfAbsent(definition.getId(), new AtomicInteger(0));

        // 创建任务执行逻辑
        Runnable taskRunnable = createTaskRunnable(definition);

        // 从TaskTrigger表中获取调度配置
        List<TaskTrigger> triggers = taskTriggerService.findByTaskId(definition.getId());
        if (triggers == null || triggers.isEmpty()) {
            // 没有找到触发器，使用默认配置
            log.warn("No trigger found for task {}, using default configuration", definition.getId());
            long defaultInterval = 60000; // 默认1分钟
            ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(
                    taskRunnable, Instant.now(), Duration.ofMillis(defaultInterval));
            scheduledFutures.put(definition.getId(), future);
            log.info("Scheduled task {} with default interval: {}ms", definition.getId(), defaultInterval);
            return;
        }

        // 处理每个触发器
        for (TaskTrigger trigger : triggers) {
            if (!trigger.isEnabled()) {
                log.debug("Trigger {} for task {} is disabled, skipping", trigger.getId(), definition.getId());
                continue;
            }

            // 检查触发器是否在有效期内
            if (!isTriggerValid(trigger)) {
                log.debug("Trigger {} for task {} is not valid, skipping", trigger.getId(), definition.getId());
                continue;
            }

            // 根据触发器类型创建调度
            ScheduledFuture<?> future = null;
            if ("CRON".equals(trigger.getTriggerType())) {
                // 处理Cron触发器
                com.tuba.schedulercore.model.CronTrigger cronTrigger = taskTriggerService
                        .findCronTriggerByTriggerId(trigger.getId());
                if (cronTrigger != null) {
                    future = taskScheduler.schedule(
                            taskRunnable,
                            new org.springframework.scheduling.support.CronTrigger(cronTrigger.getCronExpression()));
                    log.info("Scheduled task {} with cron expression: {}", definition.getId(),
                            cronTrigger.getCronExpression());
                }
            } else if ("FIXED_RATE".equals(trigger.getTriggerType())
                    || "FIXED_DELAY".equals(trigger.getTriggerType())) {
                // 处理Simple触发器
                SimpleTrigger simpleTrigger = taskTriggerService.findSimpleTriggerByTriggerId(trigger.getId());
                if (simpleTrigger != null) {
                    if ("FIXED_RATE".equals(simpleTrigger.getSimpleType())) {
                        future = taskScheduler.scheduleAtFixedRate(
                                taskRunnable,
                                Instant.now(),
                                Duration.ofMillis(simpleTrigger.getRepeatInterval()));
                    } else if ("FIXED_DELAY".equals(simpleTrigger.getSimpleType())) {
                        future = taskScheduler.scheduleWithFixedDelay(
                                taskRunnable,
                                Instant.now(),
                                Duration.ofMillis(simpleTrigger.getRepeatInterval()));
                    }
                    log.info("Scheduled task {} with {} interval: {}ms",
                            definition.getId(), simpleTrigger.getSimpleType(), simpleTrigger.getRepeatInterval());
                }
            }

            if (future != null) {
                scheduledFutures.put(definition.getId(), future);
            }
        }
    }

    /**
     * 检查触发器是否在有效期内
     *
     * @param trigger 触发器
     * @return 是否有效
     */
    private boolean isTriggerValid(TaskTrigger trigger) {
        LocalDateTime now = LocalDateTime.now();
        if (trigger.getStartTime() != null && now.isBefore(trigger.getStartTime())) {
            return false;
        }
        if (trigger.getEndTime() != null && now.isAfter(trigger.getEndTime())) {
            return false;
        }
        return true;
    }

    /**
     * 创建任务执行的Runnable
     * 
     * @param definition 任务定义
     * @return Runnable实例
     */
    private Runnable createTaskRunnable(TaskDefinition definition) {
        return () -> {
            // 检查任务是否被取消，避免不必要的执行
            if (!scheduledFutures.containsKey(definition.getId())) {
                log.debug("Task {} has been unscheduled, skipping execution.", definition.getId());
                return;
            }

            // 确保同一个任务在同一时间只有一个实例在执行
            synchronized (this) {
                // 再次检查任务是否被取消（双重检查）
                if (!scheduledFutures.containsKey(definition.getId())) {
                    log.debug("Task {} has been unscheduled, skipping execution.", definition.getId());
                    return;
                }

                // 更新任务状态并执行
                executeTask(definition);
            }
        };
    }

    /**
     * 执行任务并更新任务状态
     * 
     * @param definition 任务定义
     */
    private void executeTask(TaskDefinition definition) {
        // 执行任务
        TaskContext ctx = new TaskContext(definition);
        taskExecutor.execute(ctx);

        // 更新执行次数，确保count不为null
        AtomicInteger count = taskExecutionCounts.computeIfAbsent(definition.getId(),
                k -> new AtomicInteger(0));
        int currentCount = count.incrementAndGet();

        // 更新任务状态到数据库
        try {
            // 获取任务状态
            com.tuba.schedulercore.model.TaskStatus taskStatus = taskStatusService.getStatus(definition.getId());
            if (taskStatus != null) {
                // 更新执行次数
                taskStatus.setTotalExecutions(taskStatus.getTotalExecutions() + 1);
                taskStatusService.updateStatus(taskStatus);
            }
        } catch (Exception e) {
            log.warn("Failed to update task status for task {}", definition.getId(), e);
        }

        log.debug("Task {} executed {} times", definition.getId(), currentCount);
    }

    /**
     * 取消任务调度
     * 
     * @param taskId 任务ID
     */
    @Override
    public void unschedule(String taskId) {
        ScheduledFuture<?> future = scheduledFutures.remove(taskId);
        if (future != null) {
            future.cancel(true); // 取消任务，true 表示中断正在执行的任务
        }
        // 清理执行次数记录
        taskExecutionCounts.remove(taskId);

        // 任务被取消，更新状态为CANCELLED
        // 注意：这里无法直接获取TaskDefinition对象，需要通过TaskPersistenceService查询
        // 但考虑到性能和复杂度，这里暂时不更新数据库中的状态
        // 状态更新主要在任务执行和扫描阶段进行
    }

    /**
     * 启动调度器
     * <p>
     * 注意：调度器在构造方法中已经初始化，此方法为空实现
     */
    @Override
    public void start() {
        // 调度器在构造方法中已经初始化，无需额外操作
    }

    /**
     * 停止调度器
     * <p>
     * 取消所有已调度的任务，并优雅关闭线程池
     */
    @Override
    public void stop() {
        log.info("Stopping ThreadPoolScheduler...");

        // 取消所有已调度的任务
        int cancelledCount = 0;
        for (ScheduledFuture<?> future : scheduledFutures.values()) {
            if (future != null && !future.isCancelled()) {
                future.cancel(true);
                cancelledCount++;
            }
        }
        log.info("Cancelled {} scheduled tasks", cancelledCount);

        // 清理任务记录
        scheduledFutures.clear();
        taskExecutionCounts.clear();

        // 优雅关闭线程池
        taskScheduler.shutdown();
        try {
            if (!taskScheduler.getScheduledThreadPoolExecutor().awaitTermination(
                    SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                log.warn("TaskScheduler did not terminate gracefully within {}s, forcing shutdown",
                        SHUTDOWN_TIMEOUT_SECONDS);
                taskScheduler.getScheduledThreadPoolExecutor().shutdownNow();
            } else {
                log.info("TaskScheduler terminated gracefully");
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for TaskScheduler termination", e);
            taskScheduler.getScheduledThreadPoolExecutor().shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 销毁调度器
     * <p>
     * 实现DisposableBean接口，在Spring容器关闭时自动调用
     * 
     * @throws Exception 销毁过程中发生的异常
     */
    @Override
    public void destroy() throws Exception {
        stop();
    }

    /**
     * 立即触发任务执行
     * <p>
     * 此方法不会将任务加入调度计划，仅立即执行一次
     * 
     * @param definition 任务定义
     */
    @Override
    public void triggerNow(TaskDefinition definition) {
        if (definition == null) {
            log.warn("Cannot trigger null TaskDefinition.");
            return;
        }
        // 立即触发任务执行，不加入调度计划
        TaskContext ctx = new TaskContext(definition);
        taskExecutor.execute(ctx);
    }
}