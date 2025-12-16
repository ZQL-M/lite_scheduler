package com.tuba.schedulersamples;

import com.tuba.schedulercore.model.TaskContext;
import com.tuba.schedulercore.model.TaskDefinition;
import com.tuba.schedulercore.service.TaskSchedulerService;
import com.tuba.schedulercore.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
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
        
        // 创建任务定义，使用固定ID
        TaskDefinition definition = new TaskDefinition();
        definition.setId(taskId); // 设置固定ID，避免重复创建
        definition.setName(taskName);
        definition.setGroup(taskGroup);
        
        // 计算一分钟后的时间点
        LocalDateTime dueTime = LocalDateTime.now().plusMinutes(1);
        // 生成Cron表达式：ss mm HH dd MM ?（标准6字段格式）
        String cron = String.format("%d %d %d %d %d ?",
                dueTime.getSecond(),
                dueTime.getMinute(),
                dueTime.getHour(),
                dueTime.getDayOfMonth(),
                dueTime.getMonthValue());
        definition.setCron(cron); // 使用Cron表达式设置一分钟后执行
        definition.setRepeatCount(1); // 只执行一次
        definition.setPersistent(true); // 持久化到数据库，支持重启恢复
        definition.setEnabled(true); // 启用任务
        
        // 设置beanName和methodName，使用当前类作为Spring bean
        // Spring默认bean名称是类名首字母小写，所以使用oneTimeTaskDemo
        definition.setBeanName("oneTimeTaskDemo");
        definition.setMethodName("executeOneTimeTask"); // 使用当前类的方法

        // 使用当前对象作为Task实例，因为executeOneTimeTask方法实现了任务逻辑
        Task task = this::executeOneTimeTask;
        
        // 计算剩余时间
        long remainingSeconds = java.time.Duration.between(LocalDateTime.now(), dueTime).getSeconds();
        log.info("⏰ 一次性任务将在 {} 执行 (剩余 {} 秒)", dueTime, remainingSeconds);

        // 注册任务，添加try-catch块捕获重复键异常
        try {
            String registeredTaskId = taskSchedulerService.registerTask(task, definition);
            log.info("✅ 成功注册一次性任务");
            log.info("📅 任务ID: {}", registeredTaskId);
            log.info("⏰ 执行时间: {}", dueTime);
            log.info("🔄 重复次数: {}", definition.getRepeatCount());
            log.info("💾 持久化: {}", definition.isPersistent());
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