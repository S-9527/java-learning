# 07 · Lambda 表达式

> 状态：✅ 讲完了　·　最后更新：2026-10-13
> 代码：[`Message.java`](../src/main/java/dev/javalearning/lambda/Message.java) ·
> [`Email.java`](../src/main/java/dev/javalearning/lambda/Email.java) ·
> [`Sms.java`](../src/main/java/dev/javalearning/lambda/Sms.java) ·
> [`Calculator.java`](../src/main/java/dev/javalearning/lambda/Calculator.java) ·
> [`TextReader.java`](../src/main/java/dev/javalearning/lambda/TextReader.java) ·
> [`Greeter.java`](../src/main/java/dev/javalearning/lambda/Greeter.java) ·
> [`LambdaDemo.java`](../src/main/java/dev/javalearning/lambda/LambdaDemo.java) ·
> 断言：[`LambdaTest.java`](../src/test/java/dev/javalearning/lambda/LambdaTest.java)（19 条）

先给全貌，后面一节一节拆：

```terminal
$ java src/main/java/dev/javalearning/lambda/LambdaDemo.java

========== 1. 传统做法：发一条消息要四步 ==========
[Email] 你的订单已发货
[SMS] 验证码 8848
① 定义接口 → ② 写实现类 → ③ new 对象 → ④ 调 send
Email.java 和 Sms.java 里除了渠道名，没有任何别的信息

========== 2. Lambda 出场：同一个接口，不用建类 ==========
[Email] 你的订单已发货
[SMS] 验证码 8848
Email 类和 Sms 类一个都没用上，Message 接口也没动

========== 3. 语法简化阶梯：五步，越写越短 ==========
[完整] 参数类型和大括号都写全
[省类型] Message.send(String)，编译器知道是 String
[省括号] 只有一个参数，括号是多余的
[省大括号] 方法体只有一个表达式
方法引用 System.out::println
五种写法都是 Message 的实例，行为等价：true / true
两个参数时括号不能省：(a, b) -> a + b
addFull.calculate(3, 4)  = 7（写了 return）
addShort.calculate(3, 4) = 7（表达式就是返回值）
类型要么全写要么全省，(int a, b) 这种写一半是不行的
而 () -> ... 里的空括号不能省：Supplier 就是这种

方法引用（method reference）常见的三类：
  Math::max            → 4（静态方法）
  String::length       → 6（实例方法）
  ArrayList::new       → ArrayList（构造器）
它和 lambda 是同一件事的两种写法，不是新东西

========== 4. 函数式接口：只能有一个抽象方法 ==========
Message 的抽象方法个数 = 1
Calculator 的抽象方法个数 = 1
接口名后面能直接跟 -> 的前提，就是这里等于 1

那 Comparator 呢？抽象方法个数 = 2
它照样接得住 lambda：compare("lambda", "abc") = 3
多出来的那个是 equals(Object)——Object 早就有了，不算数
所以准确的说法是：有且只有一个「必须由实现者补上」的抽象方法

TextReader.read() 声明了 throws IOException，所以 lambda 里能抛：文件不存在
而 Consumer.accept 没声明异常，往里抛受检异常就编译不过（见 broken/）

========== 5. @FunctionalInterface：帮你挡住第二个抽象方法 ==========
Message 上有这个注解：true
Calculator 上故意没加：false
两个都能接 lambda——注解是可选的，它只是帮你把关
真加了第二个抽象方法，注解会让编译直接失败（见 broken/）

========== 6. JDK 自带的四大函数式接口 ==========
Predicate<T>    boolean test(T)   判断：test(4) = true，test(7) = false
Function<T,R>   R apply(T)        转换：apply("lambda") = 6
Consumer<T>     void accept(T)    消费：accept 两次后 = [A, B]（返回 void）
Supplier<T>     T get()           生产：get() = 刚造出来的字符串（不要参数）

这四个正好凑齐输入输出的四种组合：
  Consumer 有进无出 / Supplier 无进有出 / Predicate 有进、出的是判断结果 / Function 有进、出的是转换结果
后面 Stream 的 filter / map / forEach / 短路求值全靠它们

========== 7. 变量捕获：只能读，不能改 ==========
[捕获] lambda 读到了外面的 prefix
prefix 在 lambda 里被改？不行，真实编译错误见 broken/CaptureAndModify.java
数组元素可以改：counter[0] = 2
（捕获的是变量 counter 的值的拷贝，那个拷贝一直指向同一个数组）

更关键的是：lambda 可能比声明它的那个方法活得久
[逃逸的 lambda] 方法已经返回了，这个值还在 ← 现在才调用
makeEscapingLambda 早就返回了，栈上的局部变量也没了
但 lambda 还记得那个值——它记的是一份拷贝，不是那个栈位置

========== 8. 和匿名内部类的区别：this 指向谁 ==========
lambda 里的 this = Greeter(Albert)，和外层是同一个对象：true
匿名内部类里的 this = 那个匿名内部类对象，和外层是同一个对象：false（Greeter.this 才是外层：true）
```

