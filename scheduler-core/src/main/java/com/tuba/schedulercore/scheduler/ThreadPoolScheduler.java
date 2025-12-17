package com.tuba.schedulercore.scheduler;

import com.tuba.schedulercore.config.ExecutorProperties;
import com.tuba.schedulercore.enums.TaskStatus;
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
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
    // 保存 ScheduledFuture 以便取消任务
    private final Map<String, ScheduledFuture<?>> scheduledFutures = new ConcurrentHashMap<>();
    // 保存任务执行次数，使用AtomicInteger保证线程安全
    private final Map<String, AtomicInteger> taskExecutionCounts = new ConcurrentHashMap<>();

    public ThreadPoolScheduler(TaskExecutor taskExecutor, ExecutorProperties properties) {
        this.taskExecutor = taskExecutor;
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

        ScheduledFuture<?> future;

        // 1. 检查固定频率配置
        long fixedRate = definition.getFixedRate();
        if (fixedRate > 0) {
            future = scheduleFixedRateTask(definition, taskRunnable);
        }
        // 2. 检查固定延迟配置
        else if (definition.getFixedDelay() > 0) {
            future = scheduleFixedDelayTask(definition, taskRunnable);
        }
        // 3. 检查 Cron 表达式配置
        else if (StringUtils.hasText(definition.getCron())) {
            future = scheduleCronTask(definition, taskRunnable);
        }
        // 无有效配置
        else {
            log.error("Task {} has no valid scheduling configuration", definition.getId());
            return;
        }

        scheduledFutures.put(definition.getId(), future);
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
        // 更新任务的last_fire_time为当前时间
        Instant now = Instant.now();
        LocalDateTime lastFireTime = LocalDateTime.ofInstant(now, DEFAULT_ZONE);
        definition.setLastFireTime(lastFireTime);

        // 计算下次触发时间
        LocalDateTime nextFireTime = calculateNextFireTime(definition, lastFireTime);
        definition.setNextFireTime(nextFireTime);

        // 执行任务
        TaskContext ctx = new TaskContext(definition);
        taskExecutor.execute(ctx);

        // 更新执行次数，确保count不为null
        AtomicInteger count = taskExecutionCounts.computeIfAbsent(definition.getId(),
                k -> new AtomicInteger(0));
        int currentCount = count.incrementAndGet();

        // 检查是否达到最大执行次数
        int repeatCount = definition.getRepeatCount();
        if (repeatCount != INFINITE_REPEAT) {
            // 更新任务的剩余执行次数
            int remainingCount = Math.max(0, repeatCount - currentCount);
            definition.setRepeatCount(remainingCount);

            if (currentCount >= repeatCount) {
                // 任务已完成所有执行次数，更新状态为已完成
                definition.setStatus(TaskStatus.COMPLETED);
                log.info(
                        "Task {} has reached maximum execution count of {} (executed {} times). Unscheduling task.",
                        definition.getId(), repeatCount, currentCount);
                unschedule(definition.getId());
            }
        }
    }

    /**
     * 调度固定频率任务
     * 
     * @param definition 任务定义
     * @param runnable   任务执行逻辑
     * @return ScheduledFuture实例
     */
    private ScheduledFuture<?> scheduleFixedRateTask(TaskDefinition definition, Runnable runnable) {
        long fixedRate = definition.getFixedRate();
        log.debug("Scheduling task {} with fixed rate: {}ms", definition.getId(), fixedRate);

        // 检查是否为一次性任务
        if (definition.getRepeatCount() == 1) {
            return scheduleOneTimeTask(definition, runnable);
        }
        // 多次执行任务，使用固定频率
        else {
            return taskScheduler.scheduleAtFixedRate(runnable, Instant.now(),
                    Duration.ofMillis(fixedRate));
        }
    }

    /**
     * 调度固定延迟任务
     * 
     * @param definition 任务定义
     * @param runnable   任务执行逻辑
     * @return ScheduledFuture实例
     */
    private ScheduledFuture<?> scheduleFixedDelayTask(TaskDefinition definition, Runnable runnable) {
        long fixedDelay = definition.getFixedDelay();
        log.debug("Scheduling task {} with fixed delay: {}ms", definition.getId(), fixedDelay);

        // 检查是否为一次性任务
        if (definition.getRepeatCount() == 1) {
            return scheduleOneTimeTask(definition, runnable);
        }
        // 多次执行任务，使用固定延迟
        else {
            return taskScheduler.scheduleWithFixedDelay(runnable, Instant.now(),
                    Duration.ofMillis(fixedDelay));
        }
    }

    /**
     * 调度一次性任务
     * 
     * @param definition 任务定义
     * @param runnable   任务执行逻辑
     * @return ScheduledFuture实例
     */
    private ScheduledFuture<?> scheduleOneTimeTask(TaskDefinition definition, Runnable runnable) {
        LocalDateTime nextFireTime = definition.getNextFireTime();
        if (nextFireTime != null) {
            // 将LocalDateTime转换为Instant
            Instant startTime = nextFireTime.atZone(DEFAULT_ZONE).toInstant();
            log.debug("Task {} is one-time task, scheduling at: {}", definition.getId(), nextFireTime);
            return taskScheduler.schedule(runnable, startTime);
        } else {
            // 如果没有nextFireTime，使用立即执行
            return taskScheduler.schedule(runnable, Instant.now());
        }
    }

    /**
     * 调度Cron表达式任务
     * 
     * @param definition 任务定义
     * @param runnable   任务执行逻辑
     * @return ScheduledFuture实例
     */
    private ScheduledFuture<?> scheduleCronTask(TaskDefinition definition, Runnable runnable) {
        String cron = definition.getCron();
        log.debug("Scheduling task {} with cron: {}", definition.getId(), cron);

        Trigger trigger = createCronTrigger(cron);
        return taskScheduler.schedule(runnable, trigger);
    }

    /**
     * 创建CronTrigger实例
     * @param cron
     * @return
     */
     private Trigger createCronTrigger(String cron) {
        // 尝试解析为数字秒（固定频率）
        try {
            long seconds = Long.parseLong(cron.trim());
            if (seconds > 0) {
                // 使用固定频率调度（每 N 秒执行一次）
                PeriodicTrigger periodicTrigger = new PeriodicTrigger(seconds * 1000); // 转换为毫秒
                periodicTrigger.setFixedRate(true); // 固定频率，不是固定延迟
                return periodicTrigger;
            }
        } catch (NumberFormatException e) {
            // 不是有效数字，继续使用Cron表达式
        }

        // 使用Cron表达式
        return new CronTrigger(cron);
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

    /**
     * 计算任务的下次触发时间
     * 
     * @param definition   任务定义
     * @param lastFireTime 上次触发时间
     * @return 下次触发时间
     */
    private LocalDateTime calculateNextFireTime(TaskDefinition definition, LocalDateTime lastFireTime) {
        // 1. 固定频率
        long fixedRate = definition.getFixedRate();
        if (fixedRate > 0) {
            return lastFireTime.plus(Duration.ofMillis(fixedRate));
        }

        // 2. 固定延迟
        long fixedDelay = definition.getFixedDelay();
        if (fixedDelay > 0) {
            return lastFireTime.plus(Duration.ofMillis(fixedDelay));
        }

        // 3. Cron表达式
        String cron = definition.getCron();
        if (StringUtils.hasText(cron)) {
            try {
                // 尝试解析为数字秒（固定频率）
                long seconds = Long.parseLong(cron.trim());
                if (seconds > 0) {
                    // 数字秒，使用固定频率计算
                    return lastFireTime.plusSeconds(seconds);
                }
            } catch (NumberFormatException e) {
                // 不是数字，尝试作为完整cron表达式解析
            }

            try {
                // 使用Spring的CronExpression解析器计算下次触发时间
                org.springframework.scheduling.support.CronExpression cronExpression = org.springframework.scheduling.support.CronExpression
                        .parse(cron);
                return cronExpression.next(lastFireTime);
            } catch (IllegalArgumentException e) {
                log.error("Invalid cron expression: {}, using default delay of {}s", cron, DEFAULT_DELAY_SECONDS, e);
                return lastFireTime.plusSeconds(DEFAULT_DELAY_SECONDS);
            }
        }

        // 默认立即执行
        return lastFireTime;
    }
}