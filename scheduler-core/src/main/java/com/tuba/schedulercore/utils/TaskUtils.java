package com.tuba.schedulercore.utils;

import com.tuba.schedulercore.model.TaskDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 任务工具类，提供任务相关的公共方法
 */
public class TaskUtils {

    private static final Logger log = LoggerFactory.getLogger(TaskUtils.class);

    /**
     * 验证任务类是否存在并具有execute方法
     * 
     * @param task 任务定义
     * @return 是否验证成功
     */
    public static boolean validateTaskClass(TaskDefinition task) {
        try {
            // 检查jobClass是否存在
            if (task.getJobClass() == null || task.getJobClass().isEmpty()) {
                log.error("任务 {} (ID: {}) 的jobClass为空", task.getName(), task.getId());
                return false;
            }

            try {
                // 加载任务类
                Class<?> jobClass = Class.forName(task.getJobClass());

                // 查找execute方法
                try {
                    // 尝试查找带TaskContext参数的execute方法
                    jobClass.getDeclaredMethod("execute", com.tuba.schedulercore.model.TaskContext.class);
                } catch (NoSuchMethodException e) {
                    // 尝试查找无参数的execute方法
                    try {
                        jobClass.getDeclaredMethod("execute");
                    } catch (NoSuchMethodException e2) {
                        log.error("无法找到execute方法: {}", task.getJobClass());
                        return false;
                    }
                }

                return true;
            } catch (ClassNotFoundException e) {
                log.error("无法找到任务类: {}", task.getJobClass(), e);
                return false;
            }
        } catch (Exception e) {
            log.error("验证任务类失败: {}", e.getMessage(), e);
            return false;
        }
    }
}
