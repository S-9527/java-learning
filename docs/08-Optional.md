# 08 · Optional

> 状态：✅ 讲完了　·　最后更新：2026-10-13
> 代码：[`User.java`](../src/main/java/dev/javalearning/optional/User.java) ·
> [`UserRepository.java`](../src/main/java/dev/javalearning/optional/UserRepository.java) ·
> [`OptionalDemo.java`](../src/main/java/dev/javalearning/optional/OptionalDemo.java) ·
> 断言：[`OptionalTest.java`](../src/test/java/dev/javalearning/optional/OptionalTest.java)（27 条）

写业务代码的人，十个有九个被 `NullPointerException` 咬过。被咬之后学到的第一招是
"用之前先判空"，于是代码里开始出现一层又一层的 `if (x != null)`。

判空本身没错，错在**这件事全靠人记**。方法签名上写着 `User findUserByName(String)`，
它到底会不会返回 `null`？看不出来。你只能去翻它的实现，或者赌一把，赌输了就是线上一个 NPE。

这一课要讲的是一个把"可能没有"**写进类型里**的类：`Optional`。

它不是一个"更安全的 null"，也不是语法糖。它真正的价值是：**编译器开始帮你盯这件事**。
下面这句代码是编译不过的——

```terminal
$ javac -encoding UTF-8 -d out -sourcepath src/main/java \
    src/main/java/dev/javalearning/optional/broken/IgnoredOptional.java
src/main/java/dev/javalearning/optional/broken/IgnoredOptional.java:21: error: incompatible types: Optional<User> cannot be converted to User
        User user = repository.findByName("张三");   // ← 返回的是 Optional<User>
                                         ^
1 error
```

`Optional<User> cannot be converted to User`——想拿到里面的 `User`，你必须显式做点什么。
忘了处理空？编译器不让你过。这就是它和 `null` 最本质的区别。

先看全貌，后面一节一节拆：

```terminal
$ java src/main/java/dev/javalearning/optional/OptionalDemo.java

========== 1. 先让代码疼一次：查不到就是 null ==========
NullPointerException: Cannot invoke "dev.javalearning.optional.User.fullName()" because the return value of "dev.javalearning.optional.UserRepository.findUserByName(String)" is null
if (user != null) 挡住了：查无此人
一层套一层：user != null → nickname != null → 非空串，缩进越来越深
毛病不在「忘了判空」，在于返回类型上根本看不出可能没有

========== 2. Optional 出场：把「可能没有」写在返回类型上 ==========
findByName("张三") → Optional[张三 <zhangsan@example.com>]
findByName("赵六") → Optional.empty
两者的类型都是 Optional<User>，编译期就看得见「可能没有」
有值时：found.orElseThrow().fullName() = 张三

========== 3. 创建 Optional 的三种方式 ==========
Optional.empty()          → Optional.empty
Optional.of("张三")       → Optional[张三]
Optional.of(null)         → 抛 NullPointerException，message = null
Optional.ofNullable(null) → Optional.empty
Optional.ofNullable("张三") → Optional[张三]

of(null) 的异常 message 是 null——它自己也没告诉你「不该传 null」
所以拿不准的时候用 ofNullable，它永远不会因为你传 null 而炸

========== 4. isPresent() + get()：改了，但没真改 ==========
found.isPresent()   → true，get() 拿到 张三
missing.isPresent() → false
missing.isEmpty()   → true

写法和 if (user != null) 一模一样，只是把 null 换成了 isPresent()
这仍然是命令式编程——Optional 的正确用法是链式调用，见下面几节

========== 5. orElse 和 orElseGet：参数到底什么时候算 ==========
── 情况 A：有值，用 orElse ──
结果 = 小三，默认值被算了 1 次
── 情况 B：有值，用 orElseGet ──
结果 = 小三，supplier 被执行了 0 次
── 情况 C：没值，这次默认值真的用上了 ──
orElse    → 默认昵称，被算了 1 次
orElseGet → 默认昵称，被执行了 1 次

结论：有值的时候 orElse 也算了默认值（1 次），orElseGet 一次都没算（0 次）
如果默认值只是一句话，无所谓；如果它要查数据库、读文件，orElse 就白干了一次

========== 6. orElseThrow：查不到就抛自己的异常 ==========
有值时 orElseThrow() 直接拿到：张三
无参版没值 → java.util.NoSuchElementException: No value present
带 supplier → java.lang.IllegalArgumentException: 用户不存在：赵六

========== 7. ifPresent 和 ifPresentOrElse：有值才做事 ==========
ifPresent 命中：张三 <zhangsan@example.com>
ifPresentOrElse 命中：张三
ifPresentOrElse 落空：查无赵六
注意 ifPresent 是「有值才做」，它不返回新值；
想在两个分支里都产出结果，用 ifPresentOrElse 或者下面的 map

========== 8. map / flatMap / filter：在盒子里做转换 ==========
map 取名字长度：Optional[2]
李四没昵称，flatMap + map 链走完：Optional.empty
map(User::nickname)     → Optional[Optional[小三]]   ← 两层 Optional
flatMap(User::nickname) → Optional[小三]   ← 一层，我们要的就是这个
李四 map(User::nickname) → Optional[Optional.empty]   ← 空值也套两层
map(u -> (String) null) → Optional.empty   ← 返回 null 也只是 empty，不炸
flatMap(u -> null) → 抛 NullPointerException（map 不会）
张三 filter(邮箱是 @example.com)：Optional[张三 <zhangsan@example.com>]
钱七 filter(邮箱是 @example.com)：Optional.empty   ← 人找到了，但不满足条件，一样是 empty

口诀：转换后还是普通值 → map；转换后本身就是 Optional → flatMap

========== 9. stream()：0 个或 1 个元素的流 ==========
Optional.empty().stream().count()  = 0
Optional.of("张三").stream().count() = 1
3 个名字查出来 2 个邮箱：[zhangsan@example.com, wangwu@example.com]
赵六查不到，它的流是空的，forEach 什么都不做——不用写一个 if

========== 10. 不要用 get()：没值就抛异常 ==========
Optional.empty().get() → java.util.NoSuchElementException: No value present
异常类型是 NoSuchElementException，不是 NullPointerException
get() 不是不能用，但它把「可能没有」又变回了运行期才会发现的错误——
那和直接返回 null 有什么区别？只在已经确认有值的地方用。

========== 11. 不推荐的用法：能编译，但别这么写 ==========
① Optional 当字段：编译通过、运行正常 → Optional[小三]
② Optional 当方法参数：编译通过、运行正常 → 你好，小三
③ Optional 当构造器参数：编译通过、运行正常
④ Optional 包集合：Optional.of(List.of()) = Optional[[]]
   一个空列表已经是「没有元素」了，外面再套一层 Optional 是双重否定
⑤ isPresent() + get()：等于把 null 换了个名字，见第 4 节

这些编译器都不拦——所以只能靠你自己记住：
Optional 的主要用途是「方法的返回类型」，就这一条。
```

