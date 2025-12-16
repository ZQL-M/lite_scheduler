package com.tuba.schedulercore.scheduler;

import com.tuba.schedulercore.config.ExecutorProperties;
import com.tuba.schedulercore.executor.TaskExecutor;
import com.tuba.schedulercore.model.TaskContext;
import com.tuba.schedulercore.model.TaskDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 使用 ThreadPoolTaskScheduler 调度任务
 * 支持 Cron 表达式和数字秒（固定频率）两种调度模式
 */
@Component
public class ThreadPoolScheduler implements Scheduler, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(ThreadPoolScheduler.class);
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 30;

    private final ThreadPoolTaskScheduler taskScheduler;
    private final TaskExecutor taskExecutor;
    // 保存 ScheduledFuture 以便取消任务
    private final Map<String, ScheduledFuture<?>> scheduledFutures = new ConcurrentHashMap<>();
    // 保存任务执行次数
    private final Map<String, Integer> taskExecutionCounts = new ConcurrentHashMap<>();

    @Autowired(required = false)
    public ThreadPoolScheduler(TaskExecutor taskExecutor, ExecutorProperties properties) {
        this.taskExecutor = taskExecutor;
        this.taskScheduler = new ThreadPoolTaskScheduler();

        int poolSize = properties != null ? properties.getSchedulerPoolSize()
                : Math.max(2, Runtime.getRuntime().availableProcessors());
        String threadNamePrefix = properties != null ? properties.getThreadNamePrefix() : "lite-scheduler-";

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
        if (definition == null) {
            log.warn("Cannot schedule null TaskDefinition");
            return;
        }
        if (!definition.isEnabled()) {
            log.debug("Task {} is disabled, skipping schedule", definition.getId());
            return;
        }

        // 初始化任务执行次数
        taskExecutionCounts.put(definition.getId(), 0);

        // create a runnable that will submit to executor
        Runnable runnable = () -> {
            // 确保同一个任务在同一时间只有一个实例在执行
            synchronized (this) {
                // 检查任务是否被取消
                if (!scheduledFutures.containsKey(definition.getId())) {
                    log.debug("Task {} has been unscheduled, skipping execution", definition.getId());
                    return;
                }

                // 更新任务的last_fire_time为当前时间
                Instant now = Instant.now();
                LocalDateTime lastFireTime = LocalDateTime.ofInstant(now, java.time.ZoneId.systemDefault());
                definition.setLastFireTime(lastFireTime);

                // 计算下次触发时间
                LocalDateTime nextFireTime = calculateNextFireTime(definition, lastFireTime);
                definition.setNextFireTime(nextFireTime);

                // 执行任务
                TaskContext ctx = new TaskContext(definition);
                taskExecutor.execute(ctx);

                // 更新执行次数
                int currentCount = taskExecutionCounts.computeIfPresent(definition.getId(), (key, count) -> count + 1);

                // 检查是否达到最大执行次数
                int repeatCount = definition.getRepeatCount();
                if (repeatCount > -1) { // -1表示无限循环
                    // 更新任务的repeat_count
                    int remainingCount = repeatCount - currentCount;
                    definition.setRepeatCount(Math.max(0, remainingCount));

                    if (currentCount >= repeatCount) {
                        log.info(
                                "Task {} has reached maximum execution count of {} (executed {} times). Unscheduling task.",
                                definition.getId(), repeatCount, currentCount);
                        unschedule(definition.getId());
                        taskExecutionCounts.remove(definition.getId());
                    }
                }
            }
        };

        ScheduledFuture<?> future;

        // 1. 检查固定频率配置
        long fixedRate = definition.getFixedRate();
        if (fixedRate > 0) {
            log.debug("Scheduling task {} with fixed rate: {}ms", definition.getId(), fixedRate);

            // 检查是否为一次性任务
            int repeatCount = definition.getRepeatCount();
            if (repeatCount == 1) {
                // 一次性任务，使用Cron表达式或Date方式实现延迟执行
                LocalDateTime nextFireTime = definition.getNextFireTime();
                if (nextFireTime != null) {
                    // 将LocalDateTime转换为Instant
                    Instant startTime = nextFireTime.atZone(java.time.ZoneId.systemDefault()).toInstant();
                    log.debug("Task {} is one-time task, scheduling at: {}", definition.getId(), nextFireTime);
                    future = taskScheduler.schedule(runnable, startTime);
                } else {
                    // 如果没有nextFireTime，使用立即执行
                    future = taskScheduler.schedule(runnable, Instant.now());
                }
            } else {
                // 多次执行任务，使用固定频率，立即开始
                future = taskScheduler.scheduleAtFixedRate(runnable, Instant.now(),
                        java.time.Duration.ofMillis(fixedRate));
            }

            scheduledFutures.put(definition.getId(), future);
            return;
        }

        // 2. 检查固定延迟配置
        long fixedDelay = definition.getFixedDelay();
        if (fixedDelay > 0) {
            log.debug("Scheduling task {} with fixed delay: {}ms", definition.getId(), fixedDelay);

            // 检查是否为一次性任务
            int repeatCount = definition.getRepeatCount();
            if (repeatCount == 1) {
                // 一次性任务，使用Cron表达式或Date方式实现延迟执行
                LocalDateTime nextFireTime = definition.getNextFireTime();
                if (nextFireTime != null) {
                    // 将LocalDateTime转换为Instant
                    Instant startTime = nextFireTime.atZone(java.time.ZoneId.systemDefault()).toInstant();
                    log.debug("Task {} is one-time task, scheduling at: {}", definition.getId(), nextFireTime);
                    future = taskScheduler.schedule(runnable, startTime);
                } else {
                    // 如果没有nextFireTime，使用立即执行
                    future = taskScheduler.schedule(runnable, Instant.now());
                }
            } else {
                // 多次执行任务，使用固定延迟，立即开始
                future = taskScheduler.scheduleWithFixedDelay(runnable, Instant.now(),
                        java.time.Duration.ofMillis(fixedDelay));
            }

            scheduledFutures.put(definition.getId(), future);
            return;
        }

        // 3. 检查 Cron 表达式配置
        String cron = definition.getCron();
        if (cron != null && !cron.isEmpty()) {
            log.debug("Scheduling task {} with cron: {}", definition.getId(), cron);

            Trigger trigger;
            // 尝试解析为数字秒（固定频率）
            try {
                long seconds = Long.parseLong(cron.trim());
                if (seconds > 0) {
                    // 使用固定频率调度（每 N 秒执行一次）
                    PeriodicTrigger periodicTrigger = new PeriodicTrigger(seconds * 1000); // 转换为毫秒
                    periodicTrigger.setFixedRate(true); // 固定频率，不是固定延迟
                    trigger = periodicTrigger;
                } else {
                    // 数字无效，使用 Cron 表达式
                    trigger = new CronTrigger(cron);
                }
            } catch (NumberFormatException e) {
                // 不是数字，使用 Cron 表达式
                trigger = new CronTrigger(cron);
            }

            future = taskScheduler.schedule(runnable, trigger);
            scheduledFutures.put(definition.getId(), future);
            return;
        }

        log.error("Task {} has no valid scheduling configuration", definition.getId());
    }

    @Override
    public void unschedule(String taskId) {
        ScheduledFuture<?> future = scheduledFutures.remove(taskId);
        if (future != null) {
            future.cancel(true); // 取消任务，true 表示中断正在执行的任务
        }
        // 清理执行次数记录
        taskExecutionCounts.remove(taskId);
    }

    @Override
    public void start() {
        // already initialized in constructor; nothing else needed
    }

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
        scheduledFutures.clear();

        // 清理所有执行次数记录
        taskExecutionCounts.clear();

        // 优雅关闭线程池
        taskScheduler.shutdown();
        try {
            if (!taskScheduler.getScheduledThreadPoolExecutor().awaitTermination(
                    SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                log.warn("TaskScheduler did not terminate gracefully, forcing shutdown");
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

    @Override
    public void destroy() throws Exception {
        stop();
    }

    @Override
    public void triggerNow(TaskDefinition definition) {
        if (definition == null) {
            log.warn("Cannot trigger null TaskDefinition");
            return;
        }
        // 立即触发任务执行，不加入调度计划
        TaskContext ctx = new TaskContext(definition);
        taskExecutor.execute(ctx);
    }

    /**
     * 计算任务的下次触发时间
     * 
     * @param definition   任务定义
     * @param lastFireTime 上次触发时间
     * @return 下次触发时间
     */
    private LocalDateTime calculateNextFireTime(TaskDefinition definition, LocalDateTime lastFireTime) {
        // 根据不同的调度类型计算下次触发时间

        // 1. 固定频率
        if (definition.getFixedRate() > 0) {
            return lastFireTime.plusNanos(definition.getFixedRate() * 1000000);
        }

        // 2. 固定延迟
        if (definition.getFixedDelay() > 0) {
            return lastFireTime.plusNanos(definition.getFixedDelay() * 1000000);
        }

        // 3. Cron表达式
        if (definition.getCron() != null && !definition.getCron().isEmpty()) {
            try {
                // 使用Spring的CronExpression解析器计算下次触发时间
                org.springframework.scheduling.support.CronExpression cronExpression = org.springframework.scheduling.support.CronExpression
                        .parse(definition.getCron());
                return cronExpression.next(lastFireTime);
            } catch (IllegalArgumentException e) {
                log.error("无效的Cron表达式: {}, 使用默认时间", definition.getCron(), e);
                return lastFireTime.plusSeconds(1); // 默认1秒后执行
            }
        }

        // 默认立即执行
        return lastFireTime;
    }
}