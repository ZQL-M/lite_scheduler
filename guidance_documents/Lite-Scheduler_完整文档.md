# Lite-Scheduler 完整文档

## 📋 目录

1. [项目概述](#项目概述)
2. [核心功能和特性](#核心功能和特性)
3. [架构设计](#架构设计)
4. [核心组件详解](#核心组件详解)
5. [执行流程](#执行流程)
6. [设计模式](#设计模式)
7. [使用示例](#使用示例)
8. [配置说明](#配置说明)
9. [扩展点](#扩展点)
10. [已完成的优化](#已完成的优化)
11. [未来实现计划](#未来实现计划)
12. [技术栈](#技术栈)
13. [资源需求](#资源需求)
14. [风险预警](#风险预警)
15. [成功标准](#成功标准)

## 📦 项目概述

### 项目定位

Lite-Scheduler 是一个**轻量级、可扩展、无侵入、可插拔**的 Java 定时任务框架，设计目标是比 Quartz 更简单，比 Spring Scheduler 更灵活，比 XXL-Job 更轻量，适合作为一个课程毕设、开源小项目，也能实际落地使用。

### 核心价值

- **轻量性**：无需数据库、无需外部服务即可运行
- **无侵入性**：任务只需要一个注解即可加入调度
- **可扩展性**：插件机制赋予高度可定制能力
- **可插拔性**：Redis 插件、重试插件按需启用
- **可观测性**：提供完整执行上下文与日志
- **可管理性**：提供任务启动/停止/手动触发能力
- **适合毕设**：复杂度中等，具有框架化特色
- **可生产可用**：真正可以在业务中跑起来

### 模块结构

```
Lite-Scheduler/
├── scheduler-core/          # 核心调度模块
├── scheduler-plugin/        # 插件系统
├── scheduler-spring/        # Spring Boot 启动器
├── scheduler-samples/       # 示例应用
└── guidance_documents/      # 文档
```

## ✨ 核心功能和特性

### 已实现功能

- ✅ **声明式任务定义**：通过 `@ScheduledTask` 注解定义任务
- ✅ **自动扫描注册**：启动时自动扫描并注册所有任务
- ✅ **多种调度模式**：支持 Cron 表达式、固定频率、固定延迟和简化时间配置
- ✅ **任务持久化**：支持任务定义和执行日志的持久化
- ✅ **插件化架构**：可扩展的插件机制
- ✅ **异步/同步执行**：支持任务异步或同步执行
- ✅ **完整的执行上下文**：提供任务执行过程中的所有信息
- ✅ **优雅关闭**：应用关闭时优雅停止所有任务

### 计划实现功能

- 🔄 数据库持久化：支持 MySQL 等主流数据库
- 🔄 常用插件：日志、重试、分布式锁插件
- 🔄 任务分片功能：支持分布式环境下任务分片执行
- 🔄 监控面板：提供 Web 界面展示任务状态和统计
- 🔄 REST API 管理接口：支持动态添加、修改、删除任务
- 🔄 告警功能：任务异常时发送邮件或短信告警
- 🔄 任务依赖关系：支持任务间的依赖关系定义和执行
- 🔄 任务参数动态传递：支持任务执行时动态传递参数

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
│                                                      │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────┐
│               Persistence Layer                        │
│  ┌──────────────────────────────────────────┐          │
│  │  TaskPersistenceService                  │          │
│  │  - 任务定义持久化                        │          │
│  │  - 任务日志持久化                        │          │
│  └──────────────────────────────────────────┘          │
└─────────────────────────────────────────────────────────┘
```

### 设计原则

1. **单一职责原则**：每个组件只负责一个功能
2. **开闭原则**：对扩展开放，对修改关闭（插件机制）
3. **依赖倒置**：依赖接口而非实现
4. **接口隔离**：接口设计精简，职责清晰
5. **模块化设计**：清晰的模块划分，便于扩展和维护

## 🔧 核心组件详解

### 1. 注解层（Annotation Layer）

#### `@ScheduledTask`

**位置**：`com.tuba.schedulercore.annotation.ScheduledTask`

**作用**：声明式定义定时任务

**字段说明**：

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `name` | String | "" | 任务名称，为空时使用方法名 |
| `cron` | String | "" | Cron 表达式（与其他时间配置互斥） |
| `fixedRate` | long | -1 | 固定频率，单位毫秒 |
| `fixedDelay` | long | -1 | 固定延迟，单位毫秒 |
| `seconds` | String | "" | 秒级配置（如 "5" 表示每5秒，"0,30" 表示每分钟0和30秒） |
| `minutes` | String | "" | 分钟级配置（如 "15" 表示每15分钟） |
| `hours` | String | "" | 小时级配置（如 "9,18" 表示每天9点和18点） |
| `days` | String | "" | 天级配置（如 "1" 表示每天执行一次） |
| `async` | boolean | true | 是否异步执行 |
| `group` | String | "default" | 任务分组 |
| `description` | String | "" | 任务描述 |
| `persistent` | boolean | false | 是否持久化任务 |

**优先级顺序**：`cron > fixedRate/fixedDelay > seconds/minutes/hours/days`

### 2. 模型层（Model Layer）

#### 2.1 `TaskDefinition` - 任务定义

**位置**：`com.tuba.schedulercore.model.TaskDefinition`

**作用**：封装任务的元数据和执行信息

**核心字段**：

```java
public class TaskDefinition {
    private String id;              // 任务唯一标识（UUID）
    private String name;             // 任务名称
    private String group = "default";  // 任务分组
    private String cron;              // Cron 表达式
    private long fixedRate = -1;     // 固定频率，单位毫秒
    private long fixedDelay = -1;    // 固定延迟，单位毫秒
    private boolean async = true;     // 是否异步执行
    private Object bean;             // Spring Bean 实例
    private Method method;           // 要执行的方法
    private String description;      // 任务描述
    private boolean enabled = true;  // 是否启用
    private boolean persistent = false; // 是否持久化
    private List<TaskPlugin> plugins; // 任务级插件
}
```

#### 2.2 `TaskContext` - 任务执行上下文

**位置**：`com.tuba.schedulercore.model.TaskContext`

**作用**：封装任务执行过程中的所有信息

**核心字段**：

```java
public class TaskContext {
    private TaskDefinition definition;  // 任务定义
    private Instant startTime;          // 开始时间
    private Instant endTime;            // 结束时间
    private Map<String, Object> attributes = new HashMap<>(); // 自定义属性
    private Throwable error;            // 异常信息（如有）
}
```

#### 2.3 `TaskLog` - 任务执行日志

**位置**：`com.tuba.schedulercore.model.TaskLog`

**作用**：记录任务执行的历史信息

**核心字段**：

```java
public class TaskLog {
    private String id;           // 日志ID
    private String taskId;       // 任务ID
    private String taskName;     // 任务名称
    private String groupName;    // 任务分组
    private Instant startTime;    // 开始时间
    private Instant endTime;      // 结束时间
    private String status;        // 状态：SUCCESS / FAIL
    private String message;       // 消息（错误信息等）
    private Long durationMs;     // 执行时长（毫秒）
}
```

### 3. 持久化层（Persistence Layer）

#### 3.1 `TaskPersistenceService` - 持久化服务接口

**位置**：`com.tuba.schedulercore.persistence.TaskPersistenceService`

**作用**：定义任务持久化的标准接口

**接口定义**：

```java
public interface TaskPersistenceService {
    // 任务定义持久化
    boolean saveTask(TaskDefinition taskDefinition);
    boolean updateTask(TaskDefinition taskDefinition);
    boolean deleteTask(String taskId);
    TaskDefinition getTaskById(String taskId);
    List<TaskDefinition> getAllTasks();
    List<TaskDefinition> getEnabledTasks();
    
    // 任务日志持久化
    boolean saveTaskLog(TaskLog taskLog);
    List<TaskLog> getTaskLogs(String taskId, int limit);
    TaskLog getLatestTaskLog(String taskId);
}
```

#### 3.2 `InMemoryTaskPersistenceService` - 基于内存的实现

**位置**：`com.tuba.schedulercore.persistence.InMemoryTaskPersistenceService`

**作用**：提供基于内存的持久化实现，适合开发和测试环境

### 4. 插件系统（Plugin System）

#### 4.1 `TaskPlugin` - 插件接口

**位置**：`com.tuba.schedulercore.plugin.TaskPlugin`

**作用**：定义插件生命周期回调

**接口定义**：

```java
public interface TaskPlugin {
    default void before(TaskContext context) {}
    default void after(TaskContext context) {}
    default void onError(TaskContext context, Throwable e) {}
}
```

#### 4.2 `PluginManager` - 插件管理器

**位置**：`com.tuba.schedulercore.plugin.PluginManager`

**作用**：管理所有插件，统一调用

**核心功能**：
- 自动注入所有 `TaskPlugin` 实现
- 按顺序调用插件方法
- 捕获插件异常，记录日志但不中断流程
- 支持插件的动态扩展

### 5. 执行器（Executor）

#### 5.1 `TaskExecutor` - 执行器接口

**位置**：`com.tuba.schedulercore.executor.TaskExecutor`

**作用**：定义任务执行的标准接口

**接口定义**：

```java
public interface TaskExecutor {
    void execute(TaskContext context);
}
```

#### 5.2 `SimpleTaskExecutor` - 简单执行器实现

**位置**：`com.tuba.schedulercore.executor.SimpleTaskExecutor`

**作用**：实现任务执行的核心逻辑

**核心功能**：
- 支持同步/异步执行
- 调用插件链
- 反射调用任务方法
- 处理异常
- 生成执行日志

### 6. 调度器（Scheduler）

#### 6.1 `Scheduler` - 调度器接口

**位置**：`com.tuba.schedulercore.scheduler.Scheduler`

**作用**：定义任务调度的标准接口

**接口方法**：

```java
public interface Scheduler {
    void schedule(TaskDefinition definition);    // 调度任务
    void unschedule(String taskId);               // 取消调度
    void start();                                 // 启动调度器
    void stop();                                  // 停止调度器
    void triggerNow(TaskDefinition definition);  // 立即触发
}
```

#### 6.2 `ThreadPoolScheduler` - 线程池调度器实现

**位置**：`com.tuba.schedulercore.scheduler.ThreadPoolScheduler`

**作用**：基于 Spring `ThreadPoolTaskScheduler` 实现任务调度

**核心功能**：
- 支持多种调度模式（Cron/fixedRate/fixedDelay）
- 管理任务的调度生命周期
- 提供任务的立即触发能力
- 支持优雅关闭

### 7. 注册器（Registry）

#### 7.1 `TaskRegistry` - 任务注册表

**位置**：`com.tuba.schedulercore.registry.TaskRegistry`

**作用**：管理所有已注册的任务定义

**核心功能**：
- 注册/注销任务
- 查询任务
- 启用/禁用任务

#### 7.2 `TaskRegistrar` - 任务注册器

**位置**：`com.tuba.schedulercore.registry.TaskRegistrar`

**作用**：启动时自动扫描并注册所有 `@ScheduledTask` 方法

**核心流程**：
1. 监听 `ContextRefreshedEvent`（Spring 上下文刷新完成）
2. 从持久化服务加载所有任务
3. 遍历所有 Spring Bean
4. 扫描类的所有方法，查找 `@ScheduledTask` 注解
5. 验证方法签名（无参或单参数 `TaskContext`）
6. 创建 `TaskDefinition`
7. 注册到 `TaskRegistry`
8. 如果是新任务且需要持久化，保存到持久化服务
9. 调用 `Scheduler.schedule()` 开始调度

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
│    - 从持久化服务加载任务                                     │
│    - 扫描所有 Bean                                           │
│    - 查找 @ScheduledTask 方法                                │
│    - 创建 TaskDefinition                                    │
│    - 注册到 TaskRegistry                                     │
│    - 新任务持久化到存储                                     │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. Scheduler.schedule(TaskDefinition)                       │
│    - 检查任务是否启用                                         │
│    - 根据任务类型选择调度模式                                 │
│    - 创建 Trigger 或使用固定频率/延迟                          │
│    - 调度任务到 ThreadPoolTaskScheduler                      │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. 定时触发（根据调度模式）                                   │
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
│    └─ 持久化 TaskLog 到存储                                 │
└─────────────────────────────────────────────────────────────┘
```

## 🎨 设计模式

### 1. 策略模式（Strategy Pattern）

**应用**：`Scheduler` 接口和 `ThreadPoolScheduler` 实现

**优势**：可以轻松替换不同的调度实现（如 QuartzScheduler）

### 2. 模板方法模式（Template Method Pattern）

**应用**：`TaskExecutor` 接口和 `SimpleTaskExecutor` 实现

**优势**：定义执行骨架，子类可扩展具体步骤

### 3. 观察者模式（Observer Pattern）

**应用**：插件系统

**优势**：解耦任务执行和横切关注点（日志、重试、锁等）

### 4. 工厂模式（Factory Pattern）

**应用**：`TaskDefinition` 创建

**优势**：集中管理对象创建，便于扩展

### 5. 单例模式（Singleton Pattern）

**应用**：Spring Bean（默认单例）

- `TaskRegistry` - 全局唯一任务注册表
- `PluginManager` - 全局唯一插件管理器
- `Scheduler` - 全局唯一调度器
- `TaskPersistenceService` - 全局唯一持久化服务

**优势**：确保全局只有一个实例，节省资源

### 6. 代理模式（Proxy Pattern）

**应用**：Spring AOP 代理处理

**优势**：透明处理代理对象，确保方法反射调用正确

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

### 示例 2：使用简化时间配置

```java
@Component
public class MonitorTask {
    
    // 每5秒执行一次
    @ScheduledTask(
        name = "系统监控",
        seconds = "5",  // 简化秒级配置
        async = true,
        persistent = true  // 持久化任务
    )
    public void monitor() {
        // 监控逻辑
        checkSystemHealth();
    }
    
    // 每分钟的0和30秒执行
    @ScheduledTask(
        name = "定时检查",
        seconds = "0,30",
        async = true
    )
    public void check() {
        // 检查逻辑
    }
}
```

### 示例 3：使用固定频率和固定延迟

```java
@Component
public class BatchTask {
    
    // 固定频率：每2秒执行一次
    @ScheduledTask(
        name = "固定频率任务",
        fixedRate = 2000,
        async = true
    )
    public void fixedRateTask() {
        System.out.println("固定频率任务执行: " + LocalDateTime.now());
    }
    
    // 固定延迟：执行完成后延迟3秒再次执行
    @ScheduledTask(
        name = "固定延迟任务",
        fixedDelay = 3000,
        async = true
    )
    public void fixedDelayTask() {
        System.out.println("固定延迟任务执行: " + LocalDateTime.now());
    }
}
```

### 示例 4：使用 TaskContext

```java
@Component
public class ReportTask {
    
    @ScheduledTask(
        name = "生成报表",
        cron = "0 0 1 * * ?",  // 每天凌晨 1 点
        async = false  // 同步执行以便测试
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

### 示例 5：动态管理任务

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

## ⚙️ 配置说明

### application.yml 配置

```yaml
lite-scheduler:
  executor:
    pool-size: 10              # 执行器线程池大小（默认：CPU核心数）
    scheduler-pool-size: 5      # 调度器线程池大小（默认：max(2, CPU核心数)）
    thread-name-prefix: "lite-scheduler-"  # 线程名称前缀

spring:
  application:
    name: scheduler-samples

server:
  port: 8080

# 日志配置
logging:
  level:
    com.tuba.schedulercore: DEBUG
    root: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

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

**自动注册**：只需实现 `TaskPlugin` 并标注 `@Component`，Spring 会自动注入到 `PluginManager`

### 2. 自定义持久化实现

```java
@Component
@Primary
public class DatabaseTaskPersistenceService implements TaskPersistenceService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Override
    public boolean saveTask(TaskDefinition taskDefinition) {
        // 数据库保存逻辑
        String sql = "INSERT INTO lite_scheduler_task (...) VALUES (...)";
        // 执行保存
        return true;
    }
    
    // 实现其他方法...
}
```

**注意**：使用 `@Primary` 注解替换默认的内存实现

### 3. 自定义执行器

```java
@Component
@Primary
public class CustomTaskExecutor implements TaskExecutor {
    
    @Override
    public void execute(TaskContext context) {
        // 自定义执行逻辑
        // 例如：远程执行、分布式执行等
    }
}
```

### 4. 自定义调度器

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

## ✅ 已完成的优化

### 1. 添加日志系统（SLF4J）

**修改的文件**：
- `TaskRegistrar.java` - 替换所有 `System.out.println` 和 `System.err.println` 为 SLF4J 日志
- `PluginManager.java` - 添加日志记录

**改进点**：
- 使用 `log.info()`, `log.warn()`, `log.error()` 替代 System.out/err
- 支持日志级别控制
- 生产环境可配置日志输出

### 2. 优化异常处理

**修改的文件**：
- `PluginManager.java` - 记录插件异常而不是完全忽略

**改进点**：
- 插件异常现在会被记录到日志
- 包含完整的异常堆栈信息
- 便于生产环境故障排查

### 3. 优雅关闭线程池

**修改的文件**：
- `SimpleTaskExecutor.java` - 实现优雅关闭
- `ThreadPoolScheduler.java` - 实现优雅关闭并实现 `DisposableBean` 接口

**改进点**：
- 先调用 `shutdown()` 停止接受新任务
- 等待 30 秒让任务完成
- 如果超时，再调用 `shutdownNow()` 强制关闭
- 避免应用关闭时丢失任务

### 4. 线程池配置化

**新增文件**：
- `ExecutorProperties.java` - 配置属性类
- `ExecutorConfiguration.java` - 配置类

**修改的文件**：
- `SimpleTaskExecutor.java` - 使用配置属性
- `ThreadPoolScheduler.java` - 使用配置属性

**改进点**：
- 线程池大小可通过配置文件设置
- 支持 `application.yml` 配置
- 如果未配置，使用默认值（CPU 核心数）

### 5. 消除代码重复

**修改的文件**：
- `SimpleTaskExecutor.java` - 重构 `invokeAndHandle` 方法

**改进点**：
- 提取 `createTaskLog()` 方法
- 提取 `extractActualError()` 方法
- 提取 `calculateDuration()` 方法
- 使用 `finally` 块统一设置日志时间
- 代码更清晰，维护更容易

### 6. 增强空指针检查

**修改的文件**：
- `ThreadPoolScheduler.java` - `schedule()` 和 `triggerNow()` 方法
- `SimpleTaskExecutor.java` - `execute()` 和 `invokeMethod()` 方法
- `TaskRegistrar.java` - `registerMethod()` 方法

**改进点**：
- 在关键方法入口添加 null 检查
- 提供清晰的错误日志
- 避免 NPE 异常

## 🗓️ 未来实现计划

### 第一阶段：基础功能完善（第1-2周）

**目标**：完善核心功能，确保基础调度能力稳定可靠

| 任务 | 时间 | 责任人 | 完成标准 |
|------|------|--------|----------|
| 1. 实现数据库持久化 | 3天 | 开发 | 替换内存实现，支持 MySQL 等主流数据库 |
| 2. 完善 Spring Boot Starter | 2天 | 开发 | 支持自动配置，可直接作为 starter 引入 |
| 3. 实现常用插件 | 3天 | 开发 | 实现日志、重试、分布式锁插件 |
| 4. 添加单元测试 | 2天 | 开发 | 核心组件测试覆盖率达到 80% 以上 |
| 5. 添加集成测试 | 2天 | 测试 | 验证模块间协同工作正常 |
| 6. 修复现有 bug | 2天 | 开发 | 解决编译、运行时的已知问题 |

### 第二阶段：扩展功能实现（第3-5周）

**目标**：实现高级功能，提升框架的实用性和扩展性

| 任务 | 时间 | 责任人 | 完成标准 |
|------|------|--------|----------|
| 1. 实现任务分片功能 | 3天 | 开发 | 支持分布式环境下任务分片执行 |
| 2. 实现监控面板 | 5天 | 前端+后端 | 提供 Web 界面展示任务状态和统计 |
| 3. 实现 REST API 管理接口 | 4天 | 开发 | 支持动态添加、修改、删除任务 |
| 4. 实现告警功能 | 3天 | 开发 | 任务异常时发送邮件或短信告警 |
| 5. 实现任务依赖关系 | 3天 | 开发 | 支持任务间的依赖关系定义和执行 |
| 6. 实现任务参数动态传递 | 2天 | 开发 | 支持任务执行时动态传递参数 |

### 第三阶段：安全和性能优化（第6-7周）

**目标**：确保框架安全可靠，性能优化

| 任务 | 时间 | 责任人 | 完成标准 |
|------|------|--------|----------|
| 1. 添加任务权限控制 | 3天 | 开发 | 支持不同角色对任务的不同操作权限 |
| 2. 实现任务参数校验 | 2天 | 开发 | 防止恶意参数注入 |
| 3. 优化线程池配置 | 2天 | 开发 | 支持动态调整线程池大小 |
| 4. 性能测试和优化 | 3天 | 测试+开发 | 压力测试下稳定运行，响应时间满足要求 |
| 5. 安全审计和漏洞修复 | 3天 | 安全 | 进行安全扫描，修复潜在漏洞 |
| 6. 实现加密存储敏感信息 | 2天 | 开发 | 对敏感配置信息进行加密存储 |

### 第四阶段：文档和示例完善（第8周）

**目标**：完善文档和示例，便于用户使用和扩展

| 任务 | 时间 | 责任人 | 完成标准 |
|------|------|--------|----------|
| 1. 完善用户手册 | 2天 | 文档 | 详细说明框架的安装、配置和使用 |
| 2. 添加更多示例项目 | 3天 | 开发 | 涵盖多种使用场景的示例 |
| 3. 编写 API 文档 | 2天 | 开发 | 使用 Swagger 生成 API 文档 |
| 4. 编写部署指南 | 2天 | 文档 | 说明单机和分布式部署方式 |
| 5. 编写扩展开发指南 | 2天 | 文档 | 指导开发者扩展框架功能 |

### 第五阶段：验收和发布（第9周）

**目标**：全面验收，准备正式发布

| 任务 | 时间 | 责任人 | 完成标准 |
|------|------|--------|----------|
| 1. 全面回归测试 | 3天 | 测试 | 所有功能正常，无严重 bug |
| 2. 用户验收测试 | 2天 | 产品+测试 | 满足用户需求，用户体验良好 |
| 3. 修复验收中发现的问题 | 2天 | 开发 | 解决所有验收问题 |
| 4. 准备发布版本 | 2天 | 开发 | 打包发布版本，准备发布说明 |
| 5. 发布正式版本 | 1天 | 发布 | 发布到 Maven 中央仓库 |

## 🔧 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 开发语言 | Java | 17+ |
| 框架 | Spring Boot | 2.6+ |
| 持久化 | MySQL | 8.0+ |
| 缓存 | Redis | 7.0+ |
| 前端框架 | Vue.js | 3.0+ |
| 构建工具 | Maven | 3.6+ |
| 测试框架 | JUnit 5 | - |
| 分布式锁 | Redis | - |
| API 文档 | Swagger | 3.0+ |

## 📊 资源需求

| 角色 | 数量 | 主要职责 |
|------|------|----------|
| 后端开发 | 2-3人 | 核心功能开发，API 实现 |
| 前端开发 | 1人 | 监控面板开发 |
| 测试工程师 | 1人 | 测试用例编写，功能测试 |
| 文档工程师 | 1人 | 用户文档，API 文档编写 |
| 安全工程师 | 1人 | 安全审计，漏洞修复 |

## 🚨 风险预警

1. **技术风险**：分布式锁和任务分片实现复杂度较高，可能需要更多时间
2. **资源风险**：前端开发资源不足，可能影响监控面板的开发进度
3. **依赖风险**：外部依赖（如 Redis、MySQL）的稳定性可能影响框架运行
4. **测试风险**：全面测试需要大量时间和资源，可能影响发布时间

## 📌 注意事项

1. **优先保证核心功能**：确保核心调度功能稳定可靠，再进行扩展功能开发
2. **注重代码质量**：遵循编码规范，添加充分的注释和测试
3. **考虑扩展性**：设计时考虑未来扩展，预留扩展点
4. **注重用户体验**：提供友好的用户界面和清晰的文档
5. **确保安全性**：在设计和实现过程中始终考虑安全性

## 🎯 成功标准

1. **功能完整**：实现所有规划的功能
2. **稳定可靠**：在高并发场景下稳定运行，无严重 bug
3. **安全可控**：通过安全审计，无重大安全漏洞
4. **性能优良**：响应时间和吞吐量满足要求
5. **易于使用**：提供清晰的文档和友好的用户界面
6. **易于扩展**：提供良好的扩展机制，支持自定义功能

## 🚀 预期成果

1. **完整的调度框架**：功能齐全、安全可靠的任务调度框架
2. **丰富的插件生态**：提供多种常用插件
3. **友好的用户界面**：直观的监控和管理界面
4. **完善的文档**：详细的用户手册和 API 文档
5. **示例项目**：涵盖多种使用场景的示例
6. **社区支持**：活跃的社区，持续更新和维护

**Lite-Scheduler** - 用最少的代码，实现一个可运行、可扩展、可插拔、易理解的 Java 任务调度框架。 🚀

---

**版本**：v2.0.0  
**更新时间**：2025-12-07  
**作者**：Lite-Scheduler 开发团队