第 3、7、8 节是这一课最值钱的部分。

---

## 一、先让代码疼一次：发一条消息要四步

需求很普通：系统要能发消息，今天支持邮件和短信，明天可能加微信。

按 [继承与多态](./05-继承与多态.md)学过的规矩，第一步是定义接口：

```java
@FunctionalInterface
public interface Message {
    void send(String text);
}
```

然后给每种渠道写一个实现类：

```java
public class Email implements Message {
    @Override
    public void send(String text) {
        System.out.println("[Email] " + text);
    }
}
```

`Sms` 和它长得一模一样，只是把 `[Email]` 换成 `[SMS]`。用起来是这样：

```java
Message email = new Email();
Message sms = new Sms();
email.send("你的订单已发货");
sms.send("验证码 8848");
```

```terminal
========== 1. 传统做法：发一条消息要四步 ==========
[Email] 你的订单已发货
[SMS] 验证码 8848
① 定义接口 → ② 写实现类 → ③ new 对象 → ④ 调 send
Email.java 和 Sms.java 里除了渠道名，没有任何别的信息
```

能跑，没毛病。但把这三个文件摊开看一遍，问题就出来了：

| 步骤 | 文件 | 里面有什么 |
|---|---|---|
| ① 定义接口 | `Message.java` | `void send(String)` |
| ② 写实现类 | `Email.java` | 一行 `println` |
| ③ `new` 对象 | 调用处 | `new Email()` |
| ④ 调用 | 调用处 | `send(text)` |

**为了「发一条消息」这一件事，我们造了两个类。** 而这两个类里唯一有价值的信息，
就是 `"[Email] "` 和 `"[SMS] "` 这两个字符串——剩下全是仪式。

那么问题就来了：**如果我只是想"换一种发法"，为什么非得先给它一个类名？**

这正是 Lambda 要解决的问题。

---

## 二、正解：Lambda 直接实现那个方法

把 `Email` 和 `Sms` 两个类删掉，同一件事写成这样：

```java
Message email = text -> System.out.println("[Email] " + text);
Message sms = text -> System.out.println("[SMS] " + text);
email.send("你的订单已发货");
sms.send("验证码 8848");
```

```terminal
========== 2. Lambda 出场：同一个接口，不用建类 ==========
[Email] 你的订单已发货
[SMS] 验证码 8848
Email 类和 Sms 类一个都没用上，Message 接口也没动
```

输出和第一节**逐字相同**，但：

- 没有 `Email` 类，没有 `Sms` 类，两个文件都没了
- `Message` 接口**一个字都没改**
- `text -> System.out.println("[Email] " + text)` 这行本身**就是**那个实现

箭头左边是参数，右边是方法体。**它干的活和 `Email.send` 完全一样，只是不再需要一个名字。**

用一句话记住这个形状：

> **Lambda 表达式（lambda expression）就是"一个没有名字的方法"，
> 用来现场实现函数式接口的那个唯一抽象方法。**

第一节里那句 `new Email()`，换成 lambda 之后连"对象"这个词都不提了。
但注意——它**仍然是对象**，第 3 节会验证这一点。

---

## 三、语法简化阶梯：五步，越写越短

现在把同一个 lambda 从最啰嗦写到最短。这五步建议亲手敲一遍，敲完就再也忘不掉。

### 3.1 第一步：完整形式

```java
Message full = (String text) -> {
    System.out.println("[完整] " + text);
};
```

