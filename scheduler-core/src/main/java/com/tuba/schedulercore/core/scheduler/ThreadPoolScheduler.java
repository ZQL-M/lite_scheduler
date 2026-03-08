package com.tuba.schedulercore.core.scheduler;

import com.tuba.schedulercore.config.properties.ExecutorProperties;
import com.tuba.schedulercore.core.executor.TaskExecutor;
import com.tuba.schedulercore.core.store.JobStore;
import com.tuba.schedulercore.enums.SchedulerStatus;
import com.tuba.schedulercore.core.trigger.Trigger;
import com.tuba.schedulercore.model.TaskContext;
import com.tuba.schedulercore.model.TaskDefinition;
import com.tuba.schedulercore.model.TaskStatus;
import com.tuba.schedulercore.model.TaskTrigger;
import com.tuba.schedulercore.model.SimpleTrigger;
import com.tuba.schedulercore.model.CronTrigger;
import com.tuba.schedulercore.core.task.TubaTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
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

    private final ThreadPoolTaskScheduler taskScheduler;
    private final TaskExecutor taskExecutor;
    private final JobStore jobStore;
    // 保存 ScheduledFuture 以便取消任务
    private final Map<String, ScheduledFuture<?>> scheduledFutures = new ConcurrentHashMap<>();
    // 保存任务执行次数，使用 AtomicInteger 保证线程安全
    private final Map<String, AtomicInteger> taskExecutionCounts = new ConcurrentHashMap<>();
    // 调度器状态
    private SchedulerStatus status = SchedulerStatus.STOPPED;

    public ThreadPoolScheduler(TaskExecutor taskExecutor, ExecutorProperties properties, JobStore jobStore) {
        this.taskExecutor = taskExecutor;
        this.jobStore = jobStore;
        this.taskScheduler = new ThreadPoolTaskScheduler();

        // 使用 Optional 简化 null 检查
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

        this.status = SchedulerStatus.STARTED;
    }

    @Override
    public String scheduleTask(TubaTask task, TaskDefinition definition, Trigger trigger) {
        if (task == null || definition == null) {
            log.warn("Cannot schedule null task or definition.");
            return null;
        }

        // 存储任务定义和触发器到 JobStore
        jobStore.storeTaskDefinition(definition, true);

        // 将 Trigger 转换为 TaskTrigger 并存储
        TaskTrigger taskTrigger = convertToTaskTrigger(trigger, definition.getId());
        if (taskTrigger != null) {
            jobStore.storeTrigger(taskTrigger, true);
        }

        // 初始化任务执行次数
        taskExecutionCounts.putIfAbsent(definition.getId(), new AtomicInteger(0));

        // 创建任务执行逻辑
        Runnable taskRunnable = () -> {
            try {
                TaskContext context = new TaskContext(definition);
                task.execute(context);
            } catch (Exception e) {
                log.error("Error executing task {}", definition.getId(), e);
            }
        };

        // 调度任务
        scheduleTaskRunnable(definition.getId(), taskRunnable, trigger);

        log.info("Scheduled task {} with name {}", definition.getId(), definition.getName());
        return definition.getId();
    }

    @Override
    public String scheduleTask(Runnable task, TaskDefinition definition, Trigger trigger) {
        if (task == null || definition == null) {
            log.warn("Cannot schedule null task or definition.");
            return null;
        }

        // 存储任务定义和触发器到 JobStore
        jobStore.storeTaskDefinition(definition, true);

        TaskTrigger taskTrigger = convertToTaskTrigger(trigger, definition.getId());
        if (taskTrigger != null) {
            jobStore.storeTrigger(taskTrigger, true);
        }

        // 初始化任务执行次数
        taskExecutionCounts.putIfAbsent(definition.getId(), new AtomicInteger(0));

        // 调度任务
        scheduleTaskRunnable(definition.getId(), task, trigger);

        log.info("Scheduled task {} with name {}", definition.getId(), definition.getName());
        return definition.getId();
    }

    @Override
    public <V> String scheduleTask(Callable<V> task, TaskDefinition definition, Trigger trigger) {
        if (task == null || definition == null) {
            log.warn("Cannot schedule null task or definition.");
            return null;
        }

        // 存储任务定义和触发器到 JobStore
        jobStore.storeTaskDefinition(definition, true);

        TaskTrigger taskTrigger = convertToTaskTrigger(trigger, definition.getId());
        if (taskTrigger != null) {
            jobStore.storeTrigger(taskTrigger, true);
        }

        // 初始化任务执行次数
        taskExecutionCounts.putIfAbsent(definition.getId(), new AtomicInteger(0));

        // 包装 Callable 为 Runnable
        Runnable taskRunnable = () -> {
            try {
                task.call();
            } catch (Exception e) {
                log.error("Error executing callable task {}", definition.getId(), e);
            }
        };

        // 调度任务
        scheduleTaskRunnable(definition.getId(), taskRunnable, trigger);

        log.info("Scheduled task {} with name {}", definition.getId(), definition.getName());
        return definition.getId();
    }

    @Override
    public String scheduleTask(TubaTask task, String cronExpression) {
        if (task == null || cronExpression == null) {
            log.warn("Cannot schedule null task or cron expression.");
            return null;
        }

        TaskDefinition definition = new TaskDefinition();
        definition.setId(java.util.UUID.randomUUID().toString());
        definition.setName(task.getClass().getSimpleName());
        definition.setJobClass(task.getClass().getName());
        definition.setEnabled(true);

        Trigger trigger = new com.tuba.schedulercore.core.trigger.impl.CronTriggerImpl(
                java.util.UUID.randomUUID().toString(),
                cronExpression);

        return scheduleTask(task, definition, trigger);
    }

    @Override
    public String scheduleTask(TubaTask task, long interval, TimeUnit timeUnit) {
        if (task == null) {
            log.warn("Cannot schedule null task.");
            return null;
        }

        TaskDefinition definition = new TaskDefinition();
        definition.setId(java.util.UUID.randomUUID().toString());
        definition.setName(task.getClass().getSimpleName());
        definition.setJobClass(task.getClass().getName());
        definition.setEnabled(true);

        Trigger trigger = new com.tuba.schedulercore.core.trigger.impl.SimpleTriggerImpl(
                java.util.UUID.randomUUID().toString(),
                timeUnit.toMillis(interval),
                0,
                "FIXED_RATE");

        return scheduleTask(task, definition, trigger);
    }

    /**
     * 将 Trigger 转换为 TaskTrigger
     */
    private TaskTrigger convertToTaskTrigger(Trigger trigger, String taskId) {
        if (trigger == null) {
            return null;
        }

        TaskTrigger taskTrigger = new TaskTrigger();
        taskTrigger.setId(java.util.UUID.randomUUID().toString());
        taskTrigger.setTaskId(taskId);
        taskTrigger.setEnabled(true);
        taskTrigger.setStartTime(LocalDateTime.now());

        if (trigger instanceof com.tuba.schedulercore.core.trigger.impl.CronTriggerImpl) {
            com.tuba.schedulercore.core.trigger.impl.CronTriggerImpl cronTrigger = (com.tuba.schedulercore.core.trigger.impl.CronTriggerImpl) trigger;
            taskTrigger.setTriggerType("CRON");

            CronTrigger cronTriggerEntity = new CronTrigger();
            cronTriggerEntity.setTriggerId(cronTrigger.getId());
            cronTriggerEntity.setCronExpression(cronTrigger.getCronExpression());
            jobStore.storeCronTrigger(cronTriggerEntity);

        } else if (trigger instanceof com.tuba.schedulercore.core.trigger.impl.SimpleTriggerImpl) {
            com.tuba.schedulercore.core.trigger.impl.SimpleTriggerImpl simpleTrigger = (com.tuba.schedulercore.core.trigger.impl.SimpleTriggerImpl) trigger;
            taskTrigger.setTriggerType(simpleTrigger.getSimpleType());

            SimpleTrigger simpleTriggerEntity = new SimpleTrigger();
            simpleTriggerEntity.setTriggerId(simpleTrigger.getId());
            simpleTriggerEntity.setRepeatInterval(simpleTrigger.getInterval());
            simpleTriggerEntity.setRepeatCount(simpleTrigger.getRepeatCount());
            simpleTriggerEntity.setSimpleType(simpleTrigger.getSimpleType());
            jobStore.storeSimpleTrigger(simpleTriggerEntity);
        }

        return taskTrigger;
    }

    /**
     * 调度任务执行
     */
    private void scheduleTaskRunnable(String taskId, Runnable taskRunnable, Trigger trigger) {
        ScheduledFuture<?> future = null;

        if (trigger instanceof com.tuba.schedulercore.core.trigger.impl.CronTriggerImpl) {
            com.tuba.schedulercore.core.trigger.impl.CronTriggerImpl cronTrigger = (com.tuba.schedulercore.core.trigger.impl.CronTriggerImpl) trigger;
            future = taskScheduler.schedule(
                    taskRunnable,
                    new org.springframework.scheduling.support.CronTrigger(cronTrigger.getCronExpression()));
            log.info("Scheduled task {} with cron expression: {}", taskId, cronTrigger.getCronExpression());

        } else if (trigger instanceof com.tuba.schedulercore.core.trigger.impl.SimpleTriggerImpl) {
            com.tuba.schedulercore.core.trigger.impl.SimpleTriggerImpl simpleTrigger = (com.tuba.schedulercore.core.trigger.impl.SimpleTriggerImpl) trigger;

            if ("FIXED_RATE".equals(simpleTrigger.getSimpleType())) {
                future = taskScheduler.scheduleAtFixedRate(
                        taskRunnable,
                        Instant.now(),
                        Duration.ofMillis(simpleTrigger.getInterval()));
            } else if ("FIXED_DELAY".equals(simpleTrigger.getSimpleType())) {
                future = taskScheduler.scheduleWithFixedDelay(
                        taskRunnable,
                        Instant.now(),
                        Duration.ofMillis(simpleTrigger.getInterval()));
            }
            log.info("Scheduled task {} with {} interval: {}ms",
                    taskId, simpleTrigger.getSimpleType(), simpleTrigger.getInterval());
        }

        if (future != null) {
            scheduledFutures.put(taskId, future);
        }
    }

    @Override
    public void start() {
        if (status != SchedulerStatus.STARTED) {
            taskScheduler.initialize();
            status = SchedulerStatus.STARTED;
            log.info("Scheduler started");
        }
    }

    @Override
    public void shutdown() {
        log.info("Shutting down ThreadPoolScheduler...");

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

        status = SchedulerStatus.STOPPED;
    }

    @Override
    public void triggerNow(String taskId) {
        if (taskId == null) {
            log.warn("Cannot trigger null taskId.");
            return;
        }
        TaskDefinition definition = jobStore.retrieveTaskDefinition(taskId);
        if (definition != null) {
            TaskContext ctx = new TaskContext(definition);
            taskExecutor.execute(ctx);
            log.info("Triggered task {} immediately", taskId);
        } else {
            log.warn("Task not found: {}", taskId);
        }
    }

    @Override
    public void pauseTask(String taskId) {
        ScheduledFuture<?> future = scheduledFutures.get(taskId);
        if (future != null) {
            future.cancel(false);
            log.info("Paused task {}", taskId);
        }
    }

    @Override
    public void resumeTask(String taskId) {
        log.info("Resumed task {}", taskId);
    }

    @Override
    public void cancelTask(String taskId) {
        ScheduledFuture<?> future = scheduledFutures.remove(taskId);
        if (future != null) {
            future.cancel(true);
        }
        taskExecutionCounts.remove(taskId);
        log.info("Cancelled task {}", taskId);
    }

    @Override
    public SchedulerStatus getStatus() {
        return status;
    }

    @Override
    public boolean isShutdown() {
        return status == SchedulerStatus.STOPPED || taskScheduler.getScheduledThreadPoolExecutor().isShutdown();
    }

    @Override
    public boolean isTaskExists(String taskId) {
        return scheduledFutures.containsKey(taskId) || jobStore.isTaskExists(taskId);
    }

    @Override
    public TaskDefinition getTaskDefinition(String taskId) {
        return jobStore.retrieveTaskDefinition(taskId);
    }

    @Override
    public TaskStatus getTaskStatus(String taskId) {
        return jobStore.retrieveTaskStatus(taskId);
    }

    @Override
    public Trigger getTrigger(String taskId) {
        TaskTrigger taskTrigger = jobStore.retrieveTrigger(taskId);
        if (taskTrigger != null) {
            CronTrigger cronTrigger = jobStore.retrieveCronTrigger(taskTrigger.getId());
            if (cronTrigger != null) {
                return new com.tuba.schedulercore.core.trigger.impl.CronTriggerImpl(
                        cronTrigger.getTriggerId(),
                        cronTrigger.getCronExpression());
            }
            SimpleTrigger simpleTrigger = jobStore.retrieveSimpleTrigger(taskTrigger.getId());
            if (simpleTrigger != null) {
                return new com.tuba.schedulercore.core.trigger.impl.SimpleTriggerImpl(
                        simpleTrigger.getTriggerId(),
                        simpleTrigger.getRepeatInterval(),
                        simpleTrigger.getRepeatCount(),
                        simpleTrigger.getSimpleType());
            }
        }
        return null;
    }

    @Override
    public List<TaskDefinition> getAllTasks() {
        List<String> taskIds = jobStore.retrieveTaskIds();
        List<TaskDefinition> tasks = new ArrayList<>();
        for (String taskId : taskIds) {
            TaskDefinition definition = jobStore.retrieveTaskDefinition(taskId);
            if (definition != null) {
                tasks.add(definition);
            }
        }
        return tasks;
    }

    @Override
    public void destroy() throws Exception {
        shutdown();
    }
}