# 18 · 并发集合与 ThreadLocal

> 状态：✅ 讲完了　·　最后更新：2026-10-12
> 代码：[`CollectionsDemo.java`](../src/main/java/dev/javalearning/concurrency/collections/CollectionsDemo.java) ·
> [`ThreadLocalDemo.java`](../src/main/java/dev/javalearning/concurrency/collections/ThreadLocalDemo.java) ·
> 断言：[`CollectionsTest.java`](../src/test/java/dev/javalearning/concurrency/collections/CollectionsTest.java)（16 条）

最后一课。前面十七课把并发的机制讲完了，这一课补上两块**日常最常用**的拼图：
**并发集合**（怎么在多线程下安全地用容器）和 **`ThreadLocal`**
（怎么让每个线程有自己的一份数据）。

先看集合部分的全貌：

```terminal
$ java src/main/java/dev/javalearning/concurrency/collections/CollectionsDemo.java

========== 1. HashMap 多线程 put：元素会丢 ==========
  期望 40000 个元素，跑 10 轮
  出现过元素丢失吗 → true
  ← HashMap 不是线程安全的，并发 put 会互相覆盖
  ← 丢多少个、哪几轮丢，每次运行都不同，这正是竞态的特征

========== 2. ConcurrentHashMap 同样操作：一个都不丢 ==========
  期望 40000 个元素，跑 10 轮
  出现过元素丢失吗 → false
  一个都没丢吗 → true

========== 3. synchronizedMap 的 check-then-act 依然不是原子的 ==========
  两个线程都想「如果 a 还是 0 就写」，跑 200 轮
  出现「双方都写了」的轮数 → 200 / 200
  ← 单次方法是原子的，但「判断 + 写入」合起来不是——这就是 check-then-act
  ← 谁最后赢是随机的，所以这里只统计「有没有发生覆盖」

========== 4. ConcurrentHashMap 的复合操作是原子的 ==========
  computeIfAbsent 的映射函数一共被调用了 1 次
  k 有值吗 → true
  ← 整个「检查 + 计算 + 放入」是原子的，所以映射函数只会成功执行一次
  ← 至于 k 是 1 还是 2，取决于哪个线程先到，每次可能不同
  merge 之后 count = 11
  putIfAbsent 已存在时不覆盖 → 11
  replace 只在值匹配时才换 → true
  最终 count = 100

========== 5. ArrayList 迭代中修改 → ConcurrentModificationException ==========
  ConcurrentModificationException
  ← fail-fast：宁可立刻报错，也不给你一个不确定的结果

========== 6. ConcurrentHashMap 的迭代器是弱一致的，不抛异常 ==========
  迭代中修改 → 没抛异常
  ← 弱一致（weakly consistent）：不保证看到最新修改，但也不会抛异常
  最终内容里有 c 吗 → true

========== 7. CopyOnWriteArrayList：迭代器看到的是旧快照 ==========
  迭代器看到的 → 1 2 3
  集合的实际内容 → [1, 2, 3, 4]
  ← 迭代器拿到的是「创建它那一刻」的快照，之后的新增看不到
  ← 适合「读多写极少」的场景，写一次复制整个数组，很贵
```

---

## 一、先让代码疼一次：`HashMap` 会丢数据

`HashMap` 是日常用得最多的集合，但它**不是线程安全的**。
多线程同时 `put`，元素会悄悄消失：

```terminal
========== 1. HashMap 多线程 put：元素会丢 ==========
  期望 40000 个元素，跑 10 轮
  出现过元素丢失吗 → true
```

**注意这个输出里没有具体数字**——我一开始想打印"最少的一轮只剩 2 万多个"，
结果发现那个数字每次运行都不一样（2 万 5、2 万 9、3 万 5……）。
这恰恰是竞态的特征：**结果不确定**。所以我只保留"丢没丢"这个稳定结论。

换成 `ConcurrentHashMap`，同样四个线程各放一万个：

```terminal
========== 2. ConcurrentHashMap 同样操作：一个都不丢 ==========
  出现过元素丢失吗 → false
  一个都不丢吗 → true
```

---

## 二、`synchronizedMap` 为什么还是不够

既然 `HashMap` 不安全，那用 `Collections.synchronizedMap()` 包一层行不行？

```java
Map<String, Integer> sync = Collections.synchronizedMap(new HashMap<>());
```

它确实把**每个方法**都加上了锁，单次 `get` / `put` 是安全的。
但**复合操作**依然不是原子的：

```java
if (sync.get("a") == 0) {     // ① 判断
    sync.put("a", 1);          // ② 写入
}
```

