# 14 · JMM 与 volatile

> 状态：✅ 讲完了　·　最后更新：2026-10-12
> 代码：[`JmmDemo.java`](../src/main/java/dev/javalearning/concurrency/jmm/JmmDemo.java) ·
> [`Singleton.java`](../src/main/java/dev/javalearning/concurrency/jmm/Singleton.java) ·
> 断言：[`JmmTest.java`](../src/test/java/dev/javalearning/concurrency/jmm/JmmTest.java)（9 条）

上一课讲线程安全时，我们说"给 `running` 加上 `volatile` 就正常了"——
但没说**为什么**。

这一课就来回答那个问题：**为什么一个线程改了值，另一个线程看不见？**

先看全貌（这一课的输出分两种运行方式，下面会解释）：

```terminal
$ java src/main/java/dev/javalearning/concurrency/jmm/JmmDemo.java

========== 1. 可见性：一个线程改了值，另一个线程看不见 ==========
  主线程把 running 设成了 false
  等了 3 秒，counter 还活着吗 → true
  ← 它永远看不到 running 变成了 false（JIT 把读优化掉了）
  ← 这就是可见性问题：一个线程改了值，另一个线程看不见

========== 2. volatile 保证可见性：同样的代码，加了就正常 ==========
  主线程把 volatileRunning 设成了 false
  counter 线程退出了
  等了 3 秒，counter 还活着吗 → false

========== 3. volatile 不保证原子性：两个线程各加 100 万 ==========
  期望 2000000（两个线程各加 100 万）
  10 轮里有任何一轮达到期望吗 → false
  ← 加了 volatile 依然丢更新，因为它只管可见性不管原子性

========== 4. 锁规则：synchronized 也能保证可见性 ==========
  writer 在锁里写了 lockedValue = 42（它不是 volatile）
  reader 在同一个锁里读到 → 42
  ← 解锁 happens-before 后续加锁，所以看得见

========== 5. volatile 的正确用法：状态标志位 ==========
  worker 看到标志位变成 false，干净退出
  还活着吗 → false

========== 6. 双重检查锁定单例：instance 为什么必须是 volatile ==========
第一次拿: Singleton[初始化完成]
第二次拿: Singleton[初始化完成]
是同一个对象吗 → true

========== 7. happens-before：线程启动和 join ==========
  start 之前写的，子线程看到了，join 之后主线程也看到了
```

第 1 节和第 3 节是重点。

---

## 一、先让代码疼一次

场景很简单：一个线程在循环里等一个标志位，另一个线程负责把标志位关掉。

```java
static boolean running = true;      // 注意：没有 volatile

// 线程 1
while (running) {
    i++;
}

// 线程 2
Thread.sleep(1000);
running = false;                    // 关掉它
```

按直觉，一秒后线程 1 就该退出了。实际跑一下：

```terminal
========== 1. 可见性：一个线程改了值，另一个线程看不见 ==========
  主线程把 running 设成了 false
  等了 3 秒，counter 还活着吗 → true
```

**线程 1 永远停不下来。** 主线程明明已经把 `running` 设成 `false` 了。

### 1.1 而且这跟 JIT 优化有关（一个真实对比）

这段代码**不是每次都这样**。换一种运行方式就正常了：

```terminal
$ java -Xint src/main/java/dev/javalearning/concurrency/jmm/JmmDemo.java nojit

========== 1. 可见性（-Xint，不做 JIT 优化） ==========
  主线程把 running 设成了 false
  counter 线程退出了
  等了 3 秒，counter 还活着吗 → false
  ← 这次它看见了（nojit 模式下没有把读提升到循环外）
```

`-Xint` 让 JVM 用解释执行、不做 JIT 编译。同样一份源码，两种结果：

| 运行方式 | 结果 |
|---|---|
| 默认（JIT 编译） | 线程**永远不退出** |
| `-Xint`（不优化） | 线程正常退出 |

**这就是并发 bug 最恶劣的形态**：同一份代码，行为取决于 JVM 怎么优化它。
你在测试环境跑一百次都是对的，生产环境偶发一次。

---

## 二、正解：JMM（Java 内存模型）

要理解上面那件事，得先知道 JMM 规定的抽象结构。

### 2.1 主内存 + 工作内存

```
        主内存（所有线程共享）
        ┌──────────────────┐
        │  running = true  │
        └────────┬─────────┘
           ↑          ↑
      拷贝 │          │ 拷贝
           │          │
   ┌───────┴───┐  ┌───┴───────┐
   │ 线程1 工作 │  │ 线程2 工作 │
   │ 内存      │  │ 内存      │
   │ running=true│ │running=true│
   └───────────┘  └───────────┘
```

- **主内存**（main memory）：所有线程共享，变量真正待的地方
- **工作内存**（working memory）：每个线程私有的一份拷贝

