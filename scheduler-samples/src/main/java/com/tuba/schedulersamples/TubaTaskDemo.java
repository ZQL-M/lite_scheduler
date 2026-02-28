package com.tuba.schedulersamples;

import com.tuba.schedulercore.model.TaskContext;
import com.tuba.schedulercore.model.TaskDefinition;
import com.tuba.schedulercore.service.TaskTriggerService;
import com.tuba.schedulercore.task.TubaTask;
import com.tuba.schedulercore.service.impl.TaskSchedulerServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * TubaTask 接口实现示例
 * 演示如何创建持久化到数据库、只执行一次的任务
 */
@Component
public class TubaTaskDemo implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TubaTaskDemo.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private TaskSchedulerServiceImpl taskSchedulerService;

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) {
        log.info("开始演示 TubaTask 持久化任务...");

        // 演示创建一个只执行一次、一分钟后执行、持久化到数据库的任务
        demoOneTimePersistentTask();

        log.info("TubaTask 持久化任务演示设置完成");
    }

    /**
     * 演示创建一个只执行一次、一分钟后执行、持久化到数据库的任务
     */
    private void demoOneTimePersistentTask() {
        // 任务配置
        String taskId = "demo-once-persistent-task";
        String taskName = "OneTimePersistentTask";
        String taskGroup = "demo";

        // 检查是否已存在该任务，避免重复创建
        try {
            TaskDefinition existingTask = taskSchedulerService.getTaskDefinition(taskId);
            if (existingTask != null) {
                log.info("✅ 任务 {} (ID: {}) 已存在，跳过创建", taskName, taskId);
                return;
            }
        } catch (Exception e) {
            log.warn("⚠️  检查任务是否存在时出错: {}", e.getMessage());
            // 继续执行，允许创建新任务
        }

        // 计算一分钟后的时间点
        LocalDateTime executeTime = LocalDateTime.now().plusMinutes(1);
        log.info("⏰ 计划执行时间: {}", executeTime.format(FORMATTER));

        // 生成Cron表达式，只执行一次
        String cron = String.format("%d %d %d %d %d ?",
                executeTime.getSecond(),
                executeTime.getMinute(),
                executeTime.getHour(),
                executeTime.getDayOfMonth(),
                executeTime.getMonthValue());

        // 创建 TubaTask 实例
        TubaTask oneTimeTask = context -> {
            log.info("=========================================");
            log.info("[持久化任务] >>> 任务开始执行 <<<");
            log.info("[持久化任务] 执行时间: {}", LocalDateTime.now().format(FORMATTER));
            log.info("[持久化任务] 线程名称: {}", Thread.currentThread().getName());
            log.info("[持久化任务] 任务ID: {}", context.getDefinition().getId());
            log.info("[持久化任务] 任务名称: {}", context.getDefinition().getName());
            log.info("[持久化任务] 持久化状态: {}", context.getDefinition().isPersistent());
            log.info("[持久化任务] >>> 任务执行完成 <<<");
            log.info("=========================================");

            // 任务执行完成后，可以选择取消任务
            try {
                taskSchedulerService.cancelTask(context.getDefinition().getId());
                log.info("[持久化任务] 任务已取消，避免重复执行");
            } catch (Exception e) {
                log.warn("[持久化任务] 取消任务时出错: {}", e.getMessage());
            }
        };

        // 创建任务定义
        TaskDefinition definition = new TaskDefinition();
        definition.setId(taskId);
        definition.setName(taskName);
        definition.setGroupName(taskGroup);
        definition.setPersistent(true); // 关键：设置为持久化
        definition.setEnabled(true);

        try {
            // 注册任务
            String registeredTaskId = taskSchedulerService.registerTask(oneTimeTask, definition);

            // 创建Cron触发器，设置为一分钟后执行
            TaskTriggerService taskTriggerService = applicationContext.getBean(TaskTriggerService.class);
            taskTriggerService.createCronTrigger(registeredTaskId, cron, null, null, null, null, null);

            log.info("✅ 成功注册持久化任务");
            log.info("📅 任务ID: {}", registeredTaskId);
            log.info("⏰ 执行时间: {}", executeTime.format(FORMATTER));
            log.info("💾 持久化: {}", definition.isPersistent());
            log.info("⚙️  Cron表达式: {}", cron);
            log.info("📝 任务描述: 只执行一次，一分钟后执行，持久化到数据库");
        } catch (Exception e) {
            if (e.getMessage().contains("Duplicate entry") || e.getMessage().contains("duplicate key")) {
                log.info("⚠️  任务 {} (ID: {}) 已存在于数据库中，跳过创建", taskName, taskId);
            } else {
                log.error("❌ 注册持久化任务失败: {}", e.getMessage());
                throw e;
            }
        }
    }
}
