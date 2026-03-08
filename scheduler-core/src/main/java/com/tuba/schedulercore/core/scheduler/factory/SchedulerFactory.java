package com.tuba.schedulercore.core.scheduler.factory;

import com.tuba.schedulercore.core.scheduler.Scheduler;

import java.util.List;

/**
 * 调度器工厂接口
 * 用于创建和管理调度器实例
 *
 */
public interface SchedulerFactory {

    /**
     * 获取默认调度器
     * 
     * @return 默认调度器
     */
    Scheduler getDefaultScheduler();

    /**
     * 获取指定名称的调度器
     * 
     * @param schedulerName 调度器名称
     * @return 调度器实例
     */
    Scheduler getScheduler(String schedulerName);

    /**
     * 获取所有调度器
     * 
     * @return 调度器列表
     */
    List<Scheduler> getAllSchedulers();

    /**
     * 关闭所有调度器
     */
    void shutdownAll();
}
