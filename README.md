# EasyEventBus

## 💡 重复造轮子？也许这不是件坏事，我会告诉你为什么要这么做。

本项目通过**内存事件总线**将单体项目进行**高度模块化**，旨在适应 **AI 编程** 背景下的 **Spec 理念** 和 **上下文工程（Context Engineering）**，想象充分模块化后N个Agent并行编码的效率，真正的N x效率提升。

引入 EasyEventBus 的核心价值在于：
- **降低 AI 上下文负担**：得益于事件驱动的模块化设计，AI 在修改代码时**不再需要索引整个代码仓库**。每个模块只需关注特定的事件输入与输出，每一个Agent，可以聚焦于各自的局部上下文，显著提升了代码理解深度和生成准确率。
- **代码组织结构规范化**：不仅需求（Spec）有规范，**代码组织本身也建立了标准规范**。通过强制的“三阶段处理模式”（幂等检查 -> 业务处理 -> 失败兜底），为 AI 生成代码提供了一个清晰的结构模板，确保生成的代码天然具备健壮性和一致性。
- **增强可扩展性**：面对未来大概率出现的业务扩展和服务划分。

## ✨ 核心特性

### 🎯 三阶段事件处理模式
- **幂等性检查** (`@Idempotent`) - 防止重复处理
- **智能重试** (`@FailRetry`) - 自动重试失败的事件
- **失败处理** (`@FailSubscribe`) - 优雅处理最终失败

### 🔧 企业级功能
- **延迟事件** - 支持延迟发布事件到未来时间点
- **全局拦截器** - 提供事件处理的监控和审计能力
- **Spring Boot集成** - 零配置自动装配
- **性能监控** - 内置性能统计和监控
- **可扩展性** - 新增功能均保留可扩展性

### 对比google guava

google guava官方停止维护（又一个被google抛弃的仓库罢了）
<img width="1422" height="841" alt="1755326797112" src="https://github.com/user-attachments/assets/3e83404c-d2d2-46a1-b3c3-2e18e82a074f" />

新组件基于guava丰富了功能和保留可扩展性
<img width="1522" height="1202" alt="1755326934829" src="https://github.com/user-attachments/assets/c249a97b-c0b0-49dc-adf2-11b187a96f5b" />


## 🚀 快速开始

### Maven依赖

```xml
<dependency>
    <groupId>com.tobyang</groupId>
    <artifactId>easyeventbus-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 基础配置

```yaml
easyeventbus:
  enable: true
  async-enabled: true
  async-thread-pool-size: 10
  delayed:
    enabled: true
    core-pool-size: 2
```

### 事件监听器

```java
@Component
public class OrderEventListener {

    // 阶段1：幂等性检查
    @Idempotent
    public boolean checkOrderNotProcessed(OrderCreatedEvent event) {
        return !orderRepository.exists(event.getOrderId());
    }

    // 阶段2：业务处理（支持重试）
    @Subscribe
    @FailRetry(retries = 3, interval = 5, timeUnit = TimeUnit.SECONDS)
    public void handleOrderCreated(OrderCreatedEvent event) {
        orderService.processOrder(event);
    }

    // 阶段3：失败处理
    @FailSubscribe
    public void handleOrderFailure(OrderCreatedEvent event, FailureContext context) {
        alertService.sendAlert("订单处理失败", context);
        deadLetterQueue.send(event);
    }
}
```

## 🎨 特性

### 全局拦截器 & 失败上下文

```java
@Component
public class EventMonitorInterceptor implements EventInterceptor {
    @Override
    public void afterProcessingFailure(Object event, Throwable exception, InterceptorContext context) {
        metricsCollector.recordFailure(event.getClass(), context.getDuration());
    }
}

@FailSubscribe
public void handleFailure(PaymentEvent event, FailureContext context) {
    switch (context.getFailureType()) {
        case RETRY_EXHAUSTED -> deadLetterQueue.send(event, context);
        case PROCESSING_EXCEPTION -> errorLogger.log(event, context.getFailureCause());
        case SYSTEM_EXCEPTION -> alertService.sendAlert(context);
    }
}
```

## 🏗️ 架构

### 三阶段处理模式
```
事件发布 → 幂等性检查 → 业务处理 → 失败处理
  post()   @Idempotent   @Subscribe  @FailSubscribe
                           ↓
                      自动重试(@FailRetry)
```


---

<div align="center">

**⭐ 如果这个项目对您有帮助，请给个 Star 支持！**

</div>