参数带类型、括号、大括号、分号，一样不少。**这是"标准答案"，其余四种都是它的缩写。**

### 3.2 第二步：参数类型可省

```java
Message noType = (text) -> {
    System.out.println("[省类型] " + text);
};
```

为什么能省？因为 `Message` 接口上只写着 `void send(String text)` 这一个抽象方法，
编译器一查就知道这个 `text` 是 `String`。

**这就是"函数式接口只能有一个抽象方法"的第一层原因**——只有一个，
编译器才没有歧义可猜。第 4 节会正式讲。

### 3.3 第三步：单参数可省括号

```java
Message noParen = text -> {
    System.out.println("[省括号] " + text);
};
```

只有一个参数时，括号是多余的。**但零个或多个参数时括号不能省**，
所以 `()` 和 `(a, b)` 永远得带着括号：

```java
Supplier<String> fresh = () -> "刚造出来的字符串";     // 零个参数，括号必须有
Calculator add = (a, b) -> a + b;                     // 两个参数，括号必须有
```

### 3.4 第四步：单表达式可省大括号和 `return`

```java
Message oneLine = text -> System.out.println("[省大括号] " + text);
```

方法体只有一个表达式时，大括号和结尾的分号一起省掉。

有返回值时这条更好用——**省掉大括号的同时，`return` 也必须省掉**，
表达式的值直接当返回值：

```java
Calculator addFull = (int a, int b) -> {
    return a + b;                    // 有大括号，return 不能省
};
Calculator addShort = (a, b) -> a + b;   // 没有大括号，也不许写 return
```

```terminal
两个参数时括号不能省：(a, b) -> a + b
addFull.calculate(3, 4)  = 7（写了 return）
addShort.calculate(3, 4) = 7（表达式就是返回值）
类型要么全写要么全省，(int a, b) 这种写一半是不行的
而 () -> ... 里的空括号不能省：Supplier 就是这种
```

这里有两个容易踩的坑，都真跑过：

- **`(int a, b)` 编译不过。** 类型只能"全写"或"全省"，混着写编译器不认，
  报的是 `cannot mix implicitly-typed and explicitly-typed parameters`
- **不写大括号却写了 `return`** 也编译不过，报 `illegal start of expression`

### 3.5 第五步：方法引用

```java
Message methodRef = System.out::println;
```

这行和 `text -> System.out.println(text)` **完全等价**。

什么时候能用方法引用？**当 lambda 的方法体只是"把参数原封不动转交给另一个方法"时。**
`text -> System.out.println(text)` 里，`text` 从左边进来、原样从右边出去，
中间没有任何加工——这种"我只是个二传手"的形态，就可以写成 `System.out::println`。

常见的有三类：

```java
Calculator byStatic = Math::max;                        // 静态方法引用
Function<String, Integer> byInstance = String::length;  // 实例方法引用
Supplier<ArrayList<String>> byConstructor = ArrayList::new;  // 构造器引用
```

```terminal
方法引用（method reference）常见的三类：
  Math::max            → 4（静态方法）
  String::length       → 6（实例方法）
  ArrayList::new       → ArrayList（构造器）
它和 lambda 是同一件事的两种写法，不是新东西
```

**注意：方法引用不是"第六步"，它是 lambda 的一种写法。**
不存在"方法引用比 lambda 更快"这回事——它们是同一件事。

那到底用哪个？**哪个读起来更清楚就用哪个。**
`System.out::println` 比 `text -> System.out.println(text)` 短，所以用它；
但 `s -> s.length() > 3` 这种带判断的，写成方法引用反而更绕，就老实写 lambda。

### 3.6 阶梯总结

| 写法 | 代码 | 能省的条件 |
|---|---|---|
| 完整 | `(String text) -> { System.out.println(text); }` | —— |
| 省类型 | `(text) -> { ... }` | 编译器能从接口推断 |
| 省括号 | `text -> { ... }` | 参数**恰好一个** |
| 省大括号 | `text -> System.out.println(text)` | 方法体**恰好一个表达式** |
| 方法引用 | `System.out::println` | 方法体只是转交参数 |

