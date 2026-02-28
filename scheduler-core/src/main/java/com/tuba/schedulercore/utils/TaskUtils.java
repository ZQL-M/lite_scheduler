package com.tuba.schedulercore.utils;

import com.tuba.schedulercore.model.TaskDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;

/**
 * 任务工具类，提供任务相关的公共方法
 */
public class TaskUtils {

    private static final Logger log = LoggerFactory.getLogger(TaskUtils.class);

    /**
     * 解析bean和method对象
     * 
     * @param applicationContext Spring应用上下文
     * @param task               任务定义
     * @return 是否解析成功
     */
    public static boolean parseBeanAndMethod(ApplicationContext applicationContext, TaskDefinition task) {
        try {
            // 检查是否已经有bean和method
            if (task.getBean() != null && task.getMethod() != null) {
                log.debug("任务 {} (ID: {}) 已经有bean和method，跳过解析", task.getName(), task.getId());
                return true;
            }

            // 检查jobClass是否存在
            if (task.getJobClass() == null || task.getJobClass().isEmpty()) {
                log.error("任务 {} (ID: {}) 的jobClass为空", task.getName(), task.getId());
                return false;
            }

            try {
                // 使用jobClass创建bean实例
                Class<?> jobClass = Class.forName(task.getJobClass());
                Object bean = jobClass.newInstance();

                // 查找execute方法
                Method targetMethod = null;
                try {
                    // 尝试查找带TaskContext参数的execute方法
                    targetMethod = jobClass.getDeclaredMethod("execute",
                            com.tuba.schedulercore.model.TaskContext.class);
                } catch (NoSuchMethodException e) {
                    // 尝试查找无参数的execute方法
                    try {
                        targetMethod = jobClass.getDeclaredMethod("execute");
                    } catch (NoSuchMethodException e2) {
                        log.error("无法找到execute方法: {}", task.getJobClass());
                        return false;
                    }
                }

                // 设置bean和method
                task.setBean(bean);
                task.setMethod(targetMethod);
                return true;
            } catch (ClassNotFoundException e) {
                log.error("无法找到任务类: {}", task.getJobClass(), e);
                return false;
            } catch (InstantiationException | IllegalAccessException e) {
                log.error("无法创建任务实例: {}", task.getJobClass(), e);
                return false;
            }
        } catch (Exception e) {
            log.error("解析bean和method失败: {}", e.getMessage(), e);
            return false;
        }
    }
}