规则是：

- 线程**读**变量：先从主内存拷贝到自己的工作内存，之后读自己的工作内存
- 线程**写**变量：先写自己的工作内存，什么时候同步回主内存**不确定**

打个比方：主内存是办公室的白板，工作内存是每个人自己的小本本。
你在自己本子上改了数字，没抄到白板上，别人当然看不见。

于是线程 2 把 `running = false` 写进了**自己的小本本**，
线程 1 一直读**自己的小本本**，那里还是 `true`。

### 2.2 一个重要澄清

**JMM 是抽象模型，不是真实的 CPU 缓存架构。**

它规定的是"允许出现什么行为"，不是"物理上怎么实现"。
真实机器上可能是 CPU 缓存、可能是寄存器、可能是编译器把变量提到循环外
（我们上面看到的就是最后这种）——JMM 不关心细节，只关心：
**在多线程下，哪些结果是合法的。**

这也解释了为什么加 `-Xint` 就正常了：不做 JIT 优化，
就没有"把读提升到循环外"这回事，每次循环都真的去读一次。

---

## 三、volatile：两件事，不是一件

给 `running` 加上 `volatile`：

```java
static volatile boolean running = true;
```

```terminal
========== 2. volatile 保证可见性：同样的代码，加了就正常 ==========
  主线程把 volatileRunning 设成了 false
  counter 线程退出了
  等了 3 秒，counter 还活着吗 → false
```

**稳定退出。** `volatile` 做了两件事：

### 3.1 保证可见性

- 写 `volatile` 变量时，**立刻刷回主内存**
- 读 `volatile` 变量时，**强制从主内存读**，不用自己缓存里的旧值

### 3.2 禁止指令重排序

在 `volatile` 读写的前后插入**内存屏障**（Memory Barrier），
阻止编译器和 CPU 把指令跨过这道栅栏重排。

屏障有四种（不需要背，理解"一道不能跨越的栅栏"就够）：

| 屏障 | 作用 |
|---|---|
| LoadLoad | 前面的读先于后面的读 |
| StoreStore | 前面的写先于后面的写 |
| LoadStore | 前面的读先于后面的写 |
| StoreLoad | 前面的写先于后面的读（最贵的一种） |

### 3.3 但它**不**保证原子性

这是最常见的误解。看第 3 节：

```terminal
========== 3. volatile 不保证原子性：两个线程各加 100 万 ==========
  期望 2000000（两个线程各加 100 万）
  10 轮里有任何一轮达到期望吗 → false
  ← 加了 volatile 依然丢更新，因为它只管可见性不管原子性
```

`volatile int count; count++;` **照样是错的**。因为 `count++` 还是"读-改-写"三步，
`volatile` 只保证"读到的不是旧值"，管不了"两步之间被别人插进来"。

**一句话记住**：

> `volatile` 管的是**看得见**，不是**不被打断**。

要原子性，用第 13 课讲的 `synchronized`，或者用原子类（`AtomicInteger`，
它的断言就在本课测试里，用 CAS 实现）。

---

## 四、happens-before：JMM 的核心规则

前面说的都是现象，`happens-before` 才是 JMM 给出的**判断依据**。

规则形式是：

> 如果 A **happens-before** B，那么 A 的执行结果对 B 可见。

几条主要的：

| 规则 | 内容 |
|---|---|
| **程序顺序** | 同一线程内，前面的语句 hb 后面的语句 |
| **监视器锁** | 解锁 hb 后续对**同一把锁**的加锁 |
| **volatile 变量** | 对 volatile 变量的写 hb 后续对它的读 |
| **线程启动** | `Thread.start()` hb 新线程里的所有操作 |
| **线程终止** | 线程里的所有操作 hb 其他线程从 `join()` 返回 |
| **传递性** | A hb B，B hb C，则 A hb C |

### 4.1 锁规则：`synchronized` 也保证可见性

这条最容易忽略——**锁不只保护原子性，也保证可见性**。
第 4 节用一个**不是 `volatile`** 的普通变量证明了这点：

```terminal
========== 4. 锁规则：synchronized 也能保证可见性 ==========
  writer 在锁里写了 lockedValue = 42（它不是 volatile）
  reader 在同一个锁里读到 → 42
  ← 解锁 happens-before 后续加锁，所以看得见
```

writer 在锁里写，reader 在**同一把锁**里读，就一定能读到最新值。
所以 `synchronized` 是"三性全包"的武器——原子性、可见性、有序性都管。

### 4.2 启动与终止规则

这两条解释了为什么 `join()` 之后能安全读子线程写的数据：

```terminal
========== 7. happens-before：线程启动和 join ==========
  start 之前写的，子线程看到了，join 之后主线程也看到了
```

