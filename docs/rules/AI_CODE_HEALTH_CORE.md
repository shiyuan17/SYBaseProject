# AI 健康度开发规范：原则与函数

# 前言

本规范用于约束 AI（Codex、Claude Code、Cursor、Copilot、OpenClaw、Gemini 等）生成代码时的行为，目标不是追求极致抽象或最少代码，而是持续产出：

* 易理解
* 易维护
* 易测试
* 易扩展
* 易审查

的健康代码。

---

# 一、核心原则（必须遵守）

## 1.1 基本准则

✅ 显式优于隐式

✅ 清晰优于聪明

✅ 稳定优于炫技

✅ 可理解性优于代码行数

✅ 可维护性优于设计模式堆砌

---

## 1.2 最终目标

所有规则均服务于：

降低认知负担

而非机械式满足规范。

---

# 二、函数健康度规范

函数是系统健康度的最小单元。

---

## 2.1 单一职责原则

一个函数只负责一件事情。

### 正例

```ts
function validateOrder(order: Order): ValidationResult
```

```ts
function calculatePrice(items: OrderItem[]): number
```

### 反例

```ts
function processOrder() {
  validate();
  calculate();
  save();
  notify();
}
```

同时承担：

* 校验
* 计算
* 持久化
* 消息通知

违反单一职责原则。

---

## 2.2 单一抽象层级

同一个函数内不要混用：

* 业务逻辑
* 技术实现
* 基础设施细节

### 正例

```ts
async function createOrder(command: CreateOrderCommand) {
  const order = buildOrder(command);

  await orderRepository.save(order);

  await eventBus.publish(
    new OrderCreated(order.id)
  );
}
```

---

### 反例

```ts
async function createOrder(command) {

  const sql =
    "insert into order ...";

  await mysql.execute(sql);

  sendKafkaMessage(...);

  writeAuditLog(...);

}
```

业务与实现混杂。

---

## 2.3 无副作用原则

函数应尽量保持纯净。

### 禁止

修改外部状态：

```ts
globalState.user = user;
```

修改入参：

```ts
function updateOrder(order) {
  order.status = "PAID";
}
```

---

### 推荐

返回新对象：

```ts
function markOrderPaid(
  order: Order
): Order {
  return {
    ...order,
    status: "PAID"
  };
}
```

---

## 2.4 可独立测试

函数必须可以：

* 单独调用
* 独立验证
* 无环境依赖

---

## 2.5 行数建议

### 行数是参考值，不是目标

| 类型     | 建议      |
| ------ | ------- |
| 业务编排函数 | ≤ 30 行  |
| 算法函数   | ≤ 60 行  |
| UI组件   | ≤ 100 行 |
| 配置文件   | 不限制     |

---

### 禁止

为了减少行数：

* 过度抽象
* 提取无意义函数
* 拆分到无法阅读

---

### 反例

```ts
function process() {
  step1();
  step2();
  step3();
}
```

阅读者必须不断跳转。

属于形式主义拆分。

---