第 5 节和第 8 节是重点，我们慢慢看。

---

## 一、先让代码疼一次：查不到就是 null

假设我们在写一个用户系统。第一个版本长这样：

```java
public User findUserByName(String name) {
    for (User user : USERS) {
        if (user.fullName().equals(name)) {
            return user;
        }
    }
    return null;              // ← 查不到，返回 null
}
```

返回类型是 `User`。**"查不到"这个事实，在签名上一点都看不出来。** 调用方看到
`findUserByName` 只会想"给我一个 User"，然后顺手就这么写了：

```java
System.out.println(repository.findUserByName("赵六").fullName());
```

跑一下：

```terminal
========== 1. 先让代码疼一次：查不到就是 null ==========
NullPointerException: Cannot invoke "dev.javalearning.optional.User.fullName()" because the return value of "dev.javalearning.optional.UserRepository.findUserByName(String)" is null
```

注意这条 message 有多好——现代 JVM 会告诉你"是 `findUserByName(String)` 的返回值是 null"，
而不只是甩一行堆栈。**但再好的 message 也是运行期才有的**，代码已经上线了，
用户已经看到报错了。

（这种"有帮助的 NPE"是 JVM 的一个开关 `-XX:+ShowCodeDetailsInExceptionMessages`，
现代 JDK **默认打开**。我试着关掉它跑同一个例子，message 立刻退化成光秃秃的 `null`——
所以别把它当成理所当然，它只是一条更好看的错误信息，**该炸还是炸**。
[变量与类型](./01-变量与类型.md) 里那个 `Integer` 拆箱炸的 NPE，也属于同一类问题：
**类型上看着没问题，运行起来才知道是空。**）

### 1.1 于是开始判空

知道疼了之后，第一反应是加判断：

```java
User user = repository.findUserByName("赵六");
if (user != null) {
    System.out.println("昵称：" + user.nickname().orElse("（没填）"));
} else {
    System.out.println("if (user != null) 挡住了：查无此人");
}
```

```terminal
if (user != null) 挡住了：查无此人
```

能跑了。但问题没解决，只是被推迟了：

- **判空是"人肉约定"**。今天你记得判，明天换个人来调这个接口，他不知道要判
- **嵌套会失控**。真实业务里经常是"订单 → 客户 → 地址 → 城市"，
  每一层都可能没有，判空就得一层套一层，缩进越来越深：

  ```java
  if (order != null) {
      Customer c = order.customer();
      if (c != null) {
          Address a = c.address();
          if (a != null) {
              return a.city();
          }
      }
  }
  ```

- **最要命的是：`findUserByName` 和"一定查得到"的方法，签名长得一模一样**。
  编译器分不出这两者的区别，所以它帮不了你。

一句话总结这一节的毛病：

