# java-learning

从零开始学 Java 的公开笔记。每一课都按「**先让代码疼一次，再给解法**」的顺序写，
代码块后面紧跟**真实的执行结果**——读起来应该像看视频，而不是读 API 文档。

**19 课全部完成**（00 ~ 18）· **359 条断言全绿** · 11387 行讲解 · 39 个反面教材

---

## 学习路线

四个阶段，顺序是刻意排过的——每一课都踩在上一课的知识上。

### 第一阶段：程序是个什么东西

| 课 | 主题 | 解决的困惑 |
|---|---|---|
| [00](docs/00-程序是怎么跑起来的.md) | 程序是怎么跑起来的 | 我写的代码，电脑为什么不认识 |

### 第二阶段：Java 的地基

| 课 | 主题 | 解决的困惑 |
|---|---|---|
| [01](docs/01-变量与类型.md) | 变量与类型 | 变量到底是个什么；为什么 `7/2` 是 3 |
| [02](docs/02-运算符与流程控制.md) | 运算符与流程控制 | `&&` 为什么会短路；for-each 里删元素为什么有时炸 |
| [03](docs/03-方法.md) | 方法 | 为什么改了参数外面没反应；重载和可变参数 |
| [04](docs/04-类与对象.md) | 类与对象 | 类到底是个什么；`static` 属于谁；为什么 toString 打印出来是身份证号 |
| [05](docs/05-继承与多态.md) | 继承与多态 | 多态是什么；static 为什么被隐藏；equals 和 hashCode 的契约 |

### 第三阶段：写起来更省力的工具

| 课 | 主题 | 解决的困惑 |
|---|---|---|
| [06](docs/06-泛型.md) | 泛型 | 一个类处理所有类型，不用为 String 再抄一份 |
| [07](docs/07-Lambda表达式.md) | Lambda | 不用为了发一条消息特意建一个类 |
| [08](docs/08-Optional.md) | Optional | 让 `null` 在类型上就看出来，而不是运行时才炸 |
| [09](docs/09-反射.md) | 反射 | 运行时才知道类名，怎么创建对象、调方法 |
| [10](docs/10-Stream.md) | Stream | 用声明式取代 for 循环 |
| [11](docs/11-并行流.md) | 并行流 | **第一次写多线程**：为什么结果是乱序的 |

### 第四阶段：多线程

| 课 | 主题 | 解决的困惑 |
|---|---|---|
| [12](docs/12-多线程基础.md) | 多线程基础 | 线程创建、生命周期、守护线程、中断 |
| [13](docs/13-线程安全与同步.md) | 线程安全与同步 | 为什么加了锁就对了 |
| [14](docs/14-JMM与volatile.md) | JMM 与 volatile | 为什么改了值对方看不见 |
| [15](docs/15-锁.md) | 锁 | `tryLock`、公平锁、`Condition`、死锁 |
| [16](docs/16-线程通信.md) | 线程通信 | 两个线程怎么互相配合 |
| [17](docs/17-线程池.md) | 线程池 | 七个参数；为什么不用 `Executors` |
| [18](docs/18-并发集合与ThreadLocal.md) | 并发集合与 ThreadLocal | `ConcurrentHashMap`、内存泄漏 |

---

## 怎么跑

```bash
# 单文件直接跑，不需要构建（JDK 11+）
java src/main/java/dev/javalearning/intro/Hello.java
java src/main/java/dev/javalearning/concurrency/safety/SafetyDemo.java

# 先编译再跑
javac -encoding UTF-8 -d out src/main/java/dev/javalearning/intro/Hello.java
java -cp out dev.javalearning.intro.Hello

# 跑某一课的断言
./gradlew test --tests '*variables*'

# 跑全部断言（359 条）
./gradlew test
```

环境：JDK 25 · Gradle 9.8 · JUnit 6 · AssertJ。依赖已在本机缓存，可离线构建
（`./gradlew test --offline`）。

---

## 代码结构

代码**按主题分包**，不按课时——复习时直接找主题，不用去翻是第几课。
学习顺序由 [`docs/README.md`](docs/README.md) 的路线图保证，两件事互不干扰。

