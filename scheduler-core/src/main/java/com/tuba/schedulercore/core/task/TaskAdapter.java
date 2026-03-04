package com.tuba.schedulercore.core.task;

import com.tuba.schedulercore.model.TaskDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

/**
 * Task接口的适配器，将Task接口转换为框架可执行的方法调用
 */
@Component
public class TaskAdapter {

    private static final Logger log = LoggerFactory.getLogger(TaskAdapter.class);

    /**
     * 创建TaskDefinition
     * 
     * @param task Task实例
     * @return TaskDefinition
     */
    public TaskDefinition createTaskDefinition(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }

        TaskDefinition definition = new TaskDefinition();

        // 处理lambda表达式，获取原始类名
        String className = getOriginalClassName(task);
        definition.setName(className);
        definition.setDescription("Programmatically registered task");

        return definition;
    }

    /**
     * 获取对象的原始类名，处理lambda表达式
     * 
     * @param obj 对象
     * @return 原始类名
     */
    private String getOriginalClassName(Object obj) {
        if (obj == null) {
            return "unknown";
        }

        Class<?> clazz = obj.getClass();
        String className = clazz.getSimpleName();

        // 处理lambda表达式
        if (className.contains("$$Lambda$") || className.contains("Lambda")) {
            // 从toString()方法中提取原始类名
            String toString = obj.toString();
            try {
                // 格式：com.example.MyClass$$Lambda$123/456@789
                // 提取：com.example.MyClass
                int lambdaIndex = toString.indexOf("$$Lambda$");
                if (lambdaIndex != -1) {
                    String fullName = toString.substring(0, lambdaIndex);
                    // 获取简单类名
                    int lastDotIndex = fullName.lastIndexOf('.');
                    if (lastDotIndex != -1) {
                        className = fullName.substring(lastDotIndex + 1);
                    } else {
                        className = fullName;
                    }
                } else if (toString.contains("@")) {
                    // 格式：MyClass$$Lambda$123@456
                    int atIndex = toString.indexOf('@');
                    if (atIndex != -1) {
                        String fullName = toString.substring(0, atIndex);
                        int dollarIndex = fullName.indexOf('$');
                        if (dollarIndex != -1) {
                            className = fullName.substring(0, dollarIndex);
                        } else {
                            className = fullName;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to extract original class name from lambda: {}", toString, e);
            }
        }

        return className;
    }

    /**
     * 创建Runnable类型的TaskDefinition
     * 
     * @param runnable Runnable实例
     * @return TaskDefinition
     */
    public TaskDefinition createTaskDefinition(Runnable runnable) {
        if (runnable == null) {
            throw new IllegalArgumentException("Runnable cannot be null");
        }

        TaskDefinition definition = new TaskDefinition();

        // 处理lambda表达式，获取原始类名
        String className = getOriginalClassName(runnable);
        definition.setName(className);
        definition.setDescription("Runnable task");

        return definition;
    }

    /**
     * 创建Callable类型的TaskDefinition
     * 
     * @param callable Callable实例
     * @return TaskDefinition
     */
    public <V> TaskDefinition createTaskDefinition(Callable<V> callable) {
        if (callable == null) {
            throw new IllegalArgumentException("Callable cannot be null");
        }

        TaskDefinition definition = new TaskDefinition();

        // 处理lambda表达式，获取原始类名
        String className = getOriginalClassName(callable);
        definition.setName(className);
        definition.setDescription("Callable task");

        return definition;
    }

    /**
     * 创建TubaTask类型的TaskDefinition
     * 
     * @param task TubaTask实例
     * @return TaskDefinition
     */
    public TaskDefinition createTaskDefinition(TubaTask task) {
        if (task == null) {
            throw new IllegalArgumentException("TubaTask cannot be null");
        }

        TaskDefinition definition = new TaskDefinition();
        definition.setJobClass(task.getClass().getName());

        // 处理lambda表达式，获取原始类名
        String className = getOriginalClassName(task);
        definition.setName(className);
        definition.setDescription("TubaTask");

        return definition;
    }

}