> 毛病不在「忘了判空」，在于**返回类型上根本看不出可能没有**。

---

## 二、正解：把「可能没有」写进返回类型

修法很直接：**让返回类型自己说出"可能查不到"**。

```java
public Optional<User> findByName(String name) {
    return Optional.ofNullable(findUserByName(name));   // 老方法返回 null，包一层
}

public Optional<User> findByEmail(String email) {
    for (User user : USERS) {
        if (user.email().equals(email)) {
            return Optional.of(user);
        }
    }
    return Optional.empty();                            // 明确表示「没有」
}
```

`Optional<User>` 这个类型读作"**可能装着一个 User，也可能是空的**"。
现在调用方看到签名，想装作看不见都不行了。

```terminal
========== 2. Optional 出场：把「可能没有」写在返回类型上 ==========
findByName("张三") → Optional[张三 <zhangsan@example.com>]
findByName("赵六") → Optional.empty
两者的类型都是 Optional<User>，编译期就看得见「可能没有」
```

注意 `findByName("张三")` 和 `findByName("赵六")` 的**类型完全一样**，都是
`Optional<User>`——这跟返回 `null` 的情况截然不同：`null` 是"一个 User 类型的空指针"，
类型系统根本不区分它和真正的 User。

`Optional` 常被比喻成一个**盒子**（container）：

```
Optional.of("张三")   →  ┌─────────┐
                         │  张三   │   ← 盒子里有东西
                         └─────────┘

Optional.empty()      →  ┌─────────┐
                         │         │   ← 盒子是空的，但它仍然是个盒子
                         └─────────┘
```

盒子这个比喻的关键在于：**你可以对一个空盒子做操作，但你不能对 `null` 做操作**。
`null.fullName()` 直接炸；而 `Optional.empty().map(User::fullName)` 安安静静地还是
一个空盒子。整个 Optional 的 API 都是围着这个性质设计的。

### 2.1 为什么它比 `null` 强

| | 返回 `null` | 返回 `Optional` |
|---|---|---|
| 签名能不能看出可能没有 | 看不出来 | 一眼看出 |
| 忘了处理会怎样 | 运行期 NPE | **编译期报错** |
| 默认值 / 转换 | 得手写 `if` | `orElse` / `map` 链式表达 |
| 别人接手你的代码 | 靠猜、靠读实现 | 看类型就懂 |

最上面那条编译错误就是第一行的证明——`User user = repository.findByName("张三");`
过不了编译。

---

## 三、创建 Optional 的三种方式

三种方式，用哪个取决于你**有多确定**：

```terminal
========== 3. 创建 Optional 的三种方式 ==========
Optional.empty()          → Optional.empty
Optional.of("张三")       → Optional[张三]
Optional.of(null)         → 抛 NullPointerException，message = null
Optional.ofNullable(null) → Optional.empty
Optional.ofNullable("张三") → Optional[张三]
```

| 写法 | 含义 | 传 `null` 会怎样 |
|---|---|---|
| `Optional.empty()` | 明确表示"没有值" | —— |
| `Optional.of(value)` | 确定有值 | **立刻抛 NPE** |
| `Optional.ofNullable(value)` | 可能有、可能没有 | 变成 `empty` |

`Optional.of(null)` 抛 NPE 是**故意的**：它在告诉你"你搞错了，这里不该是 null"。
这是"真的出错了"（bug），不是"值不存在"（正常业务情况）。两种情况必须分开，
所以 `of` 和 `ofNullable` 才要分成两个方法。

这里有个细节值得单独说：

```terminal
Optional.of(null)         → 抛 NullPointerException，message = null
```

**这条 NPE 的 message 是 `null`。** 不是"value cannot be null"，就是字面上的 `null`。
也就是说，异常自己也没告诉你"不该传 null"——堆栈会指到 `Optional.of` 那一行，
然后你得自己去查文档。

> 所以：**拿不准的时候用 `ofNullable`**。它永远不会因为你传 null 而炸。
> 只有在你百分之百确定不为空、并且希望"为空就立刻炸给我看"的地方，才用 `of`。

### 3.1 从老代码迁移：`ofNullable` 是那座桥

我们仓库里的 `findByName` 就是活例子：

```java
public Optional<User> findByName(String name) {
    return Optional.ofNullable(findUserByName(name));
}
```

老的 `findUserByName` 还在，返回 `null` 的逻辑一行没改，外面包一层 `ofNullable`
就变成了 Optional。真实项目里不可能一夜之间把所有 `null` 都换掉，
`ofNullable` 就是这种渐进迁移的桥。

---

## 四、先走一遍弯路：`isPresent()` + `get()`

知道 Optional 之后，最自然的改法是把 `if (user != null)` 换成
`if (optional.isPresent())`：

