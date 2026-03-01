package com.tuba.schedulercore.scheduler;

import com.tuba.schedulercore.config.ScannerProperties;
import com.tuba.schedulercore.model.TaskDefinition;
import com.tuba.schedulercore.persistence.TaskPersistenceService;
import com.tuba.schedulercore.registry.TaskRegistry;
import com.tuba.schedulercore.task.TubaJobFactory;
import com.tuba.schedulercore.utils.TaskUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 任务扫描仪，定期扫描数据库中的任务并确保它们被正确调度
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

    @Autowired
    private TubaJobFactory tubaJobFactory;

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
        log.info("TaskScanner initialized with scan interval: {}ms",
                scannerProperties.getScanInterval());
    }

    /**
     * 扫描任务
     */
    private void scanTasks() {
        try {
            // 从数据库加载所有已启用的任务
            List<TaskDefinition> tasks = persistenceService.findEnabledTasks();

            int loadedCount = 0;
            for (TaskDefinition task : tasks) {
                // 检查任务是否已加载
                if (!loadedTasks.containsKey(task.getId())) {
                    // 验证任务类
                    if (TaskUtils.validateTaskClass(task)) {
                        // 注册任务到注册表
                        taskRegistry.register(task);
                        // 调度任务
                        scheduler.schedule(task);
                        // 标记为已加载
                        loadedTasks.put(task.getId(), true);
                        loadedCount++;
                        log.info("成功加载任务: {} (ID: {})",
                                task.getName(), task.getId());
                    } else {
                        log.error("加载任务失败: {} (ID: {}) - 任务类验证失败",
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
                log.info("完成扫描任务，本次加载 {} 个任务", loadedCount);
            }
        } catch (Exception e) {
            log.error("扫描任务时发生异常", e);
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