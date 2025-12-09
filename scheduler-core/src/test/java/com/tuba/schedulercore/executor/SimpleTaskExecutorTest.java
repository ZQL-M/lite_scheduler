package com.tuba.schedulercore.executor;

import com.tuba.schedulercore.model.TaskContext;
import com.tuba.schedulercore.model.TaskDefinition;
import com.tuba.schedulercore.plugin.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SimpleTaskExecutor 测试类
 * 验证 TaskContext 正确传递
 */
@ExtendWith(MockitoExtension.class)
class SimpleTaskExecutorTest {

    @Mock
    private PluginManager pluginManager;

    private SimpleTaskExecutor executor;
    private TaskDefinition taskDef;
    private TestTaskBean testBean;

    @BeforeEach
    void setUp() {
        executor = new SimpleTaskExecutor(pluginManager, null);  // null 使用默认配置
        testBean = new TestTaskBean();
        
        taskDef = new TaskDefinition();
        taskDef.setId("test-task");
        taskDef.setName("Test Task");
        taskDef.setAsync(false);  // 同步执行以便测试
    }

    @Test
    void testExecuteWithNoParameterMethod() throws Exception {
        // 测试无参方法
        Method method = TestTaskBean.class.getMethod("noParameterMethod");
        taskDef.setMethod(method);
        taskDef.setBean(testBean);
        
        TaskContext context = new TaskContext(taskDef);
        executor.execute(context);
        
        // 验证方法被调用
        assertTrue(testBean.noParamCalled, "No-parameter method should be called");
        
        // 验证插件被调用
        verify(pluginManager, times(1)).before(any(TaskContext.class));
        verify(pluginManager, times(1)).after(any(TaskContext.class));
        verify(pluginManager, never()).onError(any(TaskContext.class), any(Throwable.class));
    }

    @Test
    void testExecuteWithTaskContextParameter() throws Exception {
        // 测试带 TaskContext 参数的方法
        Method method = TestTaskBean.class.getMethod("withContextMethod", TaskContext.class);
        taskDef.setMethod(method);
        taskDef.setBean(testBean);
        
        TaskContext context = new TaskContext(taskDef);
        context.putAttribute("test-key", "test-value");
        
        executor.execute(context);
        
        // 验证方法被调用，并且接收到了正确的 TaskContext
        assertTrue(testBean.contextCalled, "Context-parameter method should be called");
        assertNotNull(testBean.receivedContext, "Method should receive TaskContext");
        assertEquals("test-value", testBean.receivedContext.getAttribute("test-key"), 
            "TaskContext should preserve attributes");
        
        // 验证插件被调用
        verify(pluginManager, times(1)).before(any(TaskContext.class));
        verify(pluginManager, times(1)).after(any(TaskContext.class));
    }

    @Test
    void testExecuteWithException() throws Exception {
        // 测试方法抛出异常的情况
        Method method = TestTaskBean.class.getMethod("exceptionMethod");
        taskDef.setMethod(method);
        taskDef.setBean(testBean);
        
        TaskContext context = new TaskContext(taskDef);
        executor.execute(context);
        
        // 验证异常被捕获并传递给插件
        verify(pluginManager, times(1)).before(any(TaskContext.class));
        verify(pluginManager, times(1)).onError(any(TaskContext.class), any(Throwable.class));
        verify(pluginManager, never()).after(any(TaskContext.class));
        
        // 验证 context 包含错误信息
        assertNotNull(context.getError(), "Context should contain error");
        assertEquals("Test exception", context.getError().getMessage(), 
            "Context should contain correct error message");
    }

    @Test
    void testExecuteAsync() throws Exception {
        // 测试异步执行
        taskDef.setAsync(true);
        Method method = TestTaskBean.class.getMethod("noParameterMethod");
        taskDef.setMethod(method);
        taskDef.setBean(testBean);
        
        TaskContext context = new TaskContext(taskDef);
        executor.execute(context);
        
        // 等待异步执行完成
        Thread.sleep(100);
        
        // 验证方法被调用
        assertTrue(testBean.noParamCalled, "Method should be called asynchronously");
    }

    // 测试用的 Bean 类
    static class TestTaskBean {
        boolean noParamCalled = false;
        boolean contextCalled = false;
        TaskContext receivedContext = null;

        public void noParameterMethod() {
            noParamCalled = true;
        }

        public void withContextMethod(TaskContext context) {
            contextCalled = true;
            receivedContext = context;
        }

        public void exceptionMethod() {
            throw new RuntimeException("Test exception");
        }
    }
}

