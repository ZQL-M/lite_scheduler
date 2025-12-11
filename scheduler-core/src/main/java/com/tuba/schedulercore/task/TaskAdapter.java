package com.tuba.schedulercore.task;

import com.tuba.schedulercore.model.TaskDefinition;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Task接口的适配器，将Task接口转换为框架可执行的方法调用
 */
@Component
public class TaskAdapter {

    private static final Method EXECUTE_METHOD;

    static {
        try {
            // 获取Task接口的execute方法
            EXECUTE_METHOD = Task.class.getMethod("execute", com.tuba.schedulercore.model.TaskContext.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Failed to get execute method from Task interface", e);
        }
    }

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
        definition.setBean(task);
        definition.setMethod(EXECUTE_METHOD);
        definition.setName(task.getClass().getSimpleName());
        definition.setDescription("Programmatically registered task");
        return definition;
    }
}