`Calculator addShort = (a, b) -> a + b;` 用到了第 1、4 两条（省类型 + 省大括号），
所以它是"省到不能再省"的形态。

---

## 四、函数式接口：lambda 只能挂在这种接口上

回头看那个箭头：`text -> ...`。**编译器怎么知道这个 `text` 是什么类型？**

答案在左边：`Message email = ...`。编译器看到你要把它赋给 `Message`，
就去 `Message` 里找那个唯一的抽象方法，把参数类型、返回值类型都从那里抄过来。

**这个"落地目标"有个正式名字：目标类型（target type）。**

所以 lambda 不是"随便什么类型都行"，它必须落在一个**函数式接口**上：

> **函数式接口（functional interface）：有且只有一个抽象方法的接口。**

### 4.1 为什么只能有一个

假设接口有两个抽象方法：

```java
@FunctionalInterface
public interface TwoAbstractMethods {
    void send(String text);
    void receive(String text);   // ← 第二个
}
```

现在写 `text -> System.out.println(text)`，编译器就懵了：

> 这个 lambda 到底是想实现 `send`，还是想实现 `receive`？

参数列表长得一样、返回值也一样，**光看 lambda 本身根本分不出来。**
它没法猜，只好报错。真实编译错误：

```terminal
$ javac -encoding UTF-8 -d out -sourcepath src/main/java \
    src/main/java/dev/javalearning/lambda/broken/TwoAbstractMethods.java
src/main/java/dev/javalearning/lambda/broken/TwoAbstractMethods.java:28: error: incompatible types: TwoAbstractMethods is not a functional interface
            TwoAbstractMethods m = text -> System.out.println(text);
                                   ^
    multiple non-overriding abstract methods found in interface TwoAbstractMethods
src/main/java/dev/javalearning/lambda/broken/TwoAbstractMethods.java:17: error: Unexpected @FunctionalInterface annotation
@FunctionalInterface
^
  TwoAbstractMethods is not a functional interface
    multiple non-overriding abstract methods found in interface TwoAbstractMethods
2 errors
```

两段错误值得逐字读：

- `incompatible types: ... is not a functional interface`——**这是 lambda 那行报的。**
  即使不写 `@FunctionalInterface` 注解，编译器在 lambda 落地时照样会拦
- `Unexpected @FunctionalInterface annotation`——**这是注解那行报的。**
  注解在接口声明处提前把问题抓住了

`multiple non-overriding abstract methods`（多个非重写的抽象方法）——这个措辞后面有用。

### 4.2 一个真实的例外：`Comparator`

`Comparator` 是天天在用的函数式接口，但**数它的抽象方法会数出 2 个**：

```terminal
那 Comparator 呢？抽象方法个数 = 2
它照样接得住 lambda：compare("lambda", "abc") = 3
多出来的那个是 equals(Object)——Object 早就有了，不算数
所以准确的说法是：有且只有一个「必须由实现者补上」的抽象方法
```

`Comparator` 里确实声明了两个抽象方法：

```
public abstract boolean java.util.Comparator.equals(java.lang.Object)
public abstract int     java.util.Comparator.compare(java.lang.Object,java.lang.Object)
```

但 `equals(Object)` 是 `Object` 早就提供的（每个类都有），**它不算"必须由实现者补上"**，
所以不参与计数。`multiple non-overriding abstract methods` 里的 `non-overriding`
说的就是这件事——**"非重写的"**。

这是本课第一个"我以为会怎样、结果不是"的地方，记下来：
**"只有一个抽象方法"是入门说法，准确说法是"只有一个必须由实现者补上的抽象方法"。**

### 4.3 抽象类不行

lambda 的落地目标必须是**接口**。抽象类哪怕只有一个抽象方法也不行：

```terminal
$ javac -encoding UTF-8 -d out -sourcepath src/main/java \
    src/main/java/dev/javalearning/lambda/broken/LambdaNeedsInterface.java
src/main/java/dev/javalearning/lambda/broken/LambdaNeedsInterface.java:27: error: incompatible types: Task is not a functional interface
        Task task = () -> System.out.println("试试能不能编译");
                    ^
1 error
```

写这个例子时我以为编译器会说 `interface expected here`，**实际报的是
`Task is not a functional interface`**。两种说法都对，但真话只有一种，所以按真话写。

