# Lite-Scheduler Core 模块详细讲解

## 📋 目录

1. [模块概述](#模块概述)
2. [架构设计](#架构设计)
3. [核心组件详解](#核心组件详解)
4. [执行流程](#执行流程)
5. [设计模式](#设计模式)
6. [使用示例](#使用示例)
7. [配置说明](#配置说明)
8. [扩展点](#扩展点)

---

## 📦 模块概述

### 模块定位

**scheduler-core** 是 Lite-Scheduler 的核心模块，提供了完整的任务调度框架基础能力。它不依赖 Spring（但提供了 Spring 集成），可以独立运行，也可以作为 Starter 引入到 Spring Boot 项目中。

### 核心特性

- ✅ **声明式任务定义**：通过 `@ScheduledTask` 注解定义任务
- ✅ **自动扫描注册**：启动时自动扫描并注册所有任务
- ✅ **多种调度模式**：支持 Cron 表达式和固定频率调度
- ✅ **插件化架构**：可扩展的插件机制
- ✅ **异步/同步执行**：支持任务异步或同步执行
- ✅ **完整的执行上下文**：提供任务执行过程中的所有信息
- ✅ **优雅关闭**：应用关闭时优雅停止所有任务

### 模块结构

```
scheduler-core/
├── annotation/          # 注解层
│   └── ScheduledTask.java
├── model/               # 模型层
│   ├── TaskDefinition.java
│   ├── TaskContext.java
│   └── TaskLog.java
├── plugin/             # 插件系统
│   ├── TaskPlugin.java
│   └── PluginManager.java
├── executor/           # 执行器
│   ├── TaskExecutor.java
│   └── SimpleTaskExecutor.java
├── scheduler/          # 调度器
│   ├── Scheduler.java
│   └── ThreadPoolScheduler.java
├── registry/           # 注册器
│   ├── TaskRegistry.java
│   └── TaskRegistrar.java
└── config/            # 配置
    ├── ExecutorProperties.java
    └── ExecutorConfiguration.java
```

---

## 🏗️ 架构设计

### 整体架构图

```
┌─────────────────────────────────────────────────────────┐
│                    Application Layer                     │
│  @ScheduledTask 注解的方法（用户业务代码）                │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                  Registry Layer                          │
│  ┌──────────────┐         ┌──────────────┐              │
│  │TaskRegistrar │────────▶│TaskRegistry  │              │
│  │(扫描注册)     │         │(任务注册表)   │              │
│  └──────────────┘         └──────────────┘              │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                 Scheduler Layer                         │
│  ┌──────────────────────────────────────────┐           │
│  │  ThreadPoolScheduler                      │           │
│  │  - schedule() 调度任务                    │           │
│  │  - unschedule() 取消调度                  │           │
│  │  - triggerNow() 立即触发                  │           │
│  └──────────────────┬───────────────────────┘           │
└─────────────────────┼───────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────┐
│                  Executor Layer                        │
│  ┌──────────────────────────────────────────┐          │
│  │  SimpleTaskExecutor                       │          │
│  │  - execute() 执行任务                     │          │
│  │  - 插件链调用                             │          │
│  │  - 异常处理                               │          │
│  └──────────────────┬───────────────────────┘          │
└─────────────────────┼───────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────┐
│                  Plugin Layer                          │
│  ┌──────────────────────────────────────────┐          │
│  │  PluginManager                            │          │
│  │  - before() 执行前                       │          │
│  │  - after() 执行后                        │          │
│  │  - onError() 错误处理                     │          │
│  └──────────────────────────────────────────┘          │
└─────────────────────────────────────────────────────────┘
```

### 设计原则

1. **单一职责原则**：每个组件只负责一个功能
2. **开闭原则**：对扩展开放，对修改关闭（插件机制）
3. **依赖倒置**：依赖接口而非实现
4. **接口隔离**：接口设计精简，职责清晰

---

## 🔧 核心组件详解

### 1. 注解层（Annotation Layer）

#### `@ScheduledTask`

**位置：** `com.tuba.schedulercore.annotation.ScheduledTask`

**作用：** 声明式定义定时任务

**字段说明：**

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `name` | String | "" | 任务名称，为空时使用方法名 |
| `cron` | String | **必填** | Cron 表达式或数字秒（如 "5" 表示每 5 秒） |
| `async` | boolean | true | 是否异步执行 |
| `group` | String | "default" | 任务分组 |
| `description` | String | "" | 任务描述 |

**使用示例：**
```java
@Component
public class MyTask {
    @ScheduledTask(
        name = "数据同步任务",
        cron = "0 0 2 * * ?",  // 每天凌晨 2 点
        async = true,
        group = "sync",
        description = "同步数据库数据"
    )
    public void syncData() {
        // 任务逻辑
    }
    
    @ScheduledTask(cron = "10")  // 每 10 秒执行一次
    public void quickTask() {
        // 快速任务
    }
}
```

**支持的 Cron 格式：**
- 标准 Cron：`0 0 2 * * ?`（每天凌晨 2 点）
- 数字秒：`"5"`（每 5 秒执行一次，固定频率）

---

### 2. 模型层（Model Layer）

#### 2.1 `TaskDefinition` - 任务定义

**位置：** `com.tuba.schedulercore.model.TaskDefinition`

**作用：** 封装任务的元数据和执行信息

**核心字段：**

```java
public class TaskDefinition {
    private String id;              // 任务唯一标识（UUID）
    private String name;             // 任务名称
    private String group;            // 任务分组
    private String cron;              // Cron 表达式或数字秒
    private boolean async;           // 是否异步执行
    private Object bean;             // Spring Bean 实例
    private Method method;           // 要执行的方法
    private String description;      // 任务描述
    private boolean enabled;         // 是否启用（默认 true）
    private List<TaskPlugin> plugins; // 任务级插件（可选）
}
```

**关键特性：**
- ✅ 基于 `id` 的 `equals()` 和 `hashCode()`，可用于 Set/Map
- ✅ 支持动态启用/禁用（`enabled` 字段）
- ✅ 保存 Bean 和方法引用，用于反射调用

**使用场景：**
- 任务注册时创建
- 任务调度时使用
- 任务管理时查询和修改

---

#### 2.2 `TaskContext` - 任务执行上下文

**位置：** `com.tuba.schedulercore.model.TaskContext`

**作用：** 封装任务执行过程中的所有信息

**核心字段：**

```java
public class TaskContext {
    private TaskDefinition definition;  // 任务定义
    private Instant startTime;          // 开始时间
    private Instant endTime;            // 结束时间
    private Map<String, Object> attributes; // 自定义属性
    private Throwable error;            // 异常信息（如有）
}
```

**关键方法：**

```java
// 设置属性
void putAttribute(String key, Object value)

// 获取属性
Object getAttribute(String key)

// 获取属性（类型安全）
<T> T getAttribute(String key, Class<T> clazz)
```

**使用场景：**
- 插件之间传递数据
- 任务方法接收上下文（可选参数）
- 记录执行状态和异常信息

**示例：**
```java
@ScheduledTask(cron = "5")
public void taskWithContext(TaskContext context) {
    // 获取开始时间
    Instant start = context.getStartTime();
    
    // 设置自定义属性
    context.putAttribute("customKey", "customValue");
    
    // 从属性中获取值
    String value = context.getAttribute("customKey", String.class);
}
```

---

#### 2.3 `TaskLog` - 任务执行日志

**位置：** `com.tuba.schedulercore.model.TaskLog`

**作用：** 记录任务执行的历史信息

**核心字段：**

```java
public class TaskLog {
    private String id;           // 日志ID
    private String taskId;       // 任务ID
    private Instant startTime;    // 开始时间
    private Instant endTime;      // 结束时间
    private String status;        // 状态：SUCCESS / FAIL
    private String message;       // 消息（错误信息等）
    private Long durationMs;     // 执行时长（毫秒）
}
```

**使用场景：**
- 任务执行历史记录
- 性能监控
- 错误追踪

**注意：** 当前版本日志创建后暂未持久化，后续可通过插件实现持久化。

---

### 3. 插件系统（Plugin System）

#### 3.1 `TaskPlugin` - 插件接口

**位置：** `com.tuba.schedulercore.plugin.TaskPlugin`

**作用：** 定义插件生命周期回调

**接口定义：**

```java
public interface TaskPlugin {
    default void before(TaskContext context) {}
    default void after(TaskContext context) {}
    default void onError(TaskContext context, Throwable e) {}
}
```

**设计特点：**
- ✅ 所有方法都是 `default`，插件只需实现需要的方法
- ✅ 插件异常不会中断任务执行流程
- ✅ 支持多个插件，按顺序执行

**执行顺序：**
```
任务执行流程：
1. PluginManager.before() → 所有插件的 before()
2. 执行任务方法
3. PluginManager.after() → 所有插件的 after()
   或
   PluginManager.onError() → 所有插件的 onError()（如果出错）
```

---

#### 3.2 `PluginManager` - 插件管理器

**位置：** `com.tuba.schedulercore.plugin.PluginManager`

**作用：** 管理所有插件，统一调用

**核心功能：**
- 自动注入所有 `TaskPlugin` 实现
- 按顺序调用插件方法
- 捕获插件异常，记录日志但不中断流程

**实现细节：**

```java
@Component
public class PluginManager {
    private final List<TaskPlugin> plugins;
    
    public void before(TaskContext ctx) {
        for (TaskPlugin p : plugins) {
            try {
                p.before(ctx);
            } catch (Throwable t) {
                log.error("Plugin {} failed in before()", p.getClass().getName(), t);
            }
        }
    }
    // after() 和 onError() 类似
}
```

**插件注册：**
- 只需实现 `TaskPlugin` 接口并标注 `@Component`
- Spring 会自动注入到 `PluginManager`

---

### 4. 执行器（Executor）

#### 4.1 `TaskExecutor` - 执行器接口

**位置：** `com.tuba.schedulercore.executor.TaskExecutor`

**作用：** 定义任务执行的标准接口

**接口定义：**

```java
public interface TaskExecutor {
    void execute(TaskContext context);
}
```

**职责：**
- 接收 `TaskContext`
- 执行任务方法
- 处理异常
- 调用插件链

---

#### 4.2 `SimpleTaskExecutor` - 简单执行器实现

**位置：** `com.tuba.schedulercore.executor.SimpleTaskExecutor`

**作用：** 实现任务执行的核心逻辑

**核心功能：**

1. **同步/异步执行**
   ```java
   if (def.isAsync()) {
       executor.submit(() -> invokeAndHandle(context));
   } else {
       invokeAndHandle(context);
   }
   ```

2. **执行流程**
   ```
   invokeAndHandle():
   1. 设置开始时间
   2. 调用 PluginManager.before()
   3. 反射调用任务方法
   4. 设置结束时间
   5. 调用 PluginManager.after() 或 onError()
   6. 创建 TaskLog（待持久化）
   ```

3. **方法调用支持**
   - 无参方法：`method.invoke(bean)`
   - 单参数 TaskContext：`method.invoke(bean, context)`

4. **异常处理**
   - 提取 `InvocationTargetException` 的真正异常
   - 设置到 `TaskContext.error`
   - 调用插件错误处理

**线程池配置：**
- 默认大小：CPU 核心数
- 可通过 `ExecutorProperties` 配置
- 优雅关闭：先 shutdown，等待 30 秒，再 shutdownNow

---

### 5. 调度器（Scheduler）

#### 5.1 `Scheduler` - 调度器接口

**位置：** `com.tuba.schedulercore.scheduler.Scheduler`

**作用：** 定义任务调度的标准接口

**接口方法：**

```java
public interface Scheduler {
    void schedule(TaskDefinition definition);    // 调度任务
    void unschedule(String taskId);               // 取消调度
    void start();                                 // 启动调度器
    void stop();                                  // 停止调度器
    void triggerNow(TaskDefinition definition);  // 立即触发
}
```

---

#### 5.2 `ThreadPoolScheduler` - 线程池调度器实现

**位置：** `com.tuba.schedulercore.scheduler.ThreadPoolScheduler`

**作用：** 基于 Spring `ThreadPoolTaskScheduler` 实现任务调度

**核心功能：**

1. **调度模式支持**
   - **Cron 表达式**：使用 `CronTrigger`
     ```java
     Trigger trigger = new CronTrigger("0 0 2 * * ?");
     ```
   - **数字秒（固定频率）**：使用 `PeriodicTrigger`
     ```java
     PeriodicTrigger trigger = new PeriodicTrigger(seconds * 1000);
     trigger.setFixedRate(true);  // 固定频率
     ```

2. **任务管理**
   - 保存 `ScheduledFuture<?>` 以便取消
   - 支持动态启用/禁用任务
   - 支持立即触发（不加入调度计划）

3. **优雅关闭**
   - 取消所有已调度的任务
   - 等待线程池关闭
   - 实现 `DisposableBean` 接口

**调度逻辑：**

```java
@Override
public void schedule(TaskDefinition definition) {
    if (!definition.isEnabled()) {
        return;  // 禁用任务不调度
    }
    
    Runnable runnable = () -> {
        TaskContext ctx = new TaskContext(definition);
        taskExecutor.execute(ctx);
    };
    
    Trigger trigger = parseTrigger(definition.getCron());
    ScheduledFuture<?> future = taskScheduler.schedule(runnable, trigger);
    scheduledFutures.put(definition.getId(), future);
}
```

---

### 6. 注册器（Registry）

#### 6.1 `TaskRegistry` - 任务注册表

**位置：** `com.tuba.schedulercore.registry.TaskRegistry`

**作用：** 管理所有已注册的任务定义

**核心功能：**

```java
public class TaskRegistry {
    // 注册任务
    void register(TaskDefinition def)
    
    // 查询任务
    TaskDefinition get(String id)
    Collection<TaskDefinition> getAll()
    boolean contains(String id)
    
    // 管理任务
    void unregister(String id)
    void enable(String id)    // 启用任务
    void disable(String id)    // 禁用任务
}
```

**实现特点：**
- 使用 `ConcurrentHashMap` 保证线程安全
- 支持动态启用/禁用任务
- 提供查询接口供管理端使用

---

#### 6.2 `TaskRegistrar` - 任务注册器

**位置：** `com.tuba.schedulercore.registry.TaskRegistrar`

**作用：** 启动时自动扫描并注册所有 `@ScheduledTask` 方法

**核心流程：**

```
1. 监听 ContextRefreshedEvent（Spring 上下文刷新完成）
2. 遍历所有 Spring Bean
3. 使用 AopUtils.getTargetClass() 获取原始类（处理 AOP 代理）
4. 扫描类的所有方法，查找 @ScheduledTask 注解
5. 验证方法签名（无参或单参数 TaskContext）
6. 创建 TaskDefinition
7. 注册到 TaskRegistry
8. 调用 Scheduler.schedule() 开始调度
```

**关键代码：**

```java
@EventListener(ContextRefreshedEvent.class)
public void onApplicationReady() {
    String[] beanNames = applicationContext.getBeanDefinitionNames();
    for (String beanName : beanNames) {
        Object bean = applicationContext.getBean(beanName);
        Class<?> targetClass = AopUtils.getTargetClass(bean);  // 处理代理
        Method[] methods = targetClass.getDeclaredMethods();
        for (Method m : methods) {
            ScheduledTask ann = AnnotationUtils.findAnnotation(m, ScheduledTask.class);
            if (ann != null) {
                registerMethod(bean, m, ann, targetClass);
            }
        }
    }
    scheduler.start();
}
```

**特性：**
- ✅ 自动处理 Spring AOP 代理
- ✅ 方法签名验证
- ✅ 错误处理和日志记录
- ✅ 支持方法名作为任务名（如果注解未指定）

---

### 7. 配置（Configuration）

#### 7.1 `ExecutorProperties` - 执行器配置属性

**位置：** `com.tuba.schedulercore.config.ExecutorProperties`

**作用：** 提供线程池配置属性

**配置项：**

```java
@ConfigurationProperties(prefix = "lite-scheduler.executor")
public class ExecutorProperties {
    private int poolSize = Runtime.getRuntime().availableProcessors();
    private int schedulerPoolSize = Math.max(2, Runtime.getRuntime().availableProcessors());
    private String threadNamePrefix = "lite-scheduler-";
}
```

**配置文件示例：**

```yaml
lite-scheduler:
  executor:
    pool-size: 10              # 执行器线程池大小
    scheduler-pool-size: 5     # 调度器线程池大小
    thread-name-prefix: "lite-scheduler-"  # 线程名称前缀
```

---

#### 7.2 `ExecutorConfiguration` - 配置类

**位置：** `com.tuba.schedulercore.config.ExecutorConfiguration`

**作用：** 启用配置属性绑定

```java
@Configuration
@EnableConfigurationProperties(ExecutorProperties.class)
public class ExecutorConfiguration {
}
```

---

## 🔄 执行流程

### 完整执行流程图

```
┌─────────────────────────────────────────────────────────────┐
│ 1. 应用启动                                                  │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. TaskRegistrar.onApplicationReady()                        │
│    - 扫描所有 Bean                                           │
│    - 查找 @ScheduledTask 方法                                │
│    - 创建 TaskDefinition                                    │
│    - 注册到 TaskRegistry                                     │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. Scheduler.schedule(TaskDefinition)                       │
│    - 检查任务是否启用                                         │
│    - 解析 Cron 表达式或数字秒                                │
│    - 创建 Trigger                                            │
│    - 调度任务到 ThreadPoolTaskScheduler                      │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. 定时触发（根据 Cron 或固定频率）                           │
│    - ThreadPoolTaskScheduler 触发                           │
│    - 创建 TaskContext                                        │
│    - 调用 TaskExecutor.execute()                            │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. SimpleTaskExecutor.execute()                             │
│    - 判断同步/异步                                           │
│    - 调用 invokeAndHandle()                                │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 6. invokeAndHandle()                                         │
│    ├─ 设置 startTime                                        │
│    ├─ PluginManager.before() → 所有插件的 before()          │
│    ├─ invokeMethod() → 反射调用任务方法                      │
│    ├─ 设置 endTime                                          │
│    ├─ PluginManager.after() → 所有插件的 after()            │
│    │  或                                                     │
│    └─ PluginManager.onError() → 所有插件的 onError()         │
│    └─ 创建 TaskLog                                           │
└─────────────────────────────────────────────────────────────┘
```

### 详细步骤说明

#### 步骤 1：应用启动
- Spring 容器初始化
- 所有 Bean 创建完成

#### 步骤 2：任务注册
- `TaskRegistrar` 监听 `ContextRefreshedEvent`
- 扫描所有 Bean，查找 `@ScheduledTask` 方法
- 为每个方法创建 `TaskDefinition`
- 注册到 `TaskRegistry`

#### 步骤 3：任务调度
- 调用 `Scheduler.schedule()`
- 解析 Cron 表达式或数字秒
- 创建 `Trigger` 并调度到 `ThreadPoolTaskScheduler`

#### 步骤 4：定时触发
- `ThreadPoolTaskScheduler` 根据 `Trigger` 触发任务
- 创建 `TaskContext`
- 调用 `TaskExecutor.execute()`

#### 步骤 5：任务执行
- 判断同步/异步
- 调用 `invokeAndHandle()`

#### 步骤 6：执行处理
- 设置开始时间
- 调用插件链 `before()`
- 反射调用任务方法
- 设置结束时间
- 调用插件链 `after()` 或 `onError()`
- 创建执行日志

---

## 🎨 设计模式

### 1. 策略模式（Strategy Pattern）

**应用：** `Scheduler` 接口和 `ThreadPoolScheduler` 实现

```java
// 策略接口
public interface Scheduler {
    void schedule(TaskDefinition definition);
    // ...
}

// 具体策略
public class ThreadPoolScheduler implements Scheduler {
    // 实现
}
```

**优势：** 可以轻松替换不同的调度实现（如 QuartzScheduler）

---

### 2. 模板方法模式（Template Method Pattern）

**应用：** `TaskExecutor` 接口和 `SimpleTaskExecutor` 实现

```java
// 模板方法
public void execute(TaskContext context) {
    if (def.isAsync()) {
        executor.submit(() -> invokeAndHandle(context));
    } else {
        invokeAndHandle(context);  // 模板方法
    }
}

// 具体实现
private void invokeAndHandle(TaskContext context) {
    // 1. before
    // 2. invoke
    // 3. after/onError
}
```

**优势：** 定义执行骨架，子类可扩展具体步骤

---

### 3. 观察者模式（Observer Pattern）

**应用：** 插件系统

```java
// 观察者接口
public interface TaskPlugin {
    void before(TaskContext context);
    void after(TaskContext context);
    void onError(TaskContext context, Throwable e);
}

// 主题（被观察者）
public class PluginManager {
    private List<TaskPlugin> plugins;  // 观察者列表
    
    public void before(TaskContext ctx) {
        for (TaskPlugin p : plugins) {  // 通知所有观察者
            p.before(ctx);
        }
    }
}
```

**优势：** 解耦任务执行和横切关注点（日志、重试、锁等）

---

### 4. 工厂模式（Factory Pattern）

**应用：** `TaskDefinition` 创建

```java
// TaskRegistrar 作为工厂
private void registerMethod(...) {
    TaskDefinition def = new TaskDefinition();
    def.setId(UUID.randomUUID().toString());
    def.setName(name);
    // ... 设置其他属性
    return def;
}
```

---

### 5. 单例模式（Singleton Pattern）

**应用：** Spring Bean（默认单例）

- `TaskRegistry` - 全局唯一任务注册表
- `PluginManager` - 全局唯一插件管理器
- `Scheduler` - 全局唯一调度器

---

## 💡 使用示例

### 示例 1：基本使用

```java
@Component
public class DataSyncTask {
    
    @ScheduledTask(
        name = "数据同步",
        cron = "0 0 2 * * ?",  // 每天凌晨 2 点
        description = "同步数据库数据"
    )
    public void syncData() {
        System.out.println("开始同步数据...");
        // 业务逻辑
        System.out.println("数据同步完成");
    }
}
```

---

### 示例 2：使用 TaskContext

```java
@Component
public class ReportTask {
    
    @ScheduledTask(
        name = "生成报表",
        cron = "0 0 1 * * ?",  // 每天凌晨 1 点
        async = false  // 同步执行
    )
    public void generateReport(TaskContext context) {
        // 获取任务信息
        String taskName = context.getDefinition().getName();
        Instant startTime = context.getStartTime();
        
        // 设置自定义属性
        context.putAttribute("reportType", "daily");
        
        // 业务逻辑
        generateReport();
        
        // 从上下文获取属性
        String reportType = context.getAttribute("reportType", String.class);
    }
}
```

---

### 示例 3：固定频率任务

```java
@Component
public class MonitorTask {
    
    @ScheduledTask(
        name = "系统监控",
        cron = "5",  // 每 5 秒执行一次（固定频率）
        async = true
    )
    public void monitor() {
        // 监控逻辑
        checkSystemHealth();
    }
}
```

---

### 示例 4：动态管理任务

```java
@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    
    @Autowired
    private TaskRegistry taskRegistry;
    
    @Autowired
    private Scheduler scheduler;
    
    // 获取所有任务
    @GetMapping
    public List<TaskDefinition> getAllTasks() {
        return new ArrayList<>(taskRegistry.getAll());
    }
    
    // 启用任务
    @PostMapping("/{id}/enable")
    public void enableTask(@PathVariable String id) {
        taskRegistry.enable(id);
        TaskDefinition def = taskRegistry.get(id);
        scheduler.schedule(def);  // 重新调度
    }
    
    // 禁用任务
    @PostMapping("/{id}/disable")
    public void disableTask(@PathVariable String id) {
        taskRegistry.disable(id);
        scheduler.unschedule(id);  // 取消调度
    }
    
    // 立即触发
    @PostMapping("/{id}/trigger")
    public void triggerTask(@PathVariable String id) {
        TaskDefinition def = taskRegistry.get(id);
        scheduler.triggerNow(def);
    }
}
```

---

## ⚙️ 配置说明

### application.yml 配置

```yaml
lite-scheduler:
  executor:
    pool-size: 10              # 执行器线程池大小（默认：CPU核心数）
    scheduler-pool-size: 5      # 调度器线程池大小（默认：max(2, CPU核心数)）
    thread-name-prefix: "lite-scheduler-"  # 线程名称前缀
```

### 配置说明

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `lite-scheduler.executor.pool-size` | int | CPU核心数 | 执行器线程池大小，用于执行任务 |
| `lite-scheduler.executor.scheduler-pool-size` | int | max(2, CPU核心数) | 调度器线程池大小，用于调度任务 |
| `lite-scheduler.executor.thread-name-prefix` | String | "lite-scheduler-" | 线程名称前缀，便于调试 |

---

## 🔌 扩展点

### 1. 自定义插件

```java
@Component
public class MyCustomPlugin implements TaskPlugin {
    
    @Override
    public void before(TaskContext context) {
        System.out.println("任务执行前：{}", context.getDefinition().getName());
    }
    
    @Override
    public void after(TaskContext context) {
        System.out.println("任务执行成功：{}", context.getDefinition().getName());
    }
    
    @Override
    public void onError(TaskContext context, Throwable e) {
        System.err.println("任务执行失败：{}", context.getDefinition().getName());
        e.printStackTrace();
    }
}
```

**自动注册：** 只需实现 `TaskPlugin` 并标注 `@Component`，Spring 会自动注入到 `PluginManager`

---

### 2. 自定义执行器

```java
@Component
public class CustomTaskExecutor implements TaskExecutor {
    
    @Override
    public void execute(TaskContext context) {
        // 自定义执行逻辑
        // 例如：远程执行、分布式执行等
    }
}
```

**注意：** 需要确保只有一个 `TaskExecutor` 实现，或者使用 `@Primary` 指定

---

### 3. 自定义调度器

```java
@Component
@Primary
public class QuartzScheduler implements Scheduler {
    
    @Override
    public void schedule(TaskDefinition definition) {
        // 使用 Quartz 实现调度
    }
    
    // 实现其他方法...
}
```

---

## 📊 性能特性

### 线程池隔离

- **执行器线程池**：用于执行任务方法
- **调度器线程池**：用于调度任务触发
- **隔离设计**：避免调度和执行相互影响

### 异步执行

- 默认异步执行，不阻塞调度线程
- 支持同步执行（`async = false`）

### 优雅关闭

- 应用关闭时先停止接受新任务
- 等待正在执行的任务完成（30 秒）
- 超时后强制关闭

---

## 🔒 线程安全

### 线程安全组件

1. **TaskRegistry**
   - 使用 `ConcurrentHashMap` 存储任务
   - 所有操作都是线程安全的

2. **ThreadPoolScheduler**
   - 使用 `ConcurrentHashMap` 存储 `ScheduledFuture`
   - 调度操作线程安全

3. **PluginManager**
   - 插件列表在初始化时确定，后续只读
   - 插件调用是线程安全的

---

## 📝 最佳实践

### 1. 任务方法设计

✅ **推荐：**
```java
@ScheduledTask(cron = "0 0 2 * * ?")
public void task() {
    // 无参方法，简单清晰
}

@ScheduledTask(cron = "0 0 2 * * ?")
public void taskWithContext(TaskContext context) {
    // 需要上下文信息时使用
}
```

❌ **不推荐：**
```java
@ScheduledTask(cron = "0 0 2 * * ?")
public void task(String param) {  // 不支持多参数
    // ...
}
```

---

### 2. 异常处理

✅ **推荐：**
```java
@ScheduledTask(cron = "5")
public void task() {
    try {
        // 业务逻辑
    } catch (Exception e) {
        // 记录日志
        log.error("任务执行失败", e);
        // 不要吞掉异常，让框架处理
        throw e;
    }
}
```

---

### 3. 长时间运行的任务

✅ **推荐：**
```java
@ScheduledTask(cron = "0 0 2 * * ?", async = true)
public void longRunningTask() {
    // 长时间运行的任务使用异步执行
    // 避免阻塞调度线程
}
```

---

### 4. 任务分组

✅ **推荐：**
```java
@ScheduledTask(cron = "5", group = "monitor")
public void monitorTask() {
    // 监控类任务
}

@ScheduledTask(cron = "0 0 2 * * ?", group = "sync")
public void syncTask() {
    // 同步类任务
}
```

便于管理和查询

---

## 🎯 总结

### Core 模块核心价值

1. **轻量级**：核心代码约 2000 行，易于理解
2. **可扩展**：插件机制支持功能扩展
3. **易用性**：注解式声明，自动扫描注册
4. **灵活性**：支持多种调度模式，可配置
5. **健壮性**：完善的异常处理和优雅关闭
6. **可观测性**：完整的日志和上下文信息

### 适用场景

- ✅ 中小型项目的定时任务需求
- ✅ 需要灵活扩展的任务调度
- ✅ 学习和理解调度框架原理
- ✅ 作为毕业设计项目

### 技术栈

- Java 17+
- Spring Boot 2.6+
- Spring Scheduling（ThreadPoolTaskScheduler）
- SLF4J 日志

---

## 📚 相关文档

- [需求总结.md](./需求总结.md) - 项目需求文档
- [模块详细设想.md](./模块详细设想.md) - 模块设计文档
- [优化实施总结.md](./优化实施总结.md) - 优化实施报告

---

**Core 模块是 Lite-Scheduler 的基础，提供了完整的任务调度能力，为上层模块（plugins、starter、samples）提供了坚实的基础。** 🚀

