package com.tuba.schedulercore.scheduler;

import com.tuba.schedulercore.config.ScannerProperties;
import com.tuba.schedulercore.model.TaskDefinition;
import com.tuba.schedulercore.enums.TaskStatus;
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
            // 计算扫描时间范围
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime scanAheadTime = now.plusNanos(scannerProperties.getScanAheadTime() * 1000000);

            // 从数据库加载临期任务
            List<TaskDefinition> tasks = persistenceService.findTasksByNextFireTimeRange(now, scanAheadTime);

            int loadedCount = 0;
            for (TaskDefinition task : tasks) {
                // 检查任务是否已加载或已完成
                if (!loadedTasks.containsKey(task.getId()) && task.getStatus() != TaskStatus.COMPLETED) {
                    // 解析bean和method
                    if (parseBeanAndMethod(task)) {
                        // 更新任务状态为待执行
                        task.setStatus(TaskStatus.PENDING);
                        // 注册任务到注册表
                        taskRegistry.register(task);
                        // 调度任务
                        scheduler.schedule(task);
                        // 标记为已加载
                        loadedTasks.put(task.getId(), true);
                        loadedCount++;
                        log.info("成功加载临期任务: {} (ID: {}), 状态: {}, 下次触发时间: {}",
                                task.getName(), task.getId(), task.getStatus(), task.getNextFireTime());
                    } else {
                        log.error("加载临期任务失败: {} (ID: {}) - 无法解析bean或method",
                                task.getName(), task.getId());
                    }

                    // 检查是否达到单次扫描最大任务数量
                    if (loadedCount >= scannerProperties.getMaxTasksPerScan()) {
                        log.info("已达到单次扫描最大任务数量: {}, 结束本次扫描", scannerProperties.getMaxTasksPerScan());
                        break;
                    }
                }
            }

            // 只有在加载了任务或者发生了错误时才输出完成日志
            if (loadedCount > 0) {
                log.info("完成扫描临期任务，本次加载 {} 个任务", loadedCount);
            }
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
        return com.tuba.schedulercore.utils.TaskUtils.parseBeanAndMethod(applicationContext, task);
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