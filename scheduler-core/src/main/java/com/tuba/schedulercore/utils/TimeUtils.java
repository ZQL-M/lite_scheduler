package com.tuba.schedulercore.utils;

import com.tuba.schedulercore.enums.TimeUnit;

/**
 * 时间工具类，提供时间相关的工具方法
 */
public class TimeUtils {
    
    /**
     * 将时间间隔转换为毫秒
     * 
     * @param interval 时间间隔
     * @param timeUnit 时间单位
     * @return 毫秒数
     */
    public static long convertToMillis(long interval, TimeUnit timeUnit) {
        switch (timeUnit) {
            case SECONDS:
                return interval * 1000;
            case MINUTES:
                return interval * 1000 * 60;
            case HOURS:
                return interval * 1000 * 60 * 60;
            case DAYS:
                return interval * 1000 * 60 * 60 * 24;
            case MILLISECONDS:
            default:
                return interval;
        }
    }
}