① 和 ② 之间可能被别的线程插进来。这叫 **check-then-act**（先检查再操作）。
实测结果：

```terminal
========== 3. synchronizedMap 的 check-then-act 依然不是原子的 ==========
  两个线程都想「如果 a 还是 0 就写」，跑 200 轮
  出现「双方都写了」的轮数 → 200 / 200
  ← 单次方法是原子的，但「判断 + 写入」合起来不是——这就是 check-then-act
```

**200 轮里 200 轮都发生了覆盖。** 两个线程都通过了检查，都以为自己写了。

### 2.1 `ConcurrentHashMap` 给了原子的复合操作

`ConcurrentHashMap` 不只是"加了锁的 HashMap"，它提供了一组
**本身就是原子**的复合操作：

| 方法 | 语义 |
|---|---|
| `computeIfAbsent(k, fn)` | 不存在才计算并放入 |
| `computeIfPresent(k, fn)` | 存在才计算并更新 |
| `compute(k, fn)` | 不管存不存在都计算 |
| `merge(k, v, fn)` | 不存在就放 v，存在就合并 |
| `putIfAbsent(k, v)` | 不存在才放 |
| `replace(k, old, new)` | 值匹配才替换 |

```terminal
========== 4. ConcurrentHashMap 的复合操作是原子的 ==========
  computeIfAbsent 的映射函数一共被调用了 1 次
  k 有值吗 → true
```

**映射函数只被调用了 1 次**——如果两个线程都执行了映射函数，
就说明不是原子的。这就是原子性的直接证据。

> 顺带说一个细节：`ConcurrentHashMap` **不允许 null 键和 null 值**，
> 而 `HashMap` 允许。这是故意的——并发下 `get` 返回 `null` 到底是
> "没有这个键"还是"值就是 null"，没法区分，干脆禁止。

### 2.2 为什么 `ConcurrentHashMap` 比 `synchronizedMap` 快

`synchronizedMap` 是**一把大锁锁住整个表**，所有操作串行。

`ConcurrentHashMap`（JDK 8 之后）用的是
**CAS + synchronized 锁单个桶（bucket）**：只有落到同一个桶的线程才互相等，
不同桶完全并行。读操作甚至基本不用锁（`volatile` 读）。

---

## 三、三种迭代器行为

这是实战里很容易踩的一类问题：**边遍历边改**。

### 3.1 `fail-fast`：立刻报错

`ArrayList`、`HashMap` 这些普通集合的迭代器是 **fail-fast** 的：

```terminal
========== 5. ArrayList 迭代中修改 → ConcurrentModificationException ==========
  ConcurrentModificationException
  ← fail-fast：宁可立刻报错，也不给你一个不确定的结果
```

原理在[流程控制](./02-运算符与流程控制.md)那课讲过：内部有个 `modCount`，
迭代器每次 `next()` 对一次账，账不平就抛。

**这是好事**——它把"结果不确定"变成了"立刻报错"。

### 3.2 弱一致：不抛异常，但不保证看到最新数据

`ConcurrentHashMap` 的迭代器是 **弱一致**（weakly consistent）的：

```terminal
========== 6. ConcurrentHashMap 的迭代器是弱一致的，不抛异常 ==========
  迭代中修改 → 没抛异常
  ← 弱一致（weakly consistent）：不保证看到最新修改，但也不会抛异常
```

它遍历的是"某一时刻的近似视图"，不保证包含迭代开始后的所有修改，
但也不会抛异常。适合"统计一下当前大概有多少"这类场景。

### 3.3 写时复制：迭代器拿到快照

`CopyOnWriteArrayList` 每次**写**都复制整个数组：

```terminal
========== 7. CopyOnWriteArrayList：迭代器看到的是旧快照 ==========
  迭代器看到的 → 1 2 3
  集合的实际内容 → [1, 2, 3, 4]
```

迭代器是在 `add(4)` **之前**创建的，所以它看到的是旧快照。

| 集合 | 迭代器行为 | 适合场景 |
|---|---|---|
| `ArrayList` / `HashMap` | fail-fast，抛异常 | 单线程 |
| `ConcurrentHashMap` | 弱一致，不抛异常 | 高并发读写 |
| `CopyOnWriteArrayList` | 快照，不抛异常 | **读多写极少**（如监听器列表、配置） |

`CopyOnWriteArrayList` 写一次的代价是**复制整个数组**，
所以只适合写极少的场景。用它做"每读一次就写一次"的队列，性能会崩。

### 3.4 顺带认识 `BlockingQueue`

并发集合里还有一族特别重要的：`ArrayBlockingQueue`、
`LinkedBlockingQueue`、`PriorityBlockingQueue`、`SynchronousQueue` 等。

