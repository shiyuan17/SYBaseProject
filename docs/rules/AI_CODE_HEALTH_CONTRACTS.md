# AI 健康度开发规范：命名、类型与错误

# 三、命名规范

命名是 AI 理解代码最重要的上下文。

---

## 3.1 命名原则

禁止：

* 缩写
* 自造术语
* 模糊表达

命名应直接体现业务意图。

---

## 3.2 函数命名

格式：

```text
动词 + 名词
```

示例：

```ts
createOrder()

calculatePrice()

validateUser()

generateReport()
```

---

## 3.3 布尔变量

格式：

```text
is
has
should
can
```

示例：

```ts
isPaid

hasStock

shouldRetry

canDelete
```

---

## 3.4 普通变量

使用业务语义。

```ts
totalAmount

retryCount

paymentMethod

deliveryAddress
```

---

## 3.5 事件命名

使用过去式。

```ts
OrderCreated

OrderCanceled

PaymentFailed
```

---

## 3.6 禁止命名

```ts
data
info
tmp
flag
obj
res
result1
testData
```

---

# 四、类型与接口规范

类型是 AI 理解系统结构的导航地图。

---

## 4.1 禁止 Any

禁止：

```ts
any
```

必须使用：

```ts
unknown
```

或明确类型。

---

## 4.2 公共 API 显式声明

### 正例

```ts
function calculateTotal(
  items: CartItem[]
): number
```

---

### 反例

```ts
function calculateTotal(items) {
}
```

---

## 4.3 输入集中

优先使用对象参数。

### 推荐

```ts
createOrder({
  userId,
  items,
  couponCode
});
```

---

### 避免

```ts
createOrder(
  userId,
  items,
  couponCode
);
```

---

## 4.4 输出明确

禁止返回不确定结构。

### 推荐

```ts
type CreateOrderResult
```

---

## 4.5 Schema 优先

优先使用：

* Zod
* Joi
* JSON Schema

统一输入校验。

示例：

```ts
export const CreateOrderSchema =
  z.object({
    userId: z.string(),
    items: z.array(
      z.object({
        skuId: z.string(),
        qty: z.number()
          .int()
          .positive()
      })
    )
  });
```

---

# 五、错误处理规范

错误处理是 AI 最容易失控的区域。

---

## 5.1 禁止吞异常

禁止：

```ts
try {
}
catch {
}
```

---

## 5.2 禁止字符串异常

禁止：

```ts
throw "error";
```

---

## 5.3 使用结构化错误

```ts
throw new BusinessError({
  code: "ORDER_NOT_FOUND",
  message: "订单不存在"
});
```

---

## 5.4 错误分类

| 类型     | 处理方式  |
| ------ | ----- |
| 用户输入错误 | 4xx   |
| 权限错误   | 403   |
| 资源不存在  | 404   |
| 业务规则失败 | 业务错误码 |
| 系统异常   | 日志+告警 |

---