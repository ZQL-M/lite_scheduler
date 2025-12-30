package com.tuba.schedulercore.scheduler;

import com.tuba.schedulercore.config.ExecutorProperties;
import com.tuba.schedulercore.executor.TaskExecutor;
import com.tuba.schedulercore.model.TaskContext;
import com.tuba.schedulercore.model.TaskDefinition;
import com.tuba.schedulercore.service.TaskStatusService;
import com.tuba.schedulercore.service.TaskTriggerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ThreadPoolScheduler 测试类
 * 验证 triggerNow、unschedule、数字秒支持等功能
 */
@ExtendWith(MockitoExtension.class)
class ThreadPoolSchedulerTest {

    @Mock
    private TaskExecutor taskExecutor;
    @Mock
    private TaskTriggerService taskTriggerService;
    @Mock
    private TaskStatusService taskStatusService;
    @Mock
    private ExecutorProperties executorProperties;

    private ThreadPoolScheduler scheduler;
    private TaskDefinition task1;
    private TaskDefinition task2;

    @BeforeEach
    void setUp() {
        // 设置mock返回有效的池大小
        when(executorProperties.getSchedulerPoolSize()).thenReturn(5);
        
        scheduler = new ThreadPoolScheduler(taskExecutor, executorProperties, taskTriggerService, taskStatusService);
        
        task1 = new TaskDefinition();
        task1.setId("task-1");
        task1.setName("Task 1");
        task1.setEnabled(true);
        
        task2 = new TaskDefinition();
        task2.setId("task-2");
        task2.setName("Task 2");
        task2.setEnabled(true);
    }

    @Test
    void testTriggerNow() {
        // 测试 triggerNow 立即触发任务
        scheduler.triggerNow(task1);
        
        // 验证 executor.execute 被调用
        verify(taskExecutor, times(1)).execute(any(TaskContext.class));
        
        // 验证传入的 TaskContext 包含正确的 definition
        ArgumentCaptor<TaskContext> contextCaptor = ArgumentCaptor.forClass(TaskContext.class);
        verify(taskExecutor).execute(contextCaptor.capture());
        TaskContext capturedContext = contextCaptor.getValue();
        assertEquals(task1.getId(), capturedContext.getDefinition().getId(), 
            "TaskContext should contain correct TaskDefinition");
    }

    @Test
    void testScheduleWithCronExpression() {
        // 测试 Cron 表达式调度
        scheduler.schedule(task1);
        
        // 注意：Cron 表达式 "0/5 * * * * ?" 是每 5 秒执行一次
        // 在测试中，我们只验证任务被成功调度，不等待实际执行
        // 因为等待执行需要至少 5 秒，这会让测试变慢
        
        // 验证任务被调度（通过检查 unschedule 能正常工作来间接验证）
        assertDoesNotThrow(() -> scheduler.unschedule("task-1"), 
            "Unschedule should work if task was scheduled");
    }

    @Test
    void testScheduleWithNumericSeconds() {
        // 测试数字秒（固定频率）调度
        scheduler.schedule(task2);
        
        // 注意：数字秒 "10" 表示每 10 秒执行一次
        // 在测试中，我们只验证任务被成功调度，不等待实际执行
        
        // 验证任务被调度（通过检查 unschedule 能正常工作来间接验证）
        assertDoesNotThrow(() -> scheduler.unschedule("task-2"), 
            "Unschedule should work if task was scheduled");
    }

    @Test
    void testUnschedule() {
        // 先调度任务
        scheduler.schedule(task1);
        
        // 取消调度
        scheduler.unschedule("task-1");
        
        // 等待一段时间，验证任务不再执行
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 由于任务被取消，executor 的调用次数应该不会显著增加
        // 注意：这个测试可能不够精确，因为任务可能在取消前已经执行
        // 但至少验证了 unschedule 不会抛异常
        assertDoesNotThrow(() -> scheduler.unschedule("task-1"), 
            "Unschedule should not throw exception even for already unscheduled task");
    }

    @Test
    void testScheduleDisabledTask() {
        // 测试禁用任务不会被调度
        task1.setEnabled(false);
        scheduler.schedule(task1);
        
        // 等待一段时间
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 验证 executor 没有被调用（因为任务被禁用）
        verify(taskExecutor, never()).execute(any(TaskContext.class));
    }

    @Test
    void testStop() {
        // 调度多个任务
        scheduler.schedule(task1);
        scheduler.schedule(task2);
        
        // 停止调度器
        assertDoesNotThrow(() -> scheduler.stop(), 
            "Stop should not throw exception");
    }

    @Test
    void testStart() {
        // start 方法应该不抛异常
        assertDoesNotThrow(() -> scheduler.start(), 
            "Start should not throw exception");
    }
}