```
src/main/java/dev/javalearning/
├── intro/                    第 00 课：程序是怎么跑起来的
├── basics/
│   ├── variables/            第 01 课：变量与类型
│   ├── controlflow/          第 02 课：运算符与流程控制
│   ├── methods/              第 03 课：方法
│   ├── classes/              第 04 课：类与对象
│   └── inheritance/          第 05 课：继承与多态
├── generics/                 第 06 课：泛型
├── lambda/                   第 07 课：Lambda
├── optional/                 第 08 课：Optional
├── reflection/               第 09 课：反射
├── stream/                   第 10 ~ 11 课：Stream 与并行流
└── concurrency/              第 12 ~ 18 课：多线程
    ├── threads/              多线程基础
    ├── safety/               线程安全与同步
    ├── jmm/                  JMM 与 volatile
    ├── locks/                锁
    ├── coordination/         线程通信
    ├── pool/                 线程池
    └── collections/          并发集合与 ThreadLocal

src/test/java/dev/javalearning/   ← 与 main 的包结构严格一一对应
```

**测试包必须和被测试的类在同一个包名下**（Java 硬规范）。这样
`package-private` 的成员也能被测到，而且看一眼包路径就知道这段测试测的是谁。

---

## 这个仓库的三条硬规矩

### 1. 文档里的执行结果必须真跑出来

不许凭记忆写、不许"应该是这样"。这是最硬的一条。

`scripts/verify-doc-output.py` 会把每课的主 demo 跑一遍，和文档里贴的
输出**逐字比对**。改了 demo 忘了回去改文档，CI 会红：

```bash
python3 scripts/verify-doc-output.py                 # 全部
python3 scripts/verify-doc-output.py --stable-only   # 跳过天生不稳定的（CI 用）
```

**为什么有 `--stable-only`**：并行流和竞态演示的输出天生不稳定，
它们的文档里已经注明"每次运行可能不同"。让它们进 CI 只会随机变红。

### 2. 竞态演示不能断言"这一轮一定出错"

竞态是**概率性**的。第 13 课我写过一版不加锁的售票程序，它**根本不超卖**——
因为"检查"和"扣减"只隔一条指令，几乎撞不上。加了个小间距（模拟真实业务耗时）
才复现出来。

所以这类 demo 的写法是「**循环直到观测到出错**」，而不是"断言第 N 轮错"。
第 13、14 课都用了这个模式，输出因此是稳定的。

### 3. 反面教材分两类放

| 类型 | 放哪 | 为什么 |
|---|---|---|
| **编译不过**的 | `src/main/<包>/broken/` | `build.gradle.kts` 整包排除，IDE 里标红是正常的 |
| **编译得过但行为错**的 | `src/test/<包>/` | 必须能被测试引用，才能把错误行为钉住 |

目前有 **39 个**反面教材。它们不是凑数——都是真跑出来、贴了真实报错的。
比如：

- `Box<int>` 用基本类型 → 真实编译错误
- `List<String>` 赋给 `List<Object>` → 真实编译错误
- 只重写 `equals` 漏 `hashCode` → 编译通过、运行不报错，`HashSet` 静默留下 3 份重复
- `synchronized (new Object())` → 编译通过、运行不报错，锁等于没锁

---

## CI 有三道校验

```yaml
- python3 scripts/check-links.py                        # 链接 + 路线图勾选状态
- python3 scripts/verify-doc-output.py --stable-only    # 文档输出和真实运行逐字一致
- ./gradlew test                                        # 359 条断言
```

第一条还会检查一件容易漏的事：**路线图的 ✅ 和 `docs/` 里实际有哪些文件是否对得上**。
新课写完了忘了打勾、或者打了勾却没写文档，都会被抓出来。

---

## 写笔记的约定

- 引用仓库里的文件用**相对链接**，链接文本只写文件名：
  `` [`Box.java`](src/main/java/dev/javalearning/generics/Box.java) ``
- **不要带 `#锚点`**——中文标题的锚点在不同渲染器（GitHub / VS Code）里规则不一样
- **跨课引用不要写「第 06 课」这种编号**，读者得先记住 06 是什么。
  直接写主题做成链接：`[继承与多态](docs/05-继承与多态.md)`
- 每课的结构：先让代码疼一次 → 正解 → 等价写法与边界 → 不推荐的用法 → 术语表 → 下期再见
- 方法名用英文，`@DisplayName` 用中文
- `docs/` 讲完一课才建一个文件，不留空占位

---

## 后面还可以写

这一版讲完了从语法到并发的地基。再往上是这些（都建立在现有内容之上，没有全新概念）：

- `CompletableFuture` —— 异步编排
- `ReentrantReadWriteLock` / `StampedLock` —— 读写分离的锁
- `AQS` 源码 —— `ReentrantLock`、`CountDownLatch` 的共同底座
- `ForkJoinPool` —— 并行流背后的那个池
- 虚拟线程（JDK 21+）—— 线程模型的新一代答案

想加哪一课，照着现有 19 课的结构写就行——两个校验脚本会告诉你有没有漏。
