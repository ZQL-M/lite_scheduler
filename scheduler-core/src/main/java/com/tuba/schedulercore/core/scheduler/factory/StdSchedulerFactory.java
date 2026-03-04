package com.tuba.schedulercore.core.scheduler.factory;

import com.tuba.schedulercore.config.properties.ExecutorProperties;
import com.tuba.schedulercore.core.executor.TaskExecutor;
import com.tuba.schedulercore.core.scheduler.Scheduler;
import com.tuba.schedulercore.core.scheduler.ThreadPoolScheduler;
import com.tuba.schedulercore.service.TaskStatusService;
import com.tuba.schedulercore.service.TaskTriggerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 标准调度器工厂实现
 */
@Component
public class StdSchedulerFactory implements SchedulerFactory {
    private final TaskExecutor taskExecutor;
    private final ExecutorProperties executorProperties;
    private final TaskTriggerService taskTriggerService;
    private final TaskStatusService taskStatusService;
    private final Map<String, Scheduler> schedulers = new ConcurrentHashMap<>();
    
    @Autowired
    public StdSchedulerFactory(TaskExecutor taskExecutor, ExecutorProperties executorProperties,
                              TaskTriggerService taskTriggerService, TaskStatusService taskStatusService) {
        this.taskExecutor = taskExecutor;
        this.executorProperties = executorProperties;
        this.taskTriggerService = taskTriggerService;
        this.taskStatusService = taskStatusService;
    }
    
    @Override
    public Scheduler createScheduler() {
        return createScheduler("default-scheduler");
    }
    
    @Override
    public Scheduler createScheduler(String schedulerName) {
        Scheduler scheduler = new ThreadPoolScheduler(
                taskExecutor, 
                executorProperties, 
                taskTriggerService, 
                taskStatusService
        );
        schedulers.put(schedulerName, scheduler);
        return scheduler;
    }
    
    @Override
    public List<Scheduler> getAllSchedulers() {
        return new ArrayList<>(schedulers.values());
    }
    
    @Override
    public void shutdownAllSchedulers() {
        for (Scheduler scheduler : schedulers.values()) {
            scheduler.stop();
        }
        schedulers.clear();
    }
}