```java
Optional<User> found = repository.findByName("张三");
Optional<User> missing = repository.findByName("赵六");

if (found.isPresent()) {
    System.out.println("found.isPresent()   → true，get() 拿到 " + found.get().fullName());
}
System.out.println("missing.isPresent() → " + missing.isPresent());
System.out.println("missing.isEmpty()   → " + missing.isEmpty());
```

```terminal
========== 4. isPresent() + get()：改了，但没真改 ==========
found.isPresent()   → true，get() 拿到 张三
missing.isPresent() → false
missing.isEmpty()   → true
```

`isPresent()` 是"有没有值"，`isEmpty()` 是它的反面（Java 11 加的）。
`get()` 是"把值取出来"。

**能跑。但这里大家思考一个问题：这段代码和 `if (user != null)` 有区别吗？**

没有。

```java
if (user != null)        { user.fullName(); }        // 判 null 版
if (optional.isPresent()) { optional.get().fullName(); } // Optional 版
```

两行结构完全一样，只是把 `!= null` 换成了 `.isPresent()`。这仍然是
**命令式编程**（imperative programming）：先问"有没有"，再决定"取不取"。

> **这就是这一课最关键的转折点。**
>
> 如果你的 Optional 代码里全是 `isPresent()` + `get()`，那你只是给 `null` 换了个名字，
> 一行 `if` 都没少写，Optional 带来的好处（类型安全、链式表达）一点没拿到。

那正确的用法是什么？往下看。

---

## 五、正解（函数式）：把"如果"交给 Optional 自己

Optional 的正确姿势是**不写 `if`，把"没有的时候怎么办"作为参数传进去**。

### 5.1 最值钱的实验：`orElse` 和 `orElseGet` 的区别

这两个方法看起来一模一样，都是"没值就用默认值"：

```java
optional.orElse(defaultValue)      // 直接给个默认值
optional.orElseGet(supplier)       // 给一个「生产默认值」的函数
```

但它们的**执行时机**完全不同。这一课最值钱的就是把这个差异跑出来。

我们给两个"昂贵的默认值"各装一个计数器（真实场景里它可能是查数据库、读配置文件）：

```java
private static String computeDefaultForOrElse() {
    orElseCalls++;
    return "默认昵称";
}
```

然后分别在有值和没值的情况下调用：

```java
String a = Optional.of("小三").orElse(computeDefaultForOrElse());              // 有值 + orElse
String b = Optional.of("小三").orElseGet(() -> computeDefaultForOrElseGet());  // 有值 + orElseGet
```

```terminal
========== 5. orElse 和 orElseGet：参数到底什么时候算 ==========
── 情况 A：有值，用 orElse ──
结果 = 小三，默认值被算了 1 次
── 情况 B：有值，用 orElseGet ──
结果 = 小三，supplier 被执行了 0 次
```

**值明明在盒子里，`orElse` 的默认值还是被算了一次（1 次），`orElseGet` 一次都没算（0 次）。**

为什么会这样？看 `orElse` 的签名就明白了：

```java
public T orElse(T other)                   // ← 参数是「一个已经算好的值」
public T orElseGet(Supplier<? extends T> supplier)   // ← 参数是「一段还没跑的逻辑」
```

Java 的方法参数是**提前求值**（eager evaluation）的：调 `orElse(...)` 之前，
括号里的 `computeDefaultForOrElse()` 就已经被调用了，算出来的结果才传进去。
方法体里发现"哦原来有值"，已经晚了——那次计算已经花掉了。

`orElseGet` 收的是一个 `Supplier`——**一段代码，不是一个值**。要不要执行这段代码，
由 `orElseGet` 自己决定：有值就压根不调它。这就是**惰性求值**（lazy evaluation）。

那没值的时候呢？两个都得用上默认值，此时没差别：

```terminal
── 情况 C：没值，这次默认值真的用上了 ──
orElse    → 默认昵称，被算了 1 次
orElseGet → 默认昵称，被执行了 1 次
```

### 5.2 什么时候必须用 `orElseGet`

判断标准只有一条：**默认值的计算有没有代价**。

| 默认值长什么样 | 用哪个 | 为什么 |
|---|---|---|
| 字面量 `"匿名"`、`0`、`false` | `orElse` | 反正不花钱，写着更短 |
| `new ArrayList<>()` | `orElseGet` | 每次白造一个对象，虽然不贵但没必要 |
| 查数据库、读文件、调远程接口 | **`orElseGet`** | `orElse` 会**每次**都查一遍，纯浪费 |
| 有副作用的操作（写日志、计数） | **`orElseGet`** | 有值时 `orElse` 会**错误地**产生副作用 |

第四行是真正会出 bug 的地方。假设默认值是"记录一条'用户没填昵称'的日志"，
用 `orElse` 写，那么**每个有昵称的用户也会被记一笔**——数据就脏了。

> 一句话记住：**`orElse` 的参数总会执行，`orElseGet` 的只在为空时执行。**
> 拿不准就用 `orElseGet`，它永远不会多做功。

