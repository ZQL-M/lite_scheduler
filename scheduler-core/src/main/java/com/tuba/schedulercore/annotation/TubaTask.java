package com.tuba.schedulercore.annotation;

import com.tuba.schedulercore.enums.TimeUnit;

import java.lang.annotation.Target;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TubaTask {
    /**
     * 任务名称
     */
    String name() default "";

    /**
     * Cron表达式，与interval互斥
     */
    String cron() default "";

    /**
     * 时间间隔
     */
    long interval() default -1;

    /**
     * 时间单位，默认毫秒
     */
    TimeUnit type() default TimeUnit.MILLISECONDS;

    /**
     * 是否异步执行，默认是
     */
    boolean async() default false;

    /**
     * 任务分组
     */
    String group() default "default";

    /**
     * 任务描述
     */
    String description() default "";

    /**
     * 循环次数，默认-1表示无限循环
     * -1: 无限循环
     * 0: 不执行
     * >0: 执行指定次数后停止
     */
    int repeatCount() default -1;
}