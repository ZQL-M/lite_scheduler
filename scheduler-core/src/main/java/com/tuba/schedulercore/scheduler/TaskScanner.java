package com.tuba.schedulercore.scheduler;

import com.tuba.schedulercore.config.ScannerProperties;
import com.tuba.schedulercore.model.TaskDefinition;
import com.tuba.schedulercore.persistence.TaskPersistenceService;
import com.tuba.schedulercore.registry.TaskRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 任务扫描仪，定期扫描数据库中的临期任务
 */
@Component
public class TaskScanner implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(TaskScanner.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TaskPersistenceService persistenceService;

    @Autowired
    private TaskRegistry taskRegistry;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private ScannerProperties scannerProperties;

    private final ThreadPoolTaskScheduler taskScheduler;
    private ScheduledFuture<?> scanTask;

    // 记录当前已加载到内存的任务ID，避免重复加载
    private final ConcurrentHashMap<String, Boolean> loadedTasks = new ConcurrentHashMap<>();

    public TaskScanner() {
        this.taskScheduler = new ThreadPoolTaskScheduler();
        this.taskScheduler.setPoolSize(1);
        this.taskScheduler.setThreadNamePrefix("task-scanner-");
        this.taskScheduler.initialize();
    }

    /**
     * 初始化任务扫描器
     */
    @PostConstruct
    public void init() {
        // 启动扫描任务
        scanTask = taskScheduler.scheduleAtFixedRate(
                this::scanTasks,
                scannerProperties.getScanInterval());
        log.info("TaskScanner initialized with scan interval: {}ms, scan ahead time: {}ms",
                scannerProperties.getScanInterval(), scannerProperties.getScanAheadTime());
    }

    /**
     * 扫描任务
     */
    private void scanTasks() {
        try {
            log.debug("开始扫描临期任务...");

            // 计算扫描时间范围
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime scanAheadTime = now.plusNanos(scannerProperties.getScanAheadTime() * 1000000);

            // 从数据库加载临期任务
            // 注意：这里需要TaskPersistenceService添加一个findTasksByNextFireTimeRange方法
            // 目前先使用findEnabledTasks方法作为替代
            List<TaskDefinition> tasks = persistenceService.findEnabledTasks();

            int loadedCount = 0;
            for (TaskDefinition task : tasks) {
                // 检查任务是否在扫描时间范围内
                if (task.getNextFireTime() != null &&
                        task.getNextFireTime().isBefore(scanAheadTime) &&
                        !loadedTasks.containsKey(task.getId())) {

                    // 解析bean和method
                    if (parseBeanAndMethod(task)) {
                        // 注册任务到注册表
                        taskRegistry.register(task);
                        // 调度任务
                        scheduler.schedule(task);
                        // 标记为已加载
                        loadedTasks.put(task.getId(), true);
                        loadedCount++;
                        log.debug("成功加载临期任务: {} (ID: {}), 下次触发时间: {}",
                                task.getName(), task.getId(), task.getNextFireTime());
                    } else {
                        log.error("加载临期任务失败: {} (ID: {}) - 无法解析bean或method",
                                task.getName(), task.getId());
                    }

                    // 检查是否达到单次扫描最大任务数量
                    if (loadedCount >= scannerProperties.getMaxTasksPerScan()) {
                        log.debug("已达到单次扫描最大任务数量: {}, 结束本次扫描", scannerProperties.getMaxTasksPerScan());
                        break;
                    }
                }
            }

            log.debug("完成扫描临期任务，本次加载 {} 个任务", loadedCount);
        } catch (Exception e) {
            log.error("扫描任务时发生异常", e);
        }
    }

    /**
     * 解析bean和method对象
     * 
     * @param task 任务定义
     * @return 是否解析成功
     */
    private boolean parseBeanAndMethod(TaskDefinition task) {
        try {
            if (task.getBeanName() == null || task.getBeanName().isEmpty()) {
                log.error("任务 {} (ID: {}) 的beanName为空", task.getName(), task.getId());
                return false;
            }

            if (task.getMethodName() == null || task.getMethodName().isEmpty()) {
                log.error("任务 {} (ID: {}) 的methodName为空", task.getName(), task.getId());
                return false;
            }

            // 从ApplicationContext获取bean实例
            Object bean = applicationContext.getBean(task.getBeanName());
            if (bean == null) {
                log.error("无法从ApplicationContext获取bean: {}", task.getBeanName());
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

    /**
     * 从内存中移除任务，允许下次扫描时重新加载
     * 
     * @param taskId 任务ID
     */
    public void removeLoadedTask(String taskId) {
        loadedTasks.remove(taskId);
        log.debug("从已加载任务列表中移除任务: {}", taskId);
    }

    @Override
    public void destroy() throws Exception {
        // 取消扫描任务
        if (scanTask != null) {
            scanTask.cancel(true);
        }
        // 关闭线程池
        taskScheduler.shutdown();
        log.info("TaskScanner has been destroyed");
    }
}