### 5.3 `orElseThrow`：查不到就抛自己的异常

有些场景默认值没意义——比如"查订单"，查不到就是业务错误，不该给个默认订单。
这时用 `orElseThrow`：

```terminal
========== 6. orElseThrow：查不到就抛自己的异常 ==========
有值时 orElseThrow() 直接拿到：张三
无参版没值 → java.util.NoSuchElementException: No value present
带 supplier → java.lang.IllegalArgumentException: 用户不存在：赵六
```

两个版本：

```java
optional.orElseThrow()                                          // 抛 NoSuchElementException
optional.orElseThrow(() -> new IllegalArgumentException("..."))  // 抛你自己的异常
```

带 supplier 的那个在业务代码里非常常用：异常类型和 message 都由你定，
上层能根据类型做不同处理，而不是去 catch 一个笼统的 `NoSuchElementException`。

### 5.4 `ifPresent` / `ifPresentOrElse`：有值才做事

有时候我们不要返回值，只是"有值就干点什么"：

```terminal
========== 7. ifPresent 和 ifPresentOrElse：有值才做事 ==========
ifPresent 命中：张三 <zhangsan@example.com>
ifPresentOrElse 命中：张三
ifPresentOrElse 落空：查无赵六
```

```java
repository.findByName("张三").ifPresent(u -> System.out.println("命中：" + u));
repository.findByName("赵六").ifPresent(u -> System.out.println("这行不该打印"));

repository.findByName("赵六").ifPresentOrElse(
        u -> System.out.println("命中：" + u.fullName()),
        () -> System.out.println("落空：查无赵六"));
```

`ifPresent` 只处理"有值"这一支，没值就默默什么都不做。
`ifPresentOrElse`（Java 9 加的）把两个分支都给你，正好补上这个空缺。

注意 `ifPresent` **不返回新值**——它是个"终点"操作。如果你想在两个分支里都产出结果
（比如"有值就转成字符串，没值就返回'查无此人'"），用 `map` + `orElse` 更顺：

```java
repository.findByName(name)
        .map(u -> "用户：" + u.fullName())
        .orElse("查无此人");
```

### 5.5 `map` / `flatMap` / `filter`：在盒子里做转换

这三个方法是 Optional 从函数式那边借来的，也是"消灭 `if`"的主力。

```terminal
========== 8. map / flatMap / filter：在盒子里做转换 ==========
map 取名字长度：Optional[2]
李四没昵称，flatMap + map 链走完：Optional.empty
map(User::nickname)     → Optional[Optional[小三]]   ← 两层 Optional
flatMap(User::nickname) → Optional[小三]   ← 一层，我们要的就是这个
李四 map(User::nickname) → Optional[Optional.empty]   ← 空值也套两层
map(u -> (String) null) → Optional.empty   ← 返回 null 也只是 empty，不炸
flatMap(u -> null) → 抛 NullPointerException（map 不会）
张三 filter(邮箱是 @example.com)：Optional[张三 <zhangsan@example.com>]
钱七 filter(邮箱是 @example.com)：Optional.empty   ← 人找到了，但不满足条件，一样是 empty

口诀：转换后还是普通值 → map；转换后本身就是 Optional → flatMap
```

**`map` 是"如果盒子里有值，就把它变成另一个值，再放回盒子里"。**

```java
repository.findByName("张三")     // Optional<User>
        .map(User::fullName)      // Optional<String>   ← 把 User 变成 String
        .map(String::length);     // Optional<Integer>  ← 把 String 变成 Integer
```

链子一路走下来，全程没有一个 `if`。如果中间任何一环拿到的是空盒子，
后面的 `map` 全都自动跳过，最后就是 `Optional.empty`——**不会 NPE**。

`map` 还有个贴心的地方：**转换函数返回 `null` 也不会炸**，结果会变成 `empty`：

```java
repository.findByName("张三").map(u -> (String) null)   // → Optional.empty
```

```terminal
map(u -> (String) null) → Optional.empty   ← 返回 null 也只是 empty，不炸
```

换句话说是"**`map` 帮你把 null 悄悄转成了 empty**"。这个设计有好有坏：
好处是链子不会因为某一环返回 null 而断掉；坏处是**你分不清"转换结果是 null"
和"压根没有值"**——两种不同的情况被压成了同一个 `empty`。

`flatMap` 就没有这个保护了：如果传给它的函数返回 `null`，会直接抛 NPE。
翻 JDK 源码能看到原因——它拿到函数的返回值后立刻 `Objects.requireNonNull`：

```java
public <U> Optional<U> flatMap(Function<? super T, ? extends Optional<? extends U>> mapper) {
    Objects.requireNonNull(mapper);
    if (isEmpty()) {
        return empty();
    } else {
        @SuppressWarnings("unchecked")
        Optional<U> r = (Optional<U>) mapper.apply(value);
        return Objects.requireNonNull(r);      // ← 这里，返回 null 就炸
    }
}
```

