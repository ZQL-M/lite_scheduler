package com.tuba.schedulercore.core.scheduler.factory;

import com.tuba.schedulercore.core.scheduler.Scheduler;

import java.util.List;

/**
 * 调度器工厂接口，用于创建和管理调度器
 */
public interface SchedulerFactory {
    /**
     * 创建默认调度器
     */
    Scheduler createScheduler();
    
    /**
     * 根据名称创建调度器
     */
    Scheduler createScheduler(String schedulerName);
    
    /**
     * 获取所有调度器
     */
    List<Scheduler> getAllSchedulers();
    
    /**
     * 关闭所有调度器
     */
    void shutdownAllSchedulers();
}