- `start()` 之前的写，子线程一定看得见
- 子线程里的一切，`join()` 返回之后主线程一定看得见

**这解释了第 12 课一个细节**：为什么 `join()` 之后读结果总是对的。
不是巧合，是 JMM 的保证。

---

## 五、volatile 的正确用法

### 5.1 状态标志位（最典型）

一个线程写、其他线程读的开关，就是 `volatile` 的主场：

```terminal
========== 5. volatile 的正确用法：状态标志位 ==========
  worker 看到标志位变成 false，干净退出
  还活着吗 → false
```

### 5.2 双重检查锁定单例（DCL）

这个例子很经典，能说清"禁止重排序"到底在防什么：

```java
public final class Singleton {
    private static volatile Singleton instance;   // ← volatile 不能省

    public static Singleton getInstance() {
        if (instance == null) {                   // 第一次检查：不加锁，图快
            synchronized (Singleton.class) {
                if (instance == null) {           // 第二次检查
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
```

**为什么 `instance` 必须是 `volatile`？**

`new Singleton()` 至少三步：

```
① 分配内存
② 初始化对象
③ 把引用赋给 instance
```

没有 `volatile` 时，② 和 ③ **可能被重排**成 ①→③→②：

```
线程 A：① 分配内存 → ③ instance 指向这块内存（但还没初始化完）
线程 B：第一次检查发现 instance != null → 直接返回 → 拿到一个半成品对象
```

`volatile` 禁止这种重排，保证"看到 `instance` 非 null 时，对象一定初始化完了"。

```terminal
========== 6. 双重检查锁定单例：instance 为什么必须是 volatile ==========
第一次拿: Singleton[初始化完成]
第二次拿: Singleton[初始化完成]
是同一个对象吗 → true
```

### 5.3 不适合用 volatile 的场景

- **复合操作**（`count++`）——它只管单个变量的读写
- **需要多个变量保持一致**——`volatile` 一次只管一个变量，
  两个变量之间没有原子性

---

## 六、final 的内存语义（顺带说清）

第 04 课讲过 `final` 字段造好不能改。它还有个内存语义：

> **正确构造的对象**（构造期间没把 `this` 逸出），
> 它的 `final` 字段对其他线程**一定可见**，不需要额外同步。

这就是"不可变对象天然线程安全"的基础——`String` 为什么能放心共享，
靠的就是这个。反过来，如果你在构造器里就把 `this` 交给别的线程
（比如 `new Thread(this).start()`），这个保证就失效了。

---

## 七、不推荐的用法

- **别用 `volatile` 代替锁做复合操作**。`volatile int count; count++` 是错的，
  这是本课最想让你记住的一条
- **别以为 `volatile` 能解决一切可见性问题**。它只管一个变量，
  多个变量的一致性还得靠锁
- **别在 DCL 单例里漏掉 `volatile`**。这个 bug 极其难查——
  它只在特定时机、特定 JIT 优化下才出现
- **别用共享可变状态**。最省事的办法是不共享：用局部变量、
  用不可变对象、用线程自己的数据（`ThreadLocal`，后面会讲）
- **别指望"我测不出来就是没问题"**。本课第 1 节那段代码，
  换个 JVM 参数行为就变了

---

## 八、这一课的术语表

- **JMM**（Java Memory Model）—— 规定多线程下哪些行为合法的抽象模型
- **主内存 / 工作内存**（main memory / working memory）—— 共享的 vs 线程私有的
- **可见性**（visibility）—— 一个线程的写，另一个线程能不能看见
- **`volatile`** —— 保证可见性 + 禁止重排序，**不保证原子性**
- **内存屏障**（memory barrier / fence）—— 阻止指令跨过它重排
- **happens-before** —— JMM 的核心规则：A hb B 则 A 的结果对 B 可见
- **指令重排序**（instruction reordering）—— 编译器和 CPU 为性能调整指令顺序
- **DCL**（Double-Checked Locking）—— 双重检查锁定，单例的经典写法
- **原子性**（atomicity）—— 操作不可分割；`volatile` 不提供，锁和原子类提供
- **`final` 字段的内存语义**—— 正确构造的对象，其 final 字段对其他线程可见

---

## 九、下一课

到这里，并发问题的三个根源（原子性、可见性、有序性）和两个基础武器
（`synchronized`、`volatile`）都讲完了。

但 `synchronized` 有个局限：**它只会傻等**。拿不到锁就一直阻塞，
既不能"等一会儿就走"，也不能"等某个条件成立再继续"。

下一课讲更灵活的锁——`ReentrantLock`，以及它带来的三样新能力：
`tryLock()` 不干等、公平锁、以及一个锁配多个等待队列的 `Condition`。

下期再见。