跑一下：

```terminal
flatMap(u -> null) → 抛 NullPointerException（map 不会）
```

#### `map` 会自动再包一层，`flatMap` 不会

`User::nickname` 的返回类型本身就是 `Optional<String>`。`map` 的职责是
"把结果包进盒子"，所以它把 `Optional<String>` 当成一个普通值，包成了
`Optional<Optional<String>>`——**两层盒子**。

```terminal
map(User::nickname)     → Optional[Optional[小三]]   ← 两层 Optional
flatMap(User::nickname) → Optional[小三]   ← 一层，我们要的就是这个
```

`flatMap` 是"拍平"版的 `map`：转换函数返回 Optional 时，它不再包一层，直接给你那层。
记住这个口诀：

> **转换后还是普通值 → 用 `map`；转换后本身就是 `Optional` → 用 `flatMap`。**

#### 写这一课时真踩到的坑

这个坑不是我编出来举例的——是写这课 demo 的时候**真的踩了**。当时的代码是：

```java
Optional<String> nickname = repository.findByName("张三")
        .map(User::nickname)          // ← 应该用 flatMap
        .map(String::toUpperCase);
```

编译器的反应是：

```terminal
$ javac -encoding UTF-8 -d out -sourcepath src/main/java \
    src/main/java/dev/javalearning/optional/broken/NestedMap.java
src/main/java/dev/javalearning/optional/broken/NestedMap.java:32: error: incompatible types: cannot infer type-variable(s) U
                .map(String::toUpperCase);
                    ^
    (argument mismatch; invalid method reference
      incompatible types: Optional<String> cannot be converted to Locale)
  where U,T are type-variables:
    U extends Object declared in method <U>map(Function<? super T,? extends U>)
    T extends Object declared in class Optional
1 error
```

第一眼看到 `cannot be converted to Locale` 是懵的——`toUpperCase` 跟 `Locale` 有什么关系？

原因藏在最后两行：`.map(User::nickname)` 之后类型是 `Optional<Optional<String>>`，
于是下一环 `map` 的泛型 `T` 是 `Optional<String>`。而 `String::toUpperCase` 有两个重载：

- `toUpperCase()` —— 无参
- `toUpperCase(Locale locale)` —— 一个参数

编译器在 `Optional<String>` 上找不到无参的 `toUpperCase`，就去试那个带参数的，
于是把 `Optional<String>` 往 `Locale` 上套——套不上，报错。

**报错信息指向的位置（`Optional<String> cannot be converted to Locale`）离真正的原因
（上一层该用 `flatMap`）隔了一层。** 这是泛型 + 方法引用常见的"报错跑偏"现象，
遇到时别死盯报错那一行，往上看一层类型是什么。

#### `filter`：不满足条件也变空

```java
repository.findByName("张三").filter(u -> u.email().endsWith("@example.com"))
// Optional[张三 <zhangsan@example.com>]

repository.findByName("钱七").filter(u -> u.email().endsWith("@example.com"))
// Optional.empty  ← 钱七这个人确实存在，只是不满足条件
```

`filter` 是"有条件地保留"。注意上面第二行：**"人找到了"和"值最终在不在盒子里"是两件事**。
`filter` 不满足时不会抛异常，只是把盒子变空——这跟 `if (...) { throw }` 的思路完全不同。

### 5.6 `stream()`：0 个或 1 个元素的流

```terminal
========== 9. stream()：0 个或 1 个元素的流 ==========
Optional.empty().stream().count()  = 0
Optional.of("张三").stream().count() = 1
3 个名字查出来 2 个邮箱：[zhangsan@example.com, wangwu@example.com]
```

`Optional.stream()`（Java 9 加的）把盒子打开，倒出来一个**长度是 0 或 1 的流**：

```java
Optional.empty().stream().count()   // 0
Optional.of("张三").stream().count() // 1
```

它最实用的地方是**和一批查询结果配合**：

```java
List<String> emails = new ArrayList<>();
for (String name : List.of("张三", "赵六", "王五")) {
    repository.findByName(name).stream().map(User::email).forEach(emails::add);
}
```

3 个名字查出来 2 个邮箱——"赵六"那一路是空流，`forEach` 什么都不做，
**不需要写 `if (found.isPresent())`**。这个技巧在 [Stream](./10-Stream.md) 那一课会更有用。

### 5.7 别用 `get()`

最后单独说一下 `get()`。

```terminal
========== 10. 不要用 get()：没值就抛异常 ==========
Optional.empty().get() → java.util.NoSuchElementException: No value present
异常类型是 NoSuchElementException，不是 NullPointerException
```

注意异常类型是 **`NoSuchElementException`**，不是 `NullPointerException`——
`Optional` 把"没有值"这件事从 null 换成了另一个异常，但**它照样是运行期才炸**。

