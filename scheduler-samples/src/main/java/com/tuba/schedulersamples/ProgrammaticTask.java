package com.tuba.schedulersamples;

import com.tuba.schedulercore.model.TaskContext;
import com.tuba.schedulercore.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 编程式任务示例
 */
public class ProgrammaticTask implements Task {
    
    private static final Logger log = LoggerFactory.getLogger(ProgrammaticTask.class);
    
    private final String taskName;
    
    public ProgrammaticTask() {
        this.taskName = "DefaultProgrammaticTask";
    }
    
    public ProgrammaticTask(String taskName) {
        this.taskName = taskName;
    }
    
    @Override
    public void execute(TaskContext context) {
        log.info("[{}] 执行时间: {}，线程: {}", 
                taskName, 
                java.time.LocalDateTime.now(),
                Thread.currentThread().getName());
    }
}
