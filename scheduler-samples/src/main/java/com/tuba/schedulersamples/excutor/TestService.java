package com.tuba.schedulersamples.excutor;

import org.springframework.stereotype.Service;

/**
 * 测试服务类，用于演示依赖注入
 */
@Service
public class TestService {
    
    public String getMessage() {
        return "Hello from TestService!";
    }
}
