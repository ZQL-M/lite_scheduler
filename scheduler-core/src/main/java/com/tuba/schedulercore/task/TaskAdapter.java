package com.tuba.schedulercore.task;

import com.tuba.schedulercore.model.TaskContext;
import com.tuba.schedulercore.model.TaskDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * Task接口的适配器，将Task接口转换为框架可执行的方法调用
 */
@Component
public class TaskAdapter {

    private static final Logger log = LoggerFactory.getLogger(TaskAdapter.class);
    private static final Method EXECUTE_METHOD;
    private static final Method RUNNABLE_TASK_EXECUTE_METHOD;
    private static final Method CALLABLE_TASK_EXECUTE_METHOD;

    static {
        try {
            // 获取Task接口的execute方法
            EXECUTE_METHOD = Task.class.getMethod("execute", TaskContext.class);
            // 获取RunnableTaskWrapper的execute方法
            RUNNABLE_TASK_EXECUTE_METHOD = RunnableTaskWrapper.class.getMethod("execute",
                    TaskContext.class);
            // 获取CallableTaskWrapper的execute方法
            CALLABLE_TASK_EXECUTE_METHOD = CallableTaskWrapper.class.getMethod("execute",
                    TaskContext.class);
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

        // 处理lambda表达式，获取原始类名
        String className = getOriginalClassName(task);
        definition.setName(className);
        definition.setDescription("Programmatically registered task");

        // 自动设置beanName和methodName，与注解创建保持一致
        // 获取beanName：使用Spring默认规则，首字母小写
        String beanName = className;
        if (beanName.length() > 1) {
            beanName = Character.toLowerCase(beanName.charAt(0)) + beanName.substring(1);
        }
        definition.setBeanName(beanName);

        // 获取methodName：使用Task接口的execute方法名
        definition.setMethodName(EXECUTE_METHOD.getName());

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
        RunnableTaskWrapper taskWrapper = new RunnableTaskWrapper(runnable);
        definition.setBean(taskWrapper);
        definition.setMethod(RUNNABLE_TASK_EXECUTE_METHOD);

        // 处理lambda表达式，获取原始类名
        String className = getOriginalClassName(runnable);
        definition.setName(className);
        definition.setDescription("Runnable task");

        // 自动设置beanName和methodName，与注解创建保持一致
        // 获取beanName：使用Spring默认规则，首字母小写
        String beanName = className;
        if (beanName.length() > 1) {
            beanName = Character.toLowerCase(beanName.charAt(0)) + beanName.substring(1);
        }
        definition.setBeanName(beanName);

        // 获取methodName：使用RunnableTaskWrapper的execute方法名
        definition.setMethodName(RUNNABLE_TASK_EXECUTE_METHOD.getName());

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
        CallableTaskWrapper<V> taskWrapper = new CallableTaskWrapper<>(callable);
        definition.setBean(taskWrapper);
        definition.setMethod(CALLABLE_TASK_EXECUTE_METHOD);

        // 处理lambda表达式，获取原始类名
        String className = getOriginalClassName(callable);
        definition.setName(className);
        definition.setDescription("Callable task");

        // 自动设置beanName和methodName，与注解创建保持一致
        // 获取beanName：使用Spring默认规则，首字母小写
        String beanName = className;
        if (beanName.length() > 1) {
            beanName = Character.toLowerCase(beanName.charAt(0)) + beanName.substring(1);
        }
        definition.setBeanName(beanName);

        // 获取methodName：使用CallableTaskWrapper的execute方法名
        definition.setMethodName(CALLABLE_TASK_EXECUTE_METHOD.getName());

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
