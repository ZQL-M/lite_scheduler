package com.tuba.schedulersamples;

import com.tuba.schedulercore.annotation.TubaTask;
import com.tuba.schedulercore.enums.TimeUnit;
import com.tuba.schedulercore.model.TaskContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 注解方式任务演示
 * 演示通过TubaTask注解创建持久化任务
 */
// @Component
public class AnnotationTaskDemo {

    private static final Logger log = LoggerFactory.getLogger(AnnotationTaskDemo.class);

    /**
     * 一分钟后执行的持久化任务
     * 使用最简约的注解写法
     */
    @TubaTask(
        name = "MinuteLaterTask",
        // 设置cron表达式：当前时间的秒和分，后续固定时间执行一次
        // 这里使用固定速率，1分钟后执行一次（repeatCount=1表示只执行一次）
        interval = 1, // 1分钟执行一次
        type = TimeUnit.MINUTES,
        repeatCount = 1,
        persistent = true // 持久化到数据库
    )
    public void executeMinuteLaterTask(TaskContext context) {
        // 醒目输出
        log.info("=========================================");
        log.info("[注解任务] >>> 任务开始执行 <<<");
        log.info("[注解任务] 执行时间: {}", LocalDateTime.now());
        log.info("[注解任务] 线程名称: {}", Thread.currentThread().getName());
        log.info("[注解任务] 任务ID: {}", context.getDefinition().getId());
        log.info("[注解任务] 任务名称: {}", context.getDefinition().getName());
        log.info("[注解任务] >>> 任务执行完成 <<<");
        log.info("=========================================");
        
        // 控制台醒目输出
        System.out.println("=========================================");
        System.out.println("[注解任务] >>> 任务开始执行 <<<");
        System.out.println("[注解任务] 执行时间: " + LocalDateTime.now());
        System.out.println("[注解任务] 线程名称: " + Thread.currentThread().getName());
        System.out.println("[注解任务] 任务ID: " + context.getDefinition().getId());
        System.out.println("[注解任务] 任务名称: " + context.getDefinition().getName());
        System.out.println("[注解任务] >>> 任务执行完成 <<<");
        System.out.println("=========================================");
    }
}
