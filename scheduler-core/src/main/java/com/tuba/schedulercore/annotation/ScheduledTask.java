package com.tuba.schedulercore.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ScheduledTask {
    String name() default "";
    
    /**
     * Cron 表达式，与其他时间配置属性互斥
     */
    String cron() default "";
    
    /**
     * 固定频率，单位毫秒
     */
    long fixedRate() default -1;
    
    /**
     * 固定延迟，单位毫秒
     */
    long fixedDelay() default -1;
    
    /**
     * 秒级配置，例如：
     * - "5" 表示每 5 秒执行一次
     * - "0,15,30,45" 表示每分钟的 0、15、30、45 秒执行
     */
    String seconds() default "";
    
    /**
     * 分钟级配置，例如：
     * - "10" 表示每 10 分钟执行一次
     * - "0,30" 表示每小时的 0、30 分钟执行
     */
    String minutes() default "";
    
    /**
     * 小时级配置，例如：
     * - "2" 表示每 2 小时执行一次
     * - "9,18" 表示每天的 9 点和 18 点执行
     */
    String hours() default "";
    
    /**
     * 天级配置，例如：
     * - "1" 表示每天执行一次
     * - "1,15" 表示每月的 1 号和 15 号执行
     */
    String days() default "";
    
    boolean async() default true;
    String group() default "default";
    String description() default "";
    
    /**
     * 循环次数，默认-1表示无限循环
     * -1: 无限循环
     * 0: 不执行
     * >0: 执行指定次数后停止
     */
    int repeatCount() default -1;
}