```java
Optional<User> user = repository.findByName("赵六");
user.get().fullName();      // ← 编译通过！运行期 NoSuchElementException
```

`get()` 的存在相当于在类型安全的大坝上开了个洞：你可以随时把一个
`Optional<User>` 强行变回 `User`，然后把类型系统给你的一切保护全部丢掉。

> **`get()` 不是不能用，但它应该只出现在你已经百分之百确定有值的地方。**
> 而"我确定有值"这件事，编译器不认——它只认你写没写 `orElseThrow()` / `map()` 之类。

`orElseThrow()` 和 `get()` 抛的是同一个异常（这一点在测试里钉住了），
但 `orElseThrow()` 的语义是"**我要求这里必须有值，没有就是异常情况**"，
而 `get()` 只是"我要取出来"。前者是有意的声明，后者往往是偷懒。

---

## 六、不推荐的用法

`Optional` 的官方类注释（`@apiNote`）里把它的定位写得非常明确，原文是：

> `Optional` is primarily intended for use as a method return type where there is a
> clear need to represent "no result," and where using `null` is likely to cause errors.

翻译过来就是：**Optional 主要是为了"方法返回类型"设计的**，用在需要表达"没有结果"、
而用 `null` 容易出错的地方。其他用法都有更好的替代。

（这句话可以直接在 JDK 自带的 `src.zip` 里 `java.base/java/util/Optional.java` 查到，
不是社区口口相传的"最佳实践"。）

这一节的每一条**都能编译、都能跑**，编译器一句怨言都没有：

```terminal
========== 11. 不推荐的用法：能编译，但别这么写 ==========
① Optional 当字段：编译通过、运行正常 → Optional[小三]
② Optional 当方法参数：编译通过、运行正常 → 你好，小三
③ Optional 当构造器参数：编译通过、运行正常
④ Optional 包集合：Optional.of(List.of()) = Optional[[]]
   一个空列表已经是「没有元素」了，外面再套一层 Optional 是双重否定
⑤ isPresent() + get()：等于把 null 换了个名字，见第 4 节

这些编译器都不拦——所以只能靠你自己记住：
Optional 的主要用途是「方法的返回类型」，就这一条。
```

所以这些规则**编译器不会帮你守**，只能自己记住。逐条说：

### ① 不要用作类的字段

```java
class BadStyle {
    private final Optional<String> nickname;   // ✗
}
```

**为什么不行**：`Optional` 本身**没有实现 `Serializable`**。写这一课时真的试了一遍：
一个带 `Optional<String>` 字段的类去序列化，当场抛

```
java.io.NotSerializableException: java.util.Optional
```

换成 `String` 字段就成功了（同一个类，77 字节）。所以只要你的类可能被序列化
（放进 session、走 RMI、存缓存），Optional 字段就是个定时炸弹。
另外每个 `Optional` 都是一个**额外的对象**，字段用它等于给每个实例都多分配一个盒子；
而 `null` 字段是零成本的。

> 顺便一提，Optional 的类注释里还有一句值得记住的：
> "A variable whose type is `Optional` should never itself be `null`"——
> **`Optional` 变量自己永远不该是 `null`**，它要么装着值，要么是 `Optional.empty()`。
> 如果你会写出 `Optional<User> u = null;`，那这一课就白学了。

**替代**：字段直接用 `String`（可以为 null），在 **getter 的返回类型**上包一层：

```java
private final String nickname;              // ✓ 字段就是普通类型

public Optional<String> nickname() {        // ✓ 返回类型用 Optional
    return Optional.ofNullable(nickname);
}
```

我们仓库里的 [`User.java`](../src/main/java/dev/javalearning/optional/User.java) 就是这么写的，
测试里还用反射把这两件事都钉住了：`nickname` **方法**返回 `Optional`，
`nickname` **字段**是 `String`。

### ② 不要用作方法参数

```java
String greet(Optional<String> prefix) { }   // ✗
```

**为什么不行**：参数用 `Optional`，等于**把判空的责任推给了调用方**——
调用方还是得先判断"我要不要包一个 Optional 再传进去"，甚至可能传 `null` 进来
（测试里就是这么炸的：`bad.greet(null)` → NPE）。这比直接收一个可能为 null 的参数还糟，
因为多了一层包装，问题反而更隐蔽。

**替代**：用**重载**或者两个明确的方法：

```java
String greet()                    { return greet(""); }
String greet(String prefix)       { return prefix + "，..."; }   // ✓ 不传就是不传
```

### ③ 不要用作构造器参数

同理，而且更严重：构造器参数用 Optional，字段大概率也得用 Optional，
于是 ① 和 ② 的毛病全占了。构造器只接受真正需要的值，
可选的东西要么给个默认值，要么用**静态工厂方法 + 建造者模式**。

### ④ 不要用来包装集合

```java
Optional<List<String>> wrapped = Optional.of(List.of());   // ✗
```

