package com.tuba.schedulersamples;

import com.tuba.schedulercore.annotation.TubaTask;
import com.tuba.schedulercore.enums.TimeUnit;
import com.tuba.schedulercore.model.TaskContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 演示各种@TubaTask注解使用方式的任务类
 * 从简单到复杂的示例展示
 */
@Component
public class DemoTask {

    /**
     * 示例1：最简单的使用 - 默认异步，每10秒执行一次
     * 使用默认值：异步执行，毫秒级（未指定type时默认）
     */
    @TubaTask(
            name = "最简单的任务",
            interval = 10000,
            // repeatCount = 3,
            description = "每10秒执行一次，使用默认异步执行"
    )
    public void simpleTask() {
        System.out.println("[最简单的任务] 执行时间: " + LocalDateTime.now() + ", 线程: " + Thread.currentThread().getName());
    }

    // /**
    //  * 示例2：使用TimeUnit.SECONDS - 每5秒执行一次
    //  * 显式指定时间单位为秒
    //  */
    // @TubaTask(
    //         name = "秒级任务",
    //         interval = 5,
    //         type = TimeUnit.SECONDS,
    //         description = "每5秒执行一次，显式指定秒级"
    // )
    // public void secondTask() {
    //     System.out.println("[秒级任务] 执行时间: " + LocalDateTime.now() + ", 线程: " + Thread.currentThread().getName());
    // }

    // /**
    //  * 示例3：使用TimeUnit.MINUTES - 每1分钟执行一次
    //  * 显式指定时间单位为分钟
    //  */
    // @TubaTask(
    //         name = "分钟级任务",
    //         interval = 1,
    //         type = TimeUnit.MINUTES,
    //         description = "每1分钟执行一次，显式指定分钟级"
    // )
    // public void minuteTask() {
    //     System.out.println("[分钟级任务] 执行时间: " + LocalDateTime.now() + ", 线程: " + Thread.currentThread().getName());
    // }
    //
    // /**
    //  * 示例4：使用cron表达式 - 每分钟的第10秒执行
    //  * 使用cron表达式进行更复杂的调度
    //  */
    // @TubaTask(
    //         name = "Cron任务",
    //         cron = "10 * * * * ?",
    //         description = "每分钟的第10秒执行"
    // )
    // public void cronTask() {
    //     System.out.println("[Cron任务] 执行时间: " + LocalDateTime.now() + ", 线程: " + Thread.currentThread().getName());
    // }
    //
    // /**
    //  * 示例5：同步执行任务 - 每3秒执行一次
    //  * 设置async=false，改为同步执行
    //  */
    // @TubaTask(
    //         name = "同步执行任务",
    //         interval = 3,
    //         type = TimeUnit.SECONDS,
    //         async = false,
    //         description = "每3秒执行一次，同步执行"
    // )
    // public void syncTask() {
    //     System.out.println("[同步执行任务] 执行时间: " + LocalDateTime.now() + ", 线程: " + Thread.currentThread().getName());
    //     // 模拟任务执行耗时
    //     try {
    //         Thread.sleep(1000);
    //     } catch (InterruptedException e) {
    //         e.printStackTrace();
    //     }
    // }
    //
    // /**
    //  * 示例6：有限循环任务 - 执行5次后停止
    //  * 设置repeatCount=5，执行5次后停止
    //  */
    // @TubaTask(
    //         name = "有限循环任务",
    //         interval = 2,
    //         type = TimeUnit.SECONDS,
    //         repeatCount = 5,
    //         description = "每2秒执行一次，执行5次后停止"
    // )
    // public void finiteLoopTask() {
    //     System.out.println("[有限循环任务] 执行时间: " + LocalDateTime.now() + ", 线程: " + Thread.currentThread().getName());
    // }
    //
    // /**
    //  * 示例7：带执行上下文的任务 - 每8秒执行一次
    //  * 接收TaskContext参数，可以获取任务信息和设置上下文属性
    //  */
    // @TubaTask(
    //         name = "带上下文的任务",
    //         interval = 8,
    //         type = TimeUnit.SECONDS,
    //         description = "每8秒执行一次，带执行上下文"
    // )
    // public void contextTask(TaskContext context) {
    //     System.out.println("[带上下文的任务] taskId=" + context.getDefinition().getId() + ", 任务名称: " + context.getDefinition().getName());
    //     System.out.println("[带上下文的任务] 执行时间: " + LocalDateTime.now() + ", 线程: " + Thread.currentThread().getName());
    //
    //     // 使用上下文存储和获取执行次数
    //     Integer executedCount = context.getAttribute("executedCount", Integer.class);
    //     if (executedCount == null) {
    //         executedCount = 0;
    //     }
    //     executedCount++;
    //     context.putAttribute("executedCount", executedCount);
    //     System.out.println("[带上下文的任务] 已执行次数: " + executedCount);
    // }
    //
    // /**
    //  * 示例8：使用任务分组 - 每7秒执行一次
    //  * 设置group属性，用于任务分组管理
    //  */
    // @TubaTask(
    //         name = "分组任务",
    //         interval = 7,
    //         type = TimeUnit.SECONDS,
    //         group = "demo-group",
    //         description = "每7秒执行一次，属于demo-group分组"
    // )
    // public void groupTask() {
    //     System.out.println("[分组任务] 执行时间: " + LocalDateTime.now() + ", 线程: " + Thread.currentThread().getName());
    // }
    //
    // /**
    //  * 示例9：使用TimeUnit.HOURS - 每1小时执行一次
    //  * 显式指定时间单位为小时
    //  */
    // @TubaTask(
    //         name = "小时级任务",
    //         interval = 1,
    //         type = TimeUnit.HOURS,
    //         description = "每1小时执行一次，显式指定小时级"
    // )
    // public void hourTask() {
    //     System.out.println("[小时级任务] 执行时间: " + LocalDateTime.now() + ", 线程: " + Thread.currentThread().getName());
    // }
    //
    // /**
    //  * 示例10：使用TimeUnit.DAYS - 每1天执行一次
    //  * 显式指定时间单位为天
    //  */
    // @TubaTask(
    //         name = "天级任务",
    //         interval = 1,
    //         type = TimeUnit.DAYS,
    //         description = "每1天执行一次，显式指定天级"
    // )
    // public void dayTask() {
    //     System.out.println("[天级任务] 执行时间: " + LocalDateTime.now() + ", 线程: " + Thread.currentThread().getName());
    // }
    //
    // /**
    //  * 示例11：复杂cron表达式 - 每天的10:30和14:30执行
    //  * 使用复杂的cron表达式进行特定时间点调度
    //  */
    // @TubaTask(
    //         name = "复杂Cron任务",
    //         cron = "0 30 10,14 * * ?",
    //         description = "每天的10:30和14:30执行，使用复杂cron表达式"
    // )
    // public void complexCronTask() {
    //     System.out.println("[复杂Cron任务] 执行时间: " + LocalDateTime.now() + ", 线程: " + Thread.currentThread().getName());
    // }
    //
    // /**
    //  * 示例12：不执行任务 - repeatCount=0
    //  * 设置repeatCount=0，任务不会执行
    //  */
    // @TubaTask(
    //         name = "不执行任务",
    //         interval = 1,
    //         type = TimeUnit.SECONDS,
    //         repeatCount = 0,
    //         description = "设置repeatCount=0，任务不会执行"
    // )
    // public void noExecuteTask() {
    //     System.out.println("[不执行任务] 这个任务不应该被执行！" + LocalDateTime.now());
    // }
}
