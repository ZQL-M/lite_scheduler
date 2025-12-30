package com.tuba.schedulersamples;

import com.tuba.schedulercore.model.TaskContext;
import com.tuba.schedulercore.model.TaskDefinition;
import com.tuba.schedulercore.service.TaskSchedulerService;
import com.tuba.schedulercore.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 一次性任务演示
 * 演示一分钟后执行一次的任务，验证重启后不会丢失
 */
@Component  // 注释掉@Component注解，避免Spring扫描
public class OneTimeTaskDemo implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OneTimeTaskDemo.class);

    @Resource
    private TaskSchedulerService taskSchedulerService;
    
    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) {
        log.info("开始演示一次性任务...");

        // 创建一次性任务
        demoOneTimeTask();

        log.info("一次性任务演示设置完成");
    }

    /**
     * 演示一次性任务
     */
    private void demoOneTimeTask() {
        // 任务唯一标识，使用固定ID避免重复创建
        String taskId = "demo-once-task-id";
        String taskName = "OneTimeDemoTask";
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
        LocalDateTime dueTime = LocalDateTime.now().plusMinutes(1);
        // 生成Cron表达式：ss mm HH dd MM ?（标准6字段格式）
        String cron = String.format("%d %d %d %d %d ?",
                dueTime.getSecond(),
                dueTime.getMinute(),
                dueTime.getHour(),
                dueTime.getDayOfMonth(),
                dueTime.getMonthValue());
        
        // 1. 创建Task实例
        Task task = this::executeOneTimeTask;
        
        // 直接创建并配置TaskDefinition，避免重复注册
        TaskDefinition definition = new TaskDefinition();
        definition.setId(taskId); // 设置固定ID，避免重复创建
        definition.setName(taskName);
        definition.setGroupName(taskGroup);
        definition.setPersistent(true); // 持久化到数据库
        definition.setEnabled(true); // 启用任务
        
        // 3. 只注册一次任务
        try {
            // 注册任务，使用简洁的方式
            String registeredTaskId = taskSchedulerService.registerTask(task, definition);
            
            // 创建Cron触发器，设置一分钟后执行
            com.tuba.schedulercore.service.TaskTriggerService taskTriggerService = applicationContext.getBean(com.tuba.schedulercore.service.TaskTriggerService.class);
            taskTriggerService.createCronTrigger(registeredTaskId, cron, null, null, null, null, null);
            
            // 计算剩余时间
            long remainingSeconds = Duration.between(LocalDateTime.now(), dueTime).getSeconds();
            log.info("✅ 成功注册一次性任务");
            log.info("📅 任务ID: {}", registeredTaskId);
            log.info("⏰ 执行时间: {}", dueTime);
            log.info(" 持久化: {}", definition.isPersistent());
            log.info("⚙️  Cron表达式: {}", cron);
        } catch (Exception e) {
            if (e.getMessage().contains("Duplicate entry") || e.getMessage().contains("duplicate key")) {
                log.info("⚠️  任务 {} (ID: {}) 已存在于数据库中，跳过创建", taskName, taskId);
            } else {
                log.error("❌ 注册一次性任务失败: {}", e.getMessage());
                throw e;
            }
        }
    }

    /**
     * 一次性任务的执行逻辑
     * 
     * @param context 任务上下文
     */
    public void executeOneTimeTask(TaskContext context) {
        // 醒目输出
        log.info("=========================================");
        log.info("[一次性任务] >>> 任务开始执行 <<<");
        log.info("[一次性任务] 执行时间: {}", LocalDateTime.now());
        log.info("[一次性任务] 线程名称: {}", Thread.currentThread().getName());
        log.info("[一次性任务] 任务ID: {}", context.getDefinition().getId());
        log.info("[一次性任务] 任务名称: {}", context.getDefinition().getName());
        log.info("[一次性任务] >>> 任务执行完成 <<<");
        log.info("=========================================");

    }

}