### 4.4 受检异常：lambda 能抛什么，接口说了算

这是很多人对 lambda 的一个误解：**"lambda 里不能抛受检异常"——不对。**

能不能抛，取决于**接口那个方法有没有声明 `throws`**。看两个对比：

```java
// TextReader.read() 声明了 throws IOException
TextReader failing = () -> {
    throw new java.io.IOException("文件不存在");   // ✅ 能抛
};
```

```java
// Consumer.accept 没声明任何异常
Consumer<String> reader = path -> {
    throw new IOException("文件不存在：" + path);  // ❌ 编译不过
};
```

第二个的真实错误：

```terminal
$ javac -encoding UTF-8 -d out -sourcepath src/main/java \
    src/main/java/dev/javalearning/lambda/broken/CheckedExceptionInLambda.java
src/main/java/dev/javalearning/lambda/broken/CheckedExceptionInLambda.java:22: error: unreported exception IOException; must be caught or declared to be thrown
            throw new IOException("文件不存在：" + path);
            ^
1 error
```

原因很简单：**lambda 的方法体受那个接口方法的签名约束**。
`Consumer.accept` 的签名里没有 `throws IOException`，所以 lambda 里就不能往外扔。

这也解释了 JDK 里一对"长得像但不一样"的接口：

| 接口 | 方法 | 能抛受检异常吗 |
|---|---|---|
| `Runnable` | `void run()` | 不能 |
| `Callable<T>` | `T call() throws Exception` | 能 |

**它俩的差别只有 `throws`。** 这就是为什么要分成两个接口——不是一个能顶两个用。

---

## 五、`@FunctionalInterface`：可选的守门员

这个注解**完全是可选的**。本课的 `Calculator` 就故意没加：

```terminal
========== 5. @FunctionalInterface：帮你挡住第二个抽象方法 ==========
Message 上有这个注解：true
Calculator 上故意没加：false
两个都能接 lambda——注解是可选的，它只是帮你把关
真加了第二个抽象方法，注解会让编译直接失败（见 broken/）
```

**它不做任何代码生成，也不影响能不能接 lambda。它只干一件事：让编译器替你检查
"这个接口是不是有且只有一个抽象方法"。**

那为什么建议加？因为它**把错误提前了**。对比第 4 节那段错误：

- 不加注解：你要等到**写 lambda 的地方**才发现不对，
  而那个地方可能在另一个文件、另一个人手里
- 加了注解：**接口声明处当场报错**，谁改坏的谁马上知道

```terminal
src/main/java/dev/javalearning/lambda/broken/TwoAbstractMethods.java:17: error: Unexpected @FunctionalInterface annotation
@FunctionalInterface
^
  TwoAbstractMethods is not a functional interface
    multiple non-overriding abstract methods found in interface TwoAbstractMethods
```

`Unexpected @FunctionalInterface annotation`——"这个注解不该在这儿"。
翻译过来就是：**"你说它是函数式接口，可它不是。"**

这和第 04 课的 `@Override` 是同一个思路：**注解不改变行为，只让编译器帮你盯着一件容易忘的事。**

---

## 六、JDK 自带的四大函数式接口

`Message` 是我们自己造的接口。但实际写代码时，**绝大多数场合不需要自己定义接口**——
JDK 在 `java.util.function` 包里已经准备好了常用的形状。

最常用的四个：

| 接口 | 抽象方法 | 一句话 | 输入 → 输出 |
|---|---|---|---|
| `Predicate<T>` | `boolean test(T)` | 判断 | 有进，出 `boolean` |
| `Function<T,R>` | `R apply(T)` | 转换 | 有进有出，类型可不同 |
| `Consumer<T>` | `void accept(T)` | 消费 | 有进无出 |
| `Supplier<T>` | `T get()` | 生产 | 无进有出 |

### 6.1 `Predicate<T>`：判断

```java
Predicate<Integer> isEven = n -> n % 2 == 0;
isEven.test(4);   // true
isEven.test(7);   // false
```

名字来自数学里的"谓词"——**返回真或假的函数**。`Stream.filter` 收的就是它。

### 6.2 `Function<T,R>`：转换

```java
Function<String, Integer> length = s -> s.length();
length.apply("lambda");   // 6
```

