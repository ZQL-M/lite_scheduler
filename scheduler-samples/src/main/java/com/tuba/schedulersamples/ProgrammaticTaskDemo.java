package com.tuba.schedulersamples;

import com.tuba.schedulercore.enums.TimeUnit;
import com.tuba.schedulercore.model.TaskContext;
import com.tuba.schedulercore.service.TaskSchedulerService;
import com.tuba.schedulercore.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.Callable;

/**
 * 编程式任务注册演示
 */
@Component
public class ProgrammaticTaskDemo implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProgrammaticTaskDemo.class);

    @Autowired
    private TaskSchedulerService taskSchedulerService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("开始演示编程式任务注册...");

        // 演示1：通过Task接口创建任务
        demoTaskInterface();

        // 演示2：通过Runnable创建任务
        demoRunnableTask();

        // 演示3：通过Callable创建任务
        demoCallableTask();

        log.info("编程式任务注册演示完成");
    }

    /**
     * 演示通过Task接口创建任务
     */
    private void demoTaskInterface() {
        // 创建Task接口实现类
        Task task = new Task() {
            @Override
            public void execute(TaskContext context) {
                log.info("[编程方式-Task接口] 执行时间: {}, 线程: {}", LocalDateTime.now(), Thread.currentThread().getName());
                System.out.println("[编程方式-Task接口] 执行时间: " + LocalDateTime.now() + ", 线程: " + Thread.currentThread().getName());
            }
        };

        // 注册任务，每5秒执行一次
        String taskId = taskSchedulerService.registerTask(task, 5, TimeUnit.SECONDS);
        log.info("成功注册Task接口任务，ID: {}", taskId);
    }

    /**
     * 演示通过Runnable创建任务
     */
    private void demoRunnableTask() {
        // 创建Runnable任务
        Runnable runnable = () -> {
            log.info("[编程方式-Runnable] 执行时间: {}, 线程: {}", LocalDateTime.now(), Thread.currentThread().getName());
            System.out.println("[编程方式-Runnable] 执行时间: " + LocalDateTime.now() + ", 线程: " + Thread.currentThread().getName());
        };

        // 注册任务，每8秒执行一次
        String taskId = taskSchedulerService.registerTask(runnable, 8, TimeUnit.SECONDS);
        log.info("成功注册Runnable任务，ID: {}", taskId);
    }

    /**
     * 演示通过Callable创建任务
     */
    private void demoCallableTask() {
        // 创建Callable任务
        Callable<String> callable = () -> {
            String result = "[编程方式-Callable] 执行时间: " + LocalDateTime.now() + ", 线程: " + Thread.currentThread().getName();
            log.info(result);
            System.out.println(result);
            return "Task executed successfully";
        };

        // 使用cron表达式注册任务，每15秒执行一次
        String taskId = taskSchedulerService.registerTask(callable, "0/15 * * * * ?");
        log.info("成功注册Callable任务，ID: {}", taskId);
    }
}