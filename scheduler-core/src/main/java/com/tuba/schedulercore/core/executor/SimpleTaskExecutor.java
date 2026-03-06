package com.tuba.schedulercore.core.executor;

import com.tuba.schedulercore.config.properties.ExecutorProperties;
import com.tuba.schedulercore.log.TaskLogService;
import com.tuba.schedulercore.model.TaskContext;
import com.tuba.schedulercore.model.TaskDefinition;
import com.tuba.schedulercore.model.TaskLog;
import com.tuba.schedulercore.core.persistence.TaskPersistenceService;
import com.tuba.schedulercore.plugin.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 简单执行器：支持同步或使用线程池异步执行。
 */
@Component
public class SimpleTaskExecutor implements TaskExecutor, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(SimpleTaskExecutor.class);

    private final PluginManager pluginManager;
    private final ExecutorService executor;
    private final TaskLogService taskLogService;
    private final TaskPersistenceService persistenceService;
    private final ApplicationContext applicationContext;
    private final ExecutorProperties executorProperties;

    /**
     * 生产环境构造函数
     */
    @Autowired(required = false)
    public SimpleTaskExecutor(PluginManager pluginManager, ExecutorProperties properties,
            TaskLogService taskLogService, TaskPersistenceService persistenceService,
            ApplicationContext applicationContext) {
        this.pluginManager = pluginManager;
        this.taskLogService = taskLogService;
        this.persistenceService = persistenceService;
        this.applicationContext = applicationContext;
        this.executorProperties = properties;
        int poolSize = properties != null ? properties.getPoolSize() : Runtime.getRuntime().availableProcessors();
        this.executor = Executors.newFixedThreadPool(poolSize);
        log.info("SimpleTaskExecutor initialized with pool size: {}", poolSize);
    }

    /**
     * 测试专用构造函数
     */
    public SimpleTaskExecutor(PluginManager pluginManager, ExecutorProperties properties) {
        this.pluginManager = pluginManager;
        this.taskLogService = null;
        this.persistenceService = null;
        this.applicationContext = null;
        this.executorProperties = properties;
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
            executor.submit(() -> invokeAndHandleWithTimeout(context));
        } else {
            invokeAndHandleWithTimeout(context);
        }
    }

    private void invokeAndHandleWithTimeout(TaskContext context) {
        TaskDefinition definition = context.getDefinition();
        long timeoutMs = getTaskTimeout(definition);

        // 创建可取消的任务
        Future<?> future = executor.submit(() -> {
            try {
                invokeAndHandle(context);
            } catch (Throwable e) {
                log.error("Task execution failed", e);
                context.setError(e);
                if (taskLogService != null) {
                    TaskLog taskLog = taskLogService.recordTaskStart(context);
                    if (taskLog != null) {
                        taskLogService.recordTaskFailure(context, taskLog, e);
                    }
                }
            }
        });

        // 等待任务完成或超时
        try {
            future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.warn("Task execution timed out after {}ms: {}", timeoutMs, definition.getId());
            future.cancel(true); // 尝试取消任务
            context.setError(new RuntimeException("Task execution timed out"));

            // 记录超时日志
            if (taskLogService != null) {
                TaskLog taskLog = taskLogService.recordTaskStart(context);
                if (taskLog != null) {
                    taskLogService.recordTaskFailure(context, taskLog,
                            new RuntimeException("Task execution timed out"));
                }
            }
        } catch (Exception e) {
            log.error("Error waiting for task execution", e);
        }
    }

    private long getTaskTimeout(TaskDefinition definition) {
        // 优先级：任务定义 > 全局配置 > 默认值
        if (definition.getTimeout() != null && definition.getTimeout() > 0) {
            return definition.getTimeout();
        }
        if (executorProperties != null && executorProperties.getTaskTimeout() > 0) {
            return executorProperties.getTaskTimeout();
        }
        return 60000; // 默认60秒
    }

    private void invokeAndHandle(TaskContext context) {
        TaskDefinition definition = context.getDefinition();
        int maxRetries = getTaskRetryCount(definition);
        long retryInterval = getTaskRetryInterval(definition);
        int retryCount = 0;

        while (true) {
            // 记录任务开始执行
            TaskLog taskLog = null;
            if (taskLogService != null) {
                taskLog = taskLogService.recordTaskStart(context);
            }

            try {
                context.setStartTime(Instant.now());
                pluginManager.before(context);

                invokeMethod(context);

                context.setEndTime(Instant.now());

                // 更新任务定义 - 在新设计中，任务状态和调度信息已分离到TaskStatus和TaskTrigger表
                // 这里只需要更新TaskDefinition的基本信息
                if (definition.isPersistent() && persistenceService != null && persistenceService.isAvailable()) {
                    persistenceService.update(definition);
                }

                // 记录任务执行成功
                if (taskLogService != null && taskLog != null) {
                    taskLogService.recordTaskSuccess(context, taskLog);
                }
                pluginManager.after(context);
                break; // 执行成功，退出循环
            } catch (Throwable e) {
                context.setEndTime(Instant.now());
                Throwable error = extractActualError(e);
                context.setError(error);

                // 更新任务定义 - 在新设计中，任务状态和调度信息已分离到TaskStatus和TaskTrigger表
                // 这里只需要更新TaskDefinition的基本信息
                if (definition.isPersistent() && persistenceService != null && persistenceService.isAvailable()) {
                    persistenceService.update(definition);
                }

                // 检查是否需要重试
                if (retryCount < maxRetries) {
                    retryCount++;
                    log.warn("Task execution failed, will retry {}/{}",
                            retryCount, maxRetries, e);

                    // 记录重试日志
                    if (taskLogService != null && taskLog != null) {
                        // 暂时使用recordTaskFailure记录重试，后续可以添加recordTaskRetry方法
                        taskLogService.recordTaskFailure(context, taskLog, error);
                    }

                    // 等待重试间隔
                    try {
                        Thread.sleep(retryInterval);
                    } catch (InterruptedException ie) {
                        log.warn("Retry sleep interrupted", ie);
                        Thread.currentThread().interrupt();
                        break;
                    }

                    // 重置上下文
                    context.setError(null);
                    context.setStartTime(null);
                    context.setEndTime(null);
                } else {
                    // 达到最大重试次数，记录失败
                    log.error("Task execution failed after {} retries: {}", maxRetries, definition.getId(), e);
                    if (taskLogService != null && taskLog != null) {
                        taskLogService.recordTaskFailure(context, taskLog, error);
                    }
                    pluginManager.onError(context, error);
                    break;
                }
            }
        }
    }

    private int getTaskRetryCount(TaskDefinition definition) {
        // 优先级：任务定义 > 全局配置 > 默认值
        if (definition.getRetryCount() != null) {
            return definition.getRetryCount();
        }
        if (executorProperties != null && executorProperties.getDefaultRetryCount() >= 0) {
            return executorProperties.getDefaultRetryCount();
        }
        return 0; // 默认不重试
    }

    private long getTaskRetryInterval(TaskDefinition definition) {
        // 优先级：任务定义 > 全局配置 > 默认值
        if (definition.getRetryInterval() != null && definition.getRetryInterval() > 0) {
            return definition.getRetryInterval();
        }
        if (executorProperties != null && executorProperties.getDefaultRetryInterval() > 0) {
            return executorProperties.getDefaultRetryInterval();
        }
        return 1000; // 默认1秒
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

        // 从jobClass创建实例并调用execute方法
        String jobClass = def.getJobClass();
        if (jobClass == null || jobClass.isEmpty()) {
            throw new IllegalStateException("JobClass is null for task: " + def.getId());
        }

        try {
            // 尝试从Spring容器获取Bean
            Object bean = null;
            try {
                // 尝试根据类名获取Bean
                Class<?> clazz = Class.forName(jobClass);
                // 查找容器中是否有该类型的Bean
                if (applicationContext != null) {
                    Map<String, ?> beans = applicationContext.getBeansOfType(clazz);
                    if (!beans.isEmpty()) {
                        bean = beans.values().iterator().next();
                        log.info("Found task bean in Spring container: {}", jobClass);
                    }
                }
            } catch (Exception e) {
                log.debug("No bean found in Spring container, will create new instance: {}", e.getMessage());
            }

            // 如果容器中没有，则反射创建
            if (bean == null) {
                Class<?> clazz = Class.forName(jobClass);
                bean = clazz.newInstance();
                log.info("Created new task instance: {}", jobClass);
            }

            // 查找execute方法
            Method method = null;
            try {
                // 尝试查找带TaskContext参数的execute方法
                method = bean.getClass().getDeclaredMethod("execute", TaskContext.class);
                method.setAccessible(true);
                method.invoke(bean, context);
            } catch (NoSuchMethodException e) {
                // 尝试查找无参数的execute方法
                method = bean.getClass().getDeclaredMethod("execute");
                method.setAccessible(true);
                method.invoke(bean);
            }
        } catch (ClassNotFoundException | InstantiationException | NoSuchMethodException e) {
            throw new RuntimeException("Failed to execute task: " + def.getId(), e);
        }
    }

    @Override
    public void destroy() {
        log.info("Shutting down SimpleTaskExecutor...");
        executor.shutdown(); // 先停止接受新任务

        // 从配置获取关闭超时时间
        int shutdownTimeout = executorProperties != null ? executorProperties.getShutdownTimeoutSeconds() : 30;
        int forceShutdownTimeout = executorProperties != null ? executorProperties.getForceShutdownTimeoutSeconds()
                : 10;

        try {
            // 等待任务完成
            if (!executor.awaitTermination(shutdownTimeout, TimeUnit.SECONDS)) {
                log.warn("Executor did not terminate gracefully within {} seconds, forcing shutdown",
                        shutdownTimeout);
                executor.shutdownNow(); // 强制关闭

                // 再等待
                if (!executor.awaitTermination(forceShutdownTimeout, TimeUnit.SECONDS)) {
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