**两个泛型参数：`T` 是吃的，`R` 是吐的。** 转换前后类型可以完全不同：

```java
Function<String, String> upper = String::toUpperCase;
upper.apply("lambda");   // "LAMBDA"
```

### 6.3 `Consumer<T>`：消费

```java
List<String> collected = new ArrayList<>();
Consumer<String> collect = collected::add;
collect.accept("A");
collect.accept("B");
// collected = [A, B]
```

**吃进去，不吐出来。** 返回值是 `void`，所以它注定是靠"副作用"干活——
往集合里加、往控制台打印、往文件里写。

`Iterable.forEach` 收的就是它。

### 6.4 `Supplier<T>`：生产

```java
Supplier<String> fresh = () -> "刚造出来的字符串";
fresh.get();   // "刚造出来的字符串"
```

**不要参数，只管给。** 注意它**每次 `get()` 都会重新执行一遍方法体**，不是缓存：

```java
int[] calls = {0};
Supplier<Integer> counter = () -> ++calls[0];
counter.get();   // 1
counter.get();   // 2
```

```terminal
========== 6. JDK 自带的四大函数式接口 ==========
Predicate<T>    boolean test(T)   判断：test(4) = true，test(7) = false
Function<T,R>   R apply(T)        转换：apply("lambda") = 6
Consumer<T>     void accept(T)    消费：accept 两次后 = [A, B]（返回 void）
Supplier<T>     T get()           生产：get() = 刚造出来的字符串（不要参数）
```

这四个为什么重要？因为**后面 [Stream](./10-Stream.md) 的每一个操作都收这四个之一**：

| Stream 操作 | 收的接口 |
|---|---|
| `filter` | `Predicate` |
| `map` | `Function` |
| `forEach` | `Consumer` |
| `orElseGet` / 惰性求值 | `Supplier` |

**这一课是下一课的地基。** 现在这四个名字记不住没关系，知道"有四个现成的形状"就够了。

---

## 七、变量捕获：能读，不能改

lambda 里能用外面的局部变量：

```java
String prefix = "[捕获] ";
Message captured = text -> System.out.println(prefix + text);
```

```terminal
[捕获] lambda 读到了外面的 prefix
```

但有一个硬性条件：**那个变量必须是事实上的 final（effectively final）**——
声明之后**再也没有被重新赋值过**（不一定要写 `final` 关键字）。

如果改了它呢？

```java
String prefix = "[捕获] ";
Message m = text -> {
    prefix = prefix + text;   // ← 编译错误
    System.out.println(prefix);
};
```

```terminal
$ javac -encoding UTF-8 -d out -sourcepath src/main/java \
    src/main/java/dev/javalearning/lambda/broken/CaptureAndModify.java
src/main/java/dev/javalearning/lambda/broken/CaptureAndModify.java:21: error: local variables referenced from a lambda expression must be final or effectively final
            prefix = prefix + text;          // ← 编译错误就在这一行
                     ^
src/main/java/dev/javalearning/lambda/broken/CaptureAndModify.java:21: error: local variables referenced from a lambda expression must be final or effectively final
            prefix = prefix + text;          // ← 编译错误就在这一行
            ^
src/main/java/dev/javalearning/lambda/broken/CaptureAndModify.java:22: error: local variables referenced from a lambda expression must be final or effectively final
            System.out.println(prefix);
                               ^
3 errors
```

`must be final or effectively final`——"必须是 final 或事实上的 final"。

注意它报了 **3 处**：赋值一次报两下（左边 `prefix` 一下、右边 `prefix` 一下），
读一次又报一下。**它把每一处引用都揪出来了**，而不是只报第一处就收工。

### 7.1 为什么有这个限制

因为 **lambda 捕获的是值的一份拷贝，不是那个变量本身。**

这一点最直观的证明是：**让 lambda 比声明它的方法活得久。**

```java
public static Message makeEscapingLambda(String value) {
    return text -> System.out.println("[逃逸的 lambda] " + value + " ← " + text);
}
```

```terminal
更关键的是：lambda 可能比声明它的那个方法活得久
[逃逸的 lambda] 方法已经返回了，这个值还在 ← 现在才调用
makeEscapingLambda 早就返回了，栈上的局部变量也没了
但 lambda 还记得那个值——它记的是一份拷贝，不是那个栈位置
```