它们是**阻塞队列**——队列满时 `put` 会等，队列空时 `take` 会等。
[线程通信](./16-线程通信.md)那课我们手写的 `SimpleQueue` 就是它们的雏形，
而[线程池](./17-线程池.md)的 `workQueue` 参数就是这一族。

**生产上别自己手写阻塞队列**，用 JDK 的——它们的性能、公平性、
中断处理都经过大量验证。

---

## 四、`ThreadLocal`：每个线程自己一份

换个话题。有时候你不想"共享数据"，而是想"每个线程有自己的一份"。

最典型的场景：**链路追踪**。一个请求进来分配一个 `traceId`，
后面所有日志都要带上它。如果用参数传，那每一层方法都要多一个参数。

`ThreadLocal` 解决的就是这个——数据存在**线程自己身上**：

```terminal
$ java src/main/java/dev/javalearning/concurrency/collections/ThreadLocalDemo.java

========== 1. 每个线程自己一份：互相看不见 ==========
  子线程 get() → null（看不到主线程设的）
  子线程 set 之后 get() → 子线程的值
  主线程 get() → 主线程的值（没被子线程影响）
  ← 数据存在「线程自己身上」，所以天然线程安全

========== 2. withInitial：给一个默认值 ==========
  第一次 get() → 0（走的是 withInitial 给的默认值）
  加一之后 get() → 1

========== 3. 线程池复用 → 上一个任务的数据会留下来 ==========
  第二个任务读到的 → 用户A（上一个任务留下的！）
  ← 单线程的池一定会复用线程，所以残留是必然的

========== 4. 用完 remove：这是必须的 ==========
  remove 之后第二个任务读到 → null
  ← 规矩：谁 set 谁负责 remove，用完立刻清

========== 5. 为什么会内存泄漏：key 是弱引用，value 不是 ==========
  GC 之后，ThreadLocal 这个 key 还在吗 → false
  ← key 是弱引用，没有外部强引用时会被回收
  ← 但 value 是强引用，还挂在线程的 map 上 —— 这就是泄漏的由来
  ← 如果这个线程是池里的、永远不销毁，那 1MB 就一直占着

========== 6. ThreadLocal 在实战里长什么样：一次请求一个上下文 ==========
  alice 的请求，traceId = traceId-alice
  bob 的请求，traceId = traceId-bob
  carol 的请求，traceId = traceId-carol
  ← 这就是「链路追踪」「当前登录用户」的标准实现方式
  ← 好处：不用把 traceId 一层层当参数传下去
```

### 4.1 数据到底存在哪

`ThreadLocal` 本身**不存数据**。数据存在 `Thread` 对象里的一个
`ThreadLocalMap` 里：

```
Thread
  └── threadLocals: ThreadLocalMap
        ├── key = ThreadLocal 对象（弱引用）→ value = 你的数据（强引用）
        └── ...
```

所以 `threadLocal.get()` 实际是"拿到当前线程，查它自己的 map"。
这就是为什么每个线程看不到别人的值——**压根不是同一份数据**。

### 4.2 线程池的坑：数据会残留

这是 `ThreadLocal` 最需要注意的地方。线程池的线程是**复用的**，
如果上一个任务没清理，下一个任务就会读到：

```terminal
========== 3. 线程池复用 → 上一个任务的数据会留下来 ==========
  第二个任务读到的 → 用户A（上一个任务留下的！）
```

**在生产上这可能是严重的事故**：用户 A 的请求数据被用户 B 读到了。

修法只有一个：**用完必须 `remove()`**。

```terminal
========== 4. 用完 remove：这是必须的 ==========
  remove 之后第二个任务读到 → null
```

标准写法是 `try/finally`：

```java
CONTEXT.set(traceId);
try {
    doBusiness();
} finally {
    CONTEXT.remove();      // 不管成功失败都要清
}
```

### 4.3 内存泄漏到底是怎么发生的

第 5 节的输出：

```terminal
========== 5. 为什么会内存泄漏：key 是弱引用，value 不是 ==========
  GC 之后，ThreadLocal 这个 key 还在吗 → false
```

拆开看这个链条：

```
① ThreadLocalMap 的 key 是【弱引用】
   → 如果外部没有强引用了（比如 threadLocal 变量出了作用域），
     GC 时 key 会被回收

② 但 value 是【强引用】，还被这个 map 持有着
   → key 没了，value 还在

③ 这个 map 又挂在【线程】身上
   → 线程不销毁（线程池里的线程就是不销毁），value 就一直占着内存
```

