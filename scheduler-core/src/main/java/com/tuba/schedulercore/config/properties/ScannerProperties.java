package com.tuba.schedulercore.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 任务扫描器配置属性
 */
@ConfigurationProperties(prefix = "lite-scheduler.scanner")
public class ScannerProperties {
    /**
     * 扫描频率（毫秒）
     */
    private long scanInterval = 5000;

    /**
     * 扫描提前时间（毫秒）
     */
    private long scanAheadTime = 30000;

    /**
     * 单次扫描最大任务数量
     */
    private int maxTasksPerScan = 100;

    public long getScanInterval() {
        return scanInterval;
    }

    public void setScanInterval(long scanInterval) {
        this.scanInterval = scanInterval > 0 ? scanInterval : 5000;
    }

    public long getScanAheadTime() {
        return scanAheadTime;
    }

    public void setScanAheadTime(long scanAheadTime) {
        this.scanAheadTime = scanAheadTime > 0 ? scanAheadTime : 30000;
    }

    public int getMaxTasksPerScan() {
        return maxTasksPerScan;
    }

    public void setMaxTasksPerScan(int maxTasksPerScan) {
        this.maxTasksPerScan = maxTasksPerScan > 0 ? maxTasksPerScan : 100;
    }
}