`makeEscapingLambda` 早就返回了，它的栈帧也没了，按说局部变量 `value` 该消失了。
但返回出去的那个 lambda 还能正常打印——**因为它手里拿的是 `value` 的一份拷贝**，
那份拷贝跟着 lambda 对象一起活在堆上。

想清楚这一点，限制就顺理成章了：

> 如果原变量在拷贝之后还能改，那 lambda 里的拷贝和外头的变量就会**不一致**。
> 与其让人以为"我在里面改了外面就会变"，不如直接**编译不过**。

**这是"宁可报错，不要静默错误"的又一个例子**（[继承与多态](./05-继承与多态.md)里那个
漏写 `hashCode` 的例子是反面）。

### 7.2 但数组元素和字段可以改

有个看起来像例外的现象：

```java
int[] counter = {0};
Message counting = text -> counter[0]++;   // 编译通过
counting.send("第一条");
counting.send("第二条");
// counter[0] == 2
```

```terminal
数组元素可以改：counter[0] = 2
（捕获的是变量 counter 的值的拷贝，那个拷贝一直指向同一个数组）
```

**这不是例外。** 变量 `counter` 自己从头到尾没被重新赋值过——它是 effectively final 的。
被改的是 `counter[0]`，那是**数组里的一个格子**，不是变量。

用拷贝的视角看就清楚了：

```
变量 counter  ──(拷贝一份值)──>  lambda 手里那份拷贝
     │                                    │
     └──────────┬─────────────────────────┘
                ↓
          同一个数组对象
                ↓
        counter[0] 这个格子，谁都能改
```

**拷贝的是"指向数组的那个引用"，不是数组的内容。** 两份引用指向同一个数组，
所以通过哪一份都能改里面的元素。

同样的道理，**实例字段不受这个限制**：

```java
public class Counter {
    private int count;
    void increase() {
        Message m = text -> count++;   // ✅ 字段随便改
    }
}
```

因为字段跟着**对象**活，不跟着方法的栈帧活——根本不需要"拷贝"这个动作。

一句话总结这一节：

> **局部变量捕获的是值（所以不许改），字段和数组元素是"通过引用去改"（所以可以改）。**

---

## 八、和匿名内部类的区别：`this` 是谁

lambda 出来之前，要实现一个接口又不想建类，用的是**匿名内部类**：

```java
Message m = new Message() {
    @Override
    public void send(String text) {
        System.out.println(text);
    }
};
```

写法上和 lambda 是"同一个需求的两种解法"。但有一个关键差别：**`this` 的含义不同。**

```terminal
========== 8. 和匿名内部类的区别：this 指向谁 ==========
lambda 里的 this = Greeter(Albert)，和外层是同一个对象：true
匿名内部类里的 this = 那个匿名内部类对象，和外层是同一个对象：false（Greeter.this 才是外层：true）
```

| | `this` 指的是 | 想拿外层怎么办 |
|---|---|---|
| lambda | **外层对象**（它没有自己的 `this`） | 直接 `this` |
| 匿名内部类 | **那个匿名对象自己** | 写 `Greeter.this` |

为什么会这样？因为 **lambda 不是一个类**——它没有自己的作用域，`this` 就沿着外层一路找上去。
而匿名内部类**是一个真正的类**，一个对象当然有自己的 `this`。

### 8.1 写这一节时撞上的错误

本来想打印 `false` 就完事，顺手写了个 `this == outer`，结果：

```terminal
$ javac -encoding UTF-8 -d out -sourcepath src/main/java \
    src/main/java/dev/javalearning/lambda/broken/IncomparableThis.java
src/main/java/dev/javalearning/lambda/broken/IncomparableThis.java:29: error: incomparable types: <anonymous Supplier<String>> and IncomparableThis
                return "和外层是同一个对象：" + (this == outer);
                                            ^
1 error
```

`incomparable types`——**编译器说这两种类型根本没法比。**

这比打印一个 `false` 有意思多了：编译器认为「匿名 `Supplier`」和「`Greeter`」
是两种毫不相干的类型，**连接比较的资格都没有**。想比只能先把两边都当成 `Object`，
那时候才会得到 `false`。

