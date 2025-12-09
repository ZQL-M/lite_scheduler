package com.tuba.schedulercore.executor;

import com.tuba.schedulercore.config.ExecutorProperties;
import com.tuba.schedulercore.model.TaskContext;
import com.tuba.schedulercore.model.TaskDefinition;
import com.tuba.schedulercore.model.TaskLog;
import com.tuba.schedulercore.plugin.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 简单执行器：支持同步或使用线程池异步执行。
 */
@Component
public class SimpleTaskExecutor implements TaskExecutor, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(SimpleTaskExecutor.class);
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 30;
    private static final int FORCE_SHUTDOWN_TIMEOUT_SECONDS = 10;

    private final PluginManager pluginManager;
    private final ExecutorService executor;

    @Autowired(required = false)
    public SimpleTaskExecutor(PluginManager pluginManager, ExecutorProperties properties) {
        this.pluginManager = pluginManager;
        int poolSize = properties != null ? properties.getPoolSize() : Runtime.getRuntime().availableProcessors();
        this.executor = Executors.newFixedThreadPool(poolSize);
        log.info("SimpleTaskExecutor initialized with pool size: {}", poolSize);
    }

    @Override
    public void execute(TaskContext context) {
        if (context == null) {
            log.warn("Cannot execute null TaskContext");
            return;
        }
        TaskDefinition def = context.getDefinition();
        if (def == null) {
            log.warn("TaskContext has null TaskDefinition");
            return;
        }
        if (def.isAsync()) {
            executor.submit(() -> invokeAndHandle(context));
        } else {
            invokeAndHandle(context);
        }
    }

    private void invokeAndHandle(TaskContext context) {
        context.setStartTime(Instant.now());
        pluginManager.before(context);

        TaskLog taskLog = createTaskLog(context);
        Throwable error = null;

        try {
            invokeMethod(context);
            context.setEndTime(Instant.now());
            taskLog.setStatus("SUCCESS");
            pluginManager.after(context);
        } catch (Throwable e) {
            context.setEndTime(Instant.now());
            error = extractActualError(e);
            context.setError(error);
            taskLog.setStatus("FAIL");
            taskLog.setMessage(error.getMessage());
            pluginManager.onError(context, error);
        } finally {
            // 统一设置日志时间
            taskLog.setStartTime(context.getStartTime());
            taskLog.setEndTime(context.getEndTime());
            taskLog.setDurationMs(calculateDuration(context));

        }
    }

    /**
     * 创建任务日志对象
     */
    private TaskLog createTaskLog(TaskContext context) {
        TaskLog taskLog = new TaskLog();
        taskLog.setId(java.util.UUID.randomUUID().toString());
        taskLog.setTaskId(context.getDefinition().getId());
        TaskDefinition def = context.getDefinition();
        if (def != null) {
            taskLog.setTaskName(def.getName());
        }
        return taskLog;
    }

    /**
     * 提取真正的异常（处理 InvocationTargetException）
     */
    private Throwable extractActualError(Throwable e) {
        if (e instanceof InvocationTargetException) {
            Throwable cause = e.getCause();
            return cause != null ? cause : e;
        }
        return e;
    }

    /**
     * 计算任务执行时长（毫秒）
     */
    private long calculateDuration(TaskContext context) {
        if (context.getStartTime() != null && context.getEndTime() != null) {
            return context.getEndTime().toEpochMilli() - context.getStartTime().toEpochMilli();
        }
        return 0;
    }

    /**
     * 调用任务方法
     * 
     * @param context 任务上下文
     * @throws InvocationTargetException 方法调用异常
     * @throws IllegalAccessException    非法访问异常
     */
    private void invokeMethod(TaskContext context) throws InvocationTargetException, IllegalAccessException {
        TaskDefinition def = context.getDefinition();
        if (def == null) {
            throw new IllegalStateException("TaskDefinition is null");
        }
        Method method = def.getMethod();
        if (method == null) {
            throw new IllegalStateException("Method is null for task: " + def.getId());
        }
        Object bean = def.getBean();
        if (bean == null) {
            throw new IllegalStateException("Bean is null for task: " + def.getId());
        }
        method.setAccessible(true);

        if (method.getParameterCount() == 0) {
            // 无参方法
            method.invoke(bean);
        } else if (method.getParameterCount() == 1) {
            // 单参数方法，检查是否为 TaskContext 类型
            Class<?> paramType = method.getParameterTypes()[0];
            if (TaskContext.class.isAssignableFrom(paramType)) {
                // 传递正确的 TaskContext（包含 startTime、attributes 等信息）
                method.invoke(bean, context);
            } else {
                throw new IllegalStateException(
                        "Unsupported task method signature: method parameter must be TaskContext, but got "
                                + paramType.getName());
            }
        } else {
            // 不支持的签名，抛异常
            throw new IllegalStateException(
                    "Unsupported task method signature: method must have 0 or 1 parameter (TaskContext), but got "
                            + method.getParameterCount());
        }
    }

    @Override
    public void destroy() throws Exception {
        log.info("Shutting down SimpleTaskExecutor...");
        executor.shutdown(); // 先停止接受新任务

        try {
            // 等待 30 秒让任务完成
            if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                log.warn("Executor did not terminate gracefully within {} seconds, forcing shutdown",
                        SHUTDOWN_TIMEOUT_SECONDS);
                executor.shutdownNow(); // 强制关闭

                // 再等待 10 秒
                if (!executor.awaitTermination(FORCE_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    log.error("Executor did not terminate after force shutdown");
                } else {
                    log.info("Executor terminated after force shutdown");
                }
            } else {
                log.info("Executor terminated gracefully");
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for executor termination", e);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}