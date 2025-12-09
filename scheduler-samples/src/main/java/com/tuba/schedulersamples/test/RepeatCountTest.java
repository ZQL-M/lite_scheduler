package com.tuba.schedulersamples.test;

import com.tuba.schedulercore.annotation.ScheduledTask;
import org.springframework.stereotype.Component;

/**
 * 重复次数测试类
 * 注意：此类已被禁用，仅用于参考
 */
// @Component
public class RepeatCountTest {
    
    // 测试无限循环（默认-1）
    // @ScheduledTask(
    //     name = "无限循环任务",
    //     seconds = "2",
    //     description = "每2秒执行一次，无限循环"
    // )
    public void infiniteLoopTask() {
        System.out.println("[无限循环任务] 执行时间: " + java.time.LocalDateTime.now());
    }
    
    // 测试执行5次后停止
    // @ScheduledTask(
    //     name = "有限循环任务",
    //     seconds = "1",
    //     repeatCount = 5,
    //     description = "每1秒执行一次，执行5次后停止"
    // )
    public void finiteLoopTask() {
        System.out.println("[有限循环任务] 执行时间: " + java.time.LocalDateTime.now());
    }
    
    // 测试执行0次（不执行）
    // @ScheduledTask(
    //     name = "不执行任务",
    //     seconds = "1",
    //     repeatCount = 0,
    //     description = "设置为0次，不执行"
    // )
    public void noExecuteTask() {
        System.out.println("[不执行任务] 执行时间: " + java.time.LocalDateTime.now());
    }
}