**"编译器拒绝比较"本身就是 `this` 换了含义的最强证据**——它连"可能是同一个东西"都不认。

（这个错误现在就挂在 `Greeter.java` 的注释里，提醒后来的人为什么那儿有个 `(Object)` 强转。）

---

## 九、不推荐的用法

- **别给 lambda 硬凑一个接口名。** 如果只是"传个判断条件"，
  用现成的 `Predicate` 就行，不用为它新建一个 `MyChecker` 接口——
  第 06 课讲泛型时说过的"别为每个类型抄一份"，这里是同一个道理
- **别在 lambda 里写超过三行的逻辑。** lambda 的好处是"一眼看懂它想干什么"。
  超过三行就该抽成一个有名字的方法，然后在 lambda 里**方法引用**它。
  这既解决了可读性，也让那段逻辑能单独测试
- **别在 lambda 里改外面的状态。** 虽然数组元素、字段能改（第 7.2 节），
  但那是"能"不是"该"。往 `Consumer` 里塞副作用，读代码的人很难看出这个 lambda 到底干了什么。
  要收集结果，优先用 [Stream](./10-Stream.md) 的 `collect`
- **别把 `System.out.println` 的调试代码留在 lambda 里。**
  lambda 常常是循环里被调用的，一个 `println` 会打印一万遍
- **别对 lambda 调 `getClass()` 然后按类名判断。** 它的运行时类是编译器合成的
  （名字里带 `$$`），换个 JDK 版本可能就变了。要判断类型请用 `instanceof` 接口
- **别以为加了 `@FunctionalInterface` 就"更快"或"更安全"。**
  它只影响编译期检查，生成的字节码一模一样。加它是为了"错误提前"，不是为了性能

---

## 十、这一课的术语表

- **Lambda 表达式**（lambda expression）—— `(参数) -> { 方法体 }`，一个没有名字的方法实现
- **函数式接口**（functional interface）—— 有且只有一个"必须由实现者补上"的抽象方法的接口
- **目标类型**（target type）—— lambda 要赋给的那个类型；编译器从它推断参数和返回值的类型
- **类型推断**（type inference）—— 编译器根据上下文补出你省略的类型
- **方法引用**（method reference）—— `System.out::println`，方法体只是转交参数时的简写
- **变量捕获**（variable capture）—— lambda 读取外层局部变量的行为
- **事实上的 final**（effectively final）—— 声明后没被重新赋值过；变量捕获的前提
- **匿名内部类**（anonymous inner class）—— lambda 之前的替代方案，`this` 指向它自己
- **`@FunctionalInterface`** —— 可选注解，让编译器检查"是不是只有一个抽象方法"
- **`Predicate<T>`** —— `boolean test(T)`，判断
- **`Function<T,R>`** —— `R apply(T)`，转换
- **`Consumer<T>`** —— `void accept(T)`，消费
- **`Supplier<T>`** —— `T get()`，生产
- **受检异常**（checked exception）—— lambda 能不能抛，取决于目标接口的方法有没有声明 `throws`

---

## 十一、下一课

这一课我们解决的是"**为了一个方法，值不值得建一个类**"。

答案是：不值得。Lambda 让"一段行为"可以像数据一样被传来传去——
`filter` 收一个判断、`map` 收一个转换、`forEach` 收一个消费，
方法调用方不再需要知道那段行为叫什么名字、属于哪个类。

**但这里埋了一个坑。**

第 7 节我们花了大半节讲"不能返回 `null`"这类问题的一个变种：
`Supplier<T>` 的 `get()` 万一返回 `null` 呢？`Function` 转换失败了呢？
方法返回值说好是 `User`，结果给回来一个 `null`——调用方一忘判就 NPE。

[方法](./03-方法.md)那一课讲过"别用返回值 `null` 表示没有结果"，因为调用方太容易忘判。
但光靠"约定"不够：**签名上写着 `User`，它就长得跟一定有值一样。**

那么问题就来了：**能不能让"可能没有结果"这件事，直接写在类型上，
让编译器盯着调用方必须处理？**

这就是 [Optional](./08-Optional.md) 要解决的事。

下期再见。