所以泄漏的不是 `ThreadLocal` 对象本身，而是**它曾经关联的那个 value**。

JDK 在 `get` / `set` 时会顺手清理一些"key 为 null"的槽位，但这只是
"尽力而为"——如果你之后再也不碰这个 `ThreadLocal`，它就永远不会被清理。

**结论还是那一句**：`remove()` 不是可选项，是必选项。

### 4.4 跨线程传递怎么办

`ThreadLocal` 有个"缺陷"：**子线程拿不到父线程的值**（第 1 节就演示了）。

- `InheritableThreadLocal`：能让子线程继承父线程的值。
  但它**只在线程创建时复制一次**，线程池里线程早就创建好了，所以不管用
- 解决方案：用阿里的 `TransmittableThreadLocal`（TTL），
  或者手动在提交任务时包装一层（把上下文抓出来，在任务里再 set 进去）

这是进阶话题，知道有这么个坑就够了。

---

## 五、不推荐的用法

- **别在多线程下用 `HashMap` / `ArrayList`**。丢了数据不一定报错，
  可能只是数量少了一点
- **别以为 `Collections.synchronizedMap` 就万事大吉**。复合操作依然要自己加锁，
  或者换 `ConcurrentHashMap` 的原子方法
- **别用 `CopyOnWriteArrayList` 做写多的场景**。写一次复制整个数组
- **别在迭代中修改 fail-fast 集合**。要么用迭代器的 `remove`，
  要么用 `removeIf`（[流程控制](./02-运算符与流程控制.md)那课讲过）
- **别忘 `ThreadLocal.remove()`**。这是本课最重要的一条
- **别用 `ThreadLocal` 传参数**。它是给"横切关注点"（追踪、用户、事务）用的，
  业务参数还是该老老实实传
- **别把大对象放进 `ThreadLocal`**。线程池的线程活得比你的任务久，
  放个几百 MB 的东西进去，泄漏起来很壮观

---

## 六、这一课的术语表

- **`ConcurrentHashMap`** —— 线程安全的哈希表，CAS + 分桶锁
- **`Collections.synchronizedMap`** —— 用一把大锁包装普通 Map，复合操作仍不安全
- **check-then-act** —— "先判断再操作"，两步之间可能被插入，不是原子的
- **原子复合操作** —— `computeIfAbsent` / `merge` / `putIfAbsent` / `replace`
- **fail-fast** —— 迭代中修改立刻抛 `ConcurrentModificationException`
- **弱一致**（weakly consistent）—— 迭代器不抛异常，但不保证看到最新数据
- **写时复制**（copy-on-write）—— 写时复制整个数组，`CopyOnWriteArrayList`
- **阻塞队列**（BlockingQueue）—— 满时 `put` 等、空时 `take` 等的队列
- **`ThreadLocal`** —— 每个线程一份的变量，数据存在线程自己的 map 里
- **`ThreadLocalMap`** —— `Thread` 内部存 `ThreadLocal` 数据的结构
- **内存泄漏** —— `ThreadLocal` 的 key 是弱引用、value 是强引用，
  线程不销毁则 value 一直占着
- **`remove()`** —— 清理 `ThreadLocal`，用完必须调
- **`InheritableThreadLocal`** —— 让子线程继承父线程的值（只在线程创建时复制一次）
- **TTL**（TransmittableThreadLocal）—— 解决线程池下上下文传递的第三方方案

---

## 七、这个系列到这里就讲完了

回头看一下这条线：

| 阶段 | 课 | 解决了什么 |
|---|---|---|
| 地基 | 00 ~ 05 | 变量装什么、代码怎么走、方法怎么传值、类怎么组织、类怎么共享行为 |
| 工具 | 06 ~ 10 | 泛型、Lambda、Optional、反射、Stream |
| 并发 | 11 ~ 18 | 并行流、线程基础、线程安全、JMM、锁、通信、线程池、并发集合与 ThreadLocal |

你最开始说"始终无法掌握 Java 多线程"。如果现在回头看第 13 课那个丢更新的计数器，
你应该能一眼说出**它为什么会错、错在原子性还是可见性、该怎么修、修完代价是什么**——
那就说明这条线走通了。

剩下没讲的还有不少（`CompletableFuture`、`ForkJoinPool`、
`ReentrantReadWriteLock`、`StampedLock`、`AQS` 源码、`Disruptor`……），
但它们都是这套地基上的组合，不再有全新的概念。

**推荐的自测方式**：打开任意一课，只看标题，把里面的代码和结论默写一遍。
写不出来的那一课，回去重看。

谢谢观看。欢迎关注订阅我的频道。下期再见。
