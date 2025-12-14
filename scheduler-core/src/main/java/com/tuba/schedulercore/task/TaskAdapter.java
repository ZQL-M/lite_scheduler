package com.tuba.schedulercore.task;

import com.tuba.schedulercore.model.TaskContext;
import com.tuba.schedulercore.model.TaskDefinition;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * Task接口的适配器，将Task接口转换为框架可执行的方法调用
 */
@Component
public class TaskAdapter {

    private static final Method EXECUTE_METHOD;
    private static final Method RUNNABLE_TASK_EXECUTE_METHOD;
    private static final Method CALLABLE_TASK_EXECUTE_METHOD;

    static {
        try {
            // 获取Task接口的execute方法
            EXECUTE_METHOD = Task.class.getMethod("execute", com.tuba.schedulercore.model.TaskContext.class);
            // 获取RunnableTaskWrapper的execute方法
            RUNNABLE_TASK_EXECUTE_METHOD = RunnableTaskWrapper.class.getMethod("execute", com.tuba.schedulercore.model.TaskContext.class);
            // 获取CallableTaskWrapper的execute方法
            CALLABLE_TASK_EXECUTE_METHOD = CallableTaskWrapper.class.getMethod("execute", com.tuba.schedulercore.model.TaskContext.class);
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
        definition.setBean(new RunnableTaskWrapper(runnable));
        definition.setMethod(RUNNABLE_TASK_EXECUTE_METHOD);
        definition.setName(runnable.getClass().getSimpleName());
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
        definition.setBean(new CallableTaskWrapper<>(callable));
        definition.setMethod(CALLABLE_TASK_EXECUTE_METHOD);
        definition.setName(callable.getClass().getSimpleName());
        definition.setDescription("Callable task");
        return definition;
    }

    /**
     * Runnable的包装类，将Runnable转换为Task接口
     */
    private static class RunnableTaskWrapper implements Task {
        private final Runnable runnable;

        public RunnableTaskWrapper(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void execute(TaskContext context) {
            runnable.run();
        }
    }

    /**
     * Callable的包装类，将Callable转换为Task接口
     */
    private static class CallableTaskWrapper<V> implements Task {
        private final Callable<V> callable;

        public CallableTaskWrapper(Callable<V> callable) {
            this.callable = callable;
        }

        @Override
        public void execute(TaskContext context) {
            try {
                callable.call();
            } catch (Exception e) {
                throw new RuntimeException("Callable task execution failed", e);
            }
        }
    }
}
