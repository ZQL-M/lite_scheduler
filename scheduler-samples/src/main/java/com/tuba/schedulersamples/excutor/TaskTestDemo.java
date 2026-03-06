package com.tuba.schedulersamples.excutor;

import com.tuba.schedulercore.model.TaskDefinition;
import com.tuba.schedulercore.service.TaskSchedulerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 测试主类，演示三个功能：
 * 1. 任务实例依赖注入
 * 2. 任务执行超时控制
 * 3. 任务失败重试机制
 */
@Component
public class TaskTestDemo implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TaskTestDemo.class);

    @Autowired
    private TaskSchedulerService taskSchedulerService;

    @Autowired
    private DependencyInjectionTestTask dependencyInjectionTestTask; // 注入 Spring 管理的任务实例

    @Override
    public void run(ApplicationArguments args) {
        log.info("=================================================================================");
        log.info("=== 开始测试任务执行器功能 ===");
        log.info("=================================================================================");
        
        // 等待一段时间，让其他组件初始化完成
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 测试 1: 任务实例依赖注入
        testDependencyInjection();
        
        // 等待一段时间
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 测试 2: 任务执行超时控制
        testTimeoutControl();
        
        // 等待一段时间
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 测试 3: 任务失败重试机制
        testRetryMechanism();
        
        log.info("=================================================================================");
        log.info("=== 任务执行器功能测试完成 ===");
        log.info("=================================================================================");
    }

    /**
     * 测试任务实例依赖注入
     */
    private void testDependencyInjection() {
        log.info("--- 测试 1: 任务实例依赖注入 ---");
        
        // 创建任务定义
        TaskDefinition definition = new TaskDefinition();
        definition.setId("test-dependency-injection");
        definition.setName("Dependency Injection Test");
        definition.setAsync(false);
        
        try {
            // 注册任务（使用 Spring 管理的任务实例）
            String taskId = taskSchedulerService.registerTask(dependencyInjectionTestTask, definition);
            log.info("✅ 注册依赖注入测试任务成功，任务 ID: {}", taskId);
            
            // 立即触发任务执行
            taskSchedulerService.triggerTask(taskId);
            log.info("✅ 已触发依赖注入测试任务执行");
        } catch (Exception e) {
            log.error("❌ 依赖注入测试失败", e);
        }
    }

    /**
     * 测试任务执行超时控制
     */
    private void testTimeoutControl() {
        log.info("--- 测试 2: 任务执行超时控制 ---");
        
        // 创建任务定义
        TaskDefinition definition = new TaskDefinition();
        definition.setId("test-timeout-control");
        definition.setName("Timeout Control Test");
        definition.setAsync(false);
        
        try {
            // 注册任务（设置 3 秒超时）
            String taskId = taskSchedulerService.registerTask(
                    new TimeoutTestTask(), 
                    definition,
                    3000L, // 超时时间：3 秒
                    0,      // 重试次数：0 次
                    1000L   // 重试间隔：1 秒
            );
            log.info("✅ 注册超时测试任务成功，任务 ID: {}", taskId);
            
            // 立即触发任务执行
            taskSchedulerService.triggerTask(taskId);
            log.info("✅ 已触发超时测试任务执行，预期 3 秒后超时");
        } catch (Exception e) {
            log.error("❌ 超时控制测试失败", e);
        }
    }

    /**
     * 测试任务失败重试机制
     */
    private void testRetryMechanism() {
        log.info("--- 测试 3: 任务失败重试机制 ---");
        
        // 创建任务定义
        TaskDefinition definition = new TaskDefinition();
        definition.setId("test-retry-mechanism");
        definition.setName("Retry Mechanism Test");
        definition.setAsync(false);
        
        try {
            // 注册任务（设置 3 次重试）
            String taskId = taskSchedulerService.registerTask(
                    new RetryTestTask(), 
                    definition,
                    10000L, // 超时时间：10 秒
                    3,      // 重试次数：3 次
                    2000L   // 重试间隔：2 秒
            );
            log.info("✅ 注册重试测试任务成功，任务 ID: {}", taskId);
            
            // 立即触发任务执行
            taskSchedulerService.triggerTask(taskId);
            log.info("✅ 已触发重试测试任务执行，预期失败 2 次后第 3 次成功");
        } catch (Exception e) {
            log.error("❌ 重试机制测试失败", e);
        }
    }
}
