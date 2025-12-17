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
            if (task.getBeanName() == null || task.getBeanName().isEmpty()) {
                log.error("任务 {} (ID: {}) 的beanName为空", task.getName(), task.getId());
                return false;
            }

            if (task.getMethodName() == null || task.getMethodName().isEmpty()) {
                log.error("任务 {} (ID: {}) 的methodName为空", task.getName(), task.getId());
                return false;
            }

            // 从ApplicationContext获取bean实例，添加容错机制
            Object bean = null;
            String beanName = task.getBeanName();

            try {
                // 1. 首先尝试使用原始beanName
                bean = applicationContext.getBean(beanName);
            } catch (org.springframework.beans.factory.NoSuchBeanDefinitionException e1) {
                log.warn("使用原始beanName获取失败: {}, 尝试首字母小写", beanName);

                // 2. 尝试首字母小写（Spring默认命名规则）
                if (beanName.length() > 1) {
                    String lowercaseBeanName = Character.toLowerCase(beanName.charAt(0)) + beanName.substring(1);
                    try {
                        bean = applicationContext.getBean(lowercaseBeanName);
                        log.info("使用首字母小写beanName获取成功: {}", lowercaseBeanName);
                        // 更新任务的beanName，下次直接使用正确的名称
                        task.setBeanName(lowercaseBeanName);
                    } catch (org.springframework.beans.factory.NoSuchBeanDefinitionException e2) {
                        log.error("无法从ApplicationContext获取bean: {} 或 {}", beanName, lowercaseBeanName);
                        return false;
                    }
                } else {
                    log.error("无法从ApplicationContext获取bean: {}", beanName);
                    return false;
                }
            }

            if (bean == null) {
                log.error("无法从ApplicationContext获取bean: {}", beanName);
                return false;
            }

            // 解析method对象
            Method[] methods = bean.getClass().getDeclaredMethods();
            Method targetMethod = null;
            for (Method method : methods) {
                if (method.getName().equals(task.getMethodName())) {
                    // 检查方法签名是否匹配
                    if (method.getParameterCount() == 0 ||
                            (method.getParameterCount() == 1 &&
                                    com.tuba.schedulercore.model.TaskContext.class
                                            .isAssignableFrom(method.getParameterTypes()[0]))) {
                        targetMethod = method;
                        break;
                    }
                }
            }

            if (targetMethod == null) {
                // 尝试查找原始类的方法（处理代理情况）
                Class<?> targetClass = bean.getClass();
                while (targetClass != null && targetClass != Object.class) {
                    try {
                        Method[] declaredMethods = targetClass.getDeclaredMethods();
                        for (Method method : declaredMethods) {
                            if (method.getName().equals(task.getMethodName())) {
                                if (method.getParameterCount() == 0 ||
                                        (method.getParameterCount() == 1 &&
                                                com.tuba.schedulercore.model.TaskContext.class
                                                        .isAssignableFrom(method.getParameterTypes()[0]))) {
                                    targetMethod = method;
                                    break;
                                }
                            }
                        }
                        if (targetMethod != null) {
                            break;
                        }
                    } catch (Exception e) {
                        log.error("获取原始类方法失败: {}", e.getMessage(), e);
                    }
                    targetClass = targetClass.getSuperclass();
                }
            }

            if (targetMethod == null) {
                log.error("无法找到匹配的方法: {} 上的 {}", task.getBeanName(), task.getMethodName());
                return false;
            }

            // 设置bean和method
            task.setBean(bean);
            task.setMethod(targetMethod);
            return true;
        } catch (Exception e) {
            log.error("解析bean和method失败: {}", e.getMessage(), e);
            return false;
        }
    }
}