**为什么不行**：集合本身就能表达"空"——一个空的 `List` 就是"没有元素"。
外面再套一层 `Optional`，就出现了**两种表达同一个意思的方式**：
`Optional.empty()` 和 `Optional.of(emptyList)`。这两种到底有没有区别？
调用方该怎么判？没人说得清。而且上面那个 `Optional` **永远是 present**，
判断它毫无意义（测试里也验证了这一点）。

**替代**：直接返回空集合。

```java
return List.of();    // ✓ 没有元素就是空列表，别套 Optional
```

这条有个通用版本：**"空"能被自己的类型表达时，不要用 Optional**。
数组、`Map`、`String`（空串）都一样。

### ⑤ 不要用 `isPresent()` + `get()`

第 4 节已经详细说过了。它只是给 `null` 换了个名字，Optional 的好处一点没拿到。

**替代**：`orElse` / `orElseGet` / `orElseThrow` / `map` / `ifPresent`。

### ⑥ 那它到底该用在哪

**方法的返回类型。** 就这一条，但这一条覆盖了绝大多数场景：

```java
public Optional<User> findByName(String name)     // ✓ 查不到是正常情况
public Optional<String> nickname()                // ✓ 昵称可能没填
public Optional<Config> loadConfig(Path path)     // ✓ 文件可能不存在
```

判断标准可以简化成一句话：

> **"没有值"是一种正常的、预期内的结果 → 用 `Optional` 当返回类型。**
> **"没有值"是一种错误 → 用 `orElseThrow` 抛异常，而不是返回 Optional。**

比如"按 id 查用户，查不到"是正常情况（返回 `Optional`），
而"读配置文件失败"多半是错误（直接抛异常）。

---

## 七、这一课的术语表

- **`Optional<T>`** —— 一个可能装着 `T`、也可能是空的盒子；用来把"可能没有"写进类型
- **空指针异常**（`NullPointerException`，NPE）—— 对 `null` 调用方法或访问字段时抛出
- **`Optional.empty()`** —— 明确表示"没有值"的空盒子
- **`Optional.of(value)`** —— 创建有值的盒子；传 `null` 会立刻抛 NPE
- **`Optional.ofNullable(value)`** —— 允许 `null`，为空就得到 `empty`；最常用
- **`isPresent()` / `isEmpty()`** —— 判断盒子里有没有值
- **`get()`** —— 直接取值，没值抛 `NoSuchElementException`；**不推荐随便用**
- **`orElse(default)`** —— 没值就用默认值；**参数总会被提前算出来**
- **`orElseGet(supplier)`** —— 没值才执行 supplier；**惰性求值**，默认值贵时必用
- **`orElseThrow(...)`** —— 没值就抛异常；带 supplier 的版本可以抛自定义异常
- **`ifPresent(consumer)`** —— 有值才执行，没值什么都不做
- **`ifPresentOrElse(...)`** —— 有值和没值两个分支都处理
- **`map(function)`** —— 有值就转换，结果自动包回盒子；函数返回 `null` 会变成 `empty`
- **`flatMap(function)`** —— 和 `map` 一样，但函数本身返回 `Optional` 时**不会多包一层**
- **`filter(predicate)`** —— 不满足条件就变 `empty`，不抛异常
- **`stream()`** —— 把 Optional 变成 0 个或 1 个元素的流
- **提前求值**（eager evaluation）—— 方法参数在调用前就算好，`orElse` 的行为来源
- **惰性求值**（lazy evaluation）—— 到真正需要时才计算，`orElseGet` 的行为来源
- **命令式 / 函数式**（imperative / functional）—— `isPresent()+get()` 是前者，链式调用是后者
- **`Supplier<T>`** —— 一个"不接受参数、返回 T"的函数，`orElseGet` 的参数类型

---

## 八、下一课

到这里，第三阶段"写起来更省力的工具"已经讲了三课了：

- [泛型](./06-泛型.md) 解决的是"**装什么类型**"——让编译器记住盒子里的东西
- [Lambda 表达式](./07-Lambda表达式.md) 解决的是"**怎么传行为**"——把一段代码当参数传
- Optional 解决的是"**值可能不在**"——把"可能没有"写进类型

你会发现这一课的 `map` / `filter` / `ifPresent` 全都收一个函数当参数，
这正是 Lambda 的用武之地；而 `Optional<User>` 这个写法本身就是泛型。

那么问题就来了：**如果我要处理的是 100 个用户，而不是 1 个呢？**

`Optional` 只能装 0 个或 1 个值。当数据变成一串，`for` 循环 + `if` 的写法会迅速膨胀。
下一课 [Stream](./10-Stream.md) 要把这一课的思路放大到整条数据流上：
`filter` → `map` → `collect`，用声明式取代手写循环。

到那时你会发现，Optional 就是"最多装一个元素的 Stream"——今天学的这几个方法，
在 Stream 里全都有对应的、且更强大的版本。

下期再见。
