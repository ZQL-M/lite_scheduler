package com.tuba.schedulercore.core.scheduler.factory;

import com.tuba.schedulercore.core.scheduler.Scheduler;
import com.tuba.schedulercore.core.scheduler.ThreadPoolScheduler;
import com.tuba.schedulercore.core.store.JobStore;
import com.tuba.schedulercore.core.executor.TaskExecutor;
import com.tuba.schedulercore.config.properties.ExecutorProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 标准调度器工厂实现
 * 参考 Quartz 的 StdSchedulerFactory
 *
 */
@Component
public class StdSchedulerFactory implements SchedulerFactory {

    private static final Logger log = LoggerFactory.getLogger(StdSchedulerFactory.class);

    private static final String DEFAULT_SCHEDULER_NAME = "default";

    private final Map<String, Scheduler> schedulerMap = new ConcurrentHashMap<>();

    @Autowired
    private JobStore jobStore;

    @Autowired
    private TaskExecutor taskExecutor;

    @Autowired(required = false)
    private ExecutorProperties executorProperties;

    @PostConstruct
    public void initialize() {
        // 创建默认调度器
        Scheduler defaultScheduler = createScheduler(DEFAULT_SCHEDULER_NAME);
        schedulerMap.put(DEFAULT_SCHEDULER_NAME, defaultScheduler);
        log.info("SchedulerFactory initialized with default scheduler");
    }

    @Override
    public Scheduler getDefaultScheduler() {
        return getScheduler(DEFAULT_SCHEDULER_NAME);
    }

    @Override
    public Scheduler getScheduler(String schedulerName) {
        if (schedulerName == null || schedulerName.isEmpty()) {
            schedulerName = DEFAULT_SCHEDULER_NAME;
        }

        return schedulerMap.computeIfAbsent(schedulerName, this::createScheduler);
    }

    @Override
    public List<Scheduler> getAllSchedulers() {
        return new ArrayList<>(schedulerMap.values());
    }

    @Override
    public void shutdownAll() {
        log.info("Shutting down all schedulers...");

        for (Scheduler scheduler : schedulerMap.values()) {
            try {
                scheduler.shutdown();
            } catch (Exception e) {
                log.error("Error shutting down scheduler: {}", scheduler, e);
            }
        }

        schedulerMap.clear();
        log.info("All schedulers shut down");
    }

    /**
     * 创建调度器实例
     * 
     * @param schedulerName 调度器名称
     * @return 调度器实例
     */
    protected Scheduler createScheduler(String schedulerName) {
        log.info("Creating scheduler: {}", schedulerName);

        ThreadPoolScheduler scheduler = new ThreadPoolScheduler(
                taskExecutor,
                executorProperties,
                jobStore);

        log.info("Scheduler created: {}", schedulerName);
        return scheduler;
    }
}
