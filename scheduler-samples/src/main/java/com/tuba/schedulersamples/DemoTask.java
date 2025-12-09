package com.tuba.schedulersamples;

import com.tuba.schedulercore.annotation.ScheduledTask;
import com.tuba.schedulercore.model.TaskContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 演示各种调度方式的任务类
 * 注意：此类已被禁用，仅用于参考
 */
@Component
public class DemoTask {

    /**
     * 示例1：使用注解调度 - Cron表达式
     * 已禁用 - 每分钟的0和30秒执行
     */
    // @ScheduledTask(
    //         name = "Cron表达式任务",
    //         cron = "0,30 * * * * ?",
    //         description = "每分钟的0和30秒执行",
    //         persistent = true
    // )
    public void cronTask() {
        System.out.println("[Cron任务] 执行时间: " + LocalDateTime.now());
    }

    /**
     * 示例2：使用注解调度 - 固定频率（每5秒执行一次）
     * 已禁用
     */
    // @ScheduledTask(
    //         name = "固定频率任务",
    //         fixedRate = 5000,
    //         description = "每5秒执行一次",
    //         async = true
    // )
    public void fixedRateTask() {
        System.out.println("[固定频率任务] 执行时间: " + LocalDateTime.now());
    }

    /**
     * 示例3：使用注解调度 - 固定延迟（执行完成后延迟3秒再次执行）
     * 已禁用
     */
    // @ScheduledTask(
    //         name = "固定延迟任务",
    //         fixedDelay = 3000,
    //         description = "执行完成后延迟3秒再次执行",
    //         async = false
    // )
    public void fixedDelayTask() {
        System.out.println("[固定延迟任务] 执行时间: " + LocalDateTime.now());
        // 模拟任务执行耗时
        try {
            Thread.sleep(1000);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 示例4：使用注解调度 - 简化时间配置（每10秒执行一次）
     * 已禁用
     */
    // @ScheduledTask(
    //         name = "简化时间配置任务",
    //         seconds = "10",
    //         description = "每10秒执行一次",
    //         persistent = true
    // )
    public void simplifiedTimeTask() {
        System.out.println("[简化时间配置任务] 执行时间: " + LocalDateTime.now());
    }

    /**
     * 示例5：使用注解调度 - 有限循环（执行3次后停止）
     * 已禁用
     */
    // @ScheduledTask(
    //         name = "有限循环任务",
    //         seconds = "2",
    //         repeatCount = 3,
    //         description = "执行3次后停止",
    //         persistent = true
    // )
    public void finiteLoopTask() {
        System.out.println("[有限循环任务] 执行时间: " + LocalDateTime.now());
    }

    /**
     * 示例6：使用注解调度 - 带执行上下文
     * 已禁用
     */
    // @ScheduledTask(
    //         name = "带上下文的任务",
    //         cron = "0/30 * * * * ?",
    //         description = "每30秒执行一次，带执行上下文",
    //         persistent = true
    // )
    public void contextTask(TaskContext context) {
        System.out.println("[带上下文的任务] taskId=" + context.getDefinition().getId() + ", 执行时间: " + LocalDateTime.now());
        // 设置自定义属性
        context.putAttribute("executedCount", context.getAttribute("executedCount", Integer.class) != null ? 
                context.getAttribute("executedCount", Integer.class) + 1 : 1);
        System.out.println("[带上下文的任务] 执行次数: " + context.getAttribute("executedCount"));
    }

    /**
     * 示例7：编程式调度的目标方法
     */
    public void programmaticTask() {
        System.out.println("[编程式任务] 执行时间: " + LocalDateTime.now());
    }

    /**
     * 示例8：编程式调度 - 带参数的方法（演示如何处理）
     */
    public void programmaticTaskWithParam(String param) {
        System.out.println("[编程式任务-带参数] 执行时间: " + LocalDateTime.now() + ", 参数: " + param);
    }

    /**
     * 示例9：REST API 调度的目标方法
     */
    public void restApiTask() {
        System.out.println("[REST API任务] 执行时间: " + LocalDateTime.now());
    }

    /**
     * 示例10：事件驱动调度的目标方法
     */
    public void eventDrivenTask() {
        System.out.println("[事件驱动任务] 执行时间: " + LocalDateTime.now());
    }
}
