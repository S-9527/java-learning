# 10 · Stream

> 状态：✅ 讲完了　·　最后更新：2026-10-13
> 代码：[`Person.java`](../src/main/java/dev/javalearning/stream/Person.java) ·
> [`Order.java`](../src/main/java/dev/javalearning/stream/Order.java) ·
> [`StreamDemo.java`](../src/main/java/dev/javalearning/stream/StreamDemo.java) ·
> 断言：[`StreamTest.java`](../src/test/java/dev/javalearning/stream/StreamTest.java)（55 条）

假设产品经理给你一个需求：

> 把订单里金额超过 100 的挑出来，按金额从高到低排，取前 10 个。

你打开 `OrderService`，里面已经有七八个类似的循环。你照着上一个抄了一遍——
`for`、`if`、`list.add`、`Collections.sort`、`subList`。跑通了，代码评审也过了。

三个星期后又要改：金额超过 100 的、**去重**、按金额排、取前 10 个。
你盯着那段循环看了半分钟，才找到该在哪一行加去重。

这一课要讲的 Stream，就是来解决这件事的。它不改变你能做什么，
只改变你**表达**这件事的方式。

这一课全程依赖上一课讲的 Lambda：`filter(p -> p.age() > 18)` 里那个箭头就是 Lambda，
`Person::name` 是它的简写。如果那段还有点模糊，先回去看
[Lambda 表达式](./07-Lambda表达式.md)。流本身也大量用 `Optional`（`findFirst()` 的返回值），
那部分留给 [Optional](./08-Optional.md)。

先给全貌。下面这是 [`StreamDemo.java`](../src/main/java/dev/javalearning/stream/StreamDemo.java)
的完整输出，后面一节一节拆：

```terminal
$ java src/main/java/dev/javalearning/stream/StreamDemo.java

========== 1. 先让代码疼一次：for 循环版 ==========
年龄大于 18 的：[Alice(30,美国), Cindy(25,中国), Eve(35,美国), Frank(25,中国), Grace(30,美国), Cindy(25,中国)]
结果对，但读的人得把 for + if + add 在脑子里翻译成「筛出成年人」

========== 2. Stream 版：一行说清意图 ==========
同样的结果：[Alice(30,美国), Cindy(25,中国), Eve(35,美国), Frank(25,中国), Grace(30,美国), Cindy(25,中国)]
和 for 版结果一样吗 → true

========== 3. Stream 的三个步骤 ==========
① 创建      people.stream()      → 一个流，还没干活
② 中间操作  .filter(..).map(..)  → 还是流，可以继续往后接
③ 终端操作  .collect(toList())   → [Alice, Cindy, Eve, Frank, Grace, Cindy]
终端操作返回的不再是流，所以后面不能再接 .filter()

========== 4. 惰性求值：没有终端操作，一行都不会跑 ==========
链已经建好了，上面两个打印一句都没出现——因为还没有终端操作
现在补上终端操作 collect，注意打印是这时候才刷出来的：
    filter 跑了：Alice
    map    跑了：Alice
    filter 跑了：Bob
    filter 跑了：Cindy
    map    跑了：Cindy
    filter 跑了：David
    filter 跑了：Eve
    map    跑了：Eve
    filter 跑了：Frank
    map    跑了：Frank
    filter 跑了：Grace
    map    跑了：Grace
    filter 跑了：Cindy
    map    跑了：Cindy
collect 之后拿到 6 个名字：[Alice, Cindy, Eve, Frank, Grace, Cindy]

========== 5. 创建流的多种方式 ==========
集合 .stream()      → [Alice, Bob, Cindy]
数组 Arrays.stream  → [3, 1, 4, 1, 5]
  数组版是 IntStream，可以直接 sum() = 14
Stream.of("甲","乙","丙") → [甲, 乙, 丙]
Stream.builder()    → [甲, 乙, 丙]
Files.lines(文件)   → [苹果, 香蕉, 樱桃]
IntStream.range(1,5)       → [1, 2, 3, 4]
IntStream.rangeClosed(1,5) → [1, 2, 3, 4, 5]
  1 到 100 的和 = 5050
new Random(42).ints(5, 1, 100) → [63, 49, 64, 72, 37]
Stream.generate(..).limit(3)   → [重复, 重复, 重复]
Stream.iterate(1, n -> n*2).limit(6) → [1, 2, 4, 8, 16, 32]
三参数的 iterate（带停止条件，不用 limit）→ [1, 2, 3, 4, 5]
Stream.empty() 的 count = 0
Stream.concat  → [1, 2, 3, 4]

========== 6. 中间操作：filter / distinct / limit / skip ==========
原始数据（8 个人）：[Alice(30,美国), Bob(17,英国), Cindy(25,中国), David(17,英国), Eve(35,美国), Frank(25,中国), Grace(30,美国), Cindy(25,中国)]
filter(age > 18)          → 6 个：[Alice(30,美国), Cindy(25,中国), Eve(35,美国), Frank(25,中国), Grace(30,美国), Cindy(25,中国)]
distinct()                → 7 个：[Alice(30,美国), Bob(17,英国), Cindy(25,中国), David(17,英国), Eve(35,美国), Frank(25,中国), Grace(30,美国)]（Cindy 重了，剩一个）
limit(3)                  → [Alice(30,美国), Bob(17,英国), Cindy(25,中国)]
skip(2).limit(3)          → [Cindy(25,中国), David(17,英国), Eve(35,美国)]
skip 超过长度不报错，只是空：0 个

========== 7. 中间操作：map 与 flatMap 的区别 ==========
map(Order::items)     → [[键盘, 鼠标], [显示器], [鼠标, 耳机, 键盘]]
  还是 3 个元素，只是每个元素里套了一个 List
flatMap(items.stream) → [键盘, 鼠标, 显示器, 鼠标, 耳机, 键盘]
  6 个元素，全都摊平到一层了（3 个订单：2 + 1 + 3）
摊平之后 distinct() 才有意义 → [键盘, 鼠标, 显示器, 耳机]
拆字符串也是一样       → [甲, 乙, 丙, 丁]

========== 8. 中间操作：mapToInt 转成数值流 ==========
mapToInt(Person::age).sum()     = 204
mapToInt(Person::age).average() = 25.5
summaryStatistics()             = IntSummaryStatistics{count=8, sum=204, min=17, average=25.500000, max=35}
数值流 .boxed() 转回对象流      = [30, 17, 25]
mapToInt(..).toArray()          = [30, 17, 25, 17, 35, 25, 30, 25]
IntStream 自己也能直接造         = [1, 4, 9, 16, 25]

========== 9. 中间操作：sorted 与比较器 ==========
sorted() 自然序        → [Alice, Bob, Cindy, David, Eve, Frank, Grace]
sorted(comparingInt(age)) → [Bob(17,英国), David(17,英国), Cindy(25,中国), Frank(25,中国), Alice(30,美国), Grace(30,美国), Eve(35,美国)]
  年龄相同的 Alice/Grace 保持原来的先后——sorted 是稳定排序
reversed()             → [Eve(35,美国), Alice(30,美国), Grace(30,美国), Cindy(25,中国), Frank(25,中国), Bob(17,英国), David(17,英国)]
thenComparing(name)    → [Bob(17,英国), David(17,英国), Cindy(25,中国), Frank(25,中国), Alice(30,美国), Grace(30,美国), Eve(35,美国)]

但无参 sorted() 排 Person 会炸——而且要到运行期才知道：
  ClassCastException: class Person cannot be cast to class java.lang.Comparable
  （完整 message 还会带 module / loader 信息，每次都不同）
只有一个元素时却不炸：[Alice(30,美国)]   ← 数据一多就炸，测试只放一条就发现不了

========== 10. 串起来：filter + distinct + sorted + map + limit ==========
结果：[Eve, Alice, Grace]
五步读下来就是一句话：25 岁以上的不重复的人，按年龄从大到小取前三个

for 循环写同一件事要这么长：
    List<Person> adults = new ArrayList<>();
    for (Person p : people) if (p.age() >= 25) adults.add(p);
    Set<Person> seen = new LinkedHashSet<>(adults);      // 还得自己想怎么去重
    adults = new ArrayList<>(seen);
    adults.sort(Comparator.comparingInt(Person::age).reversed());  // 再想怎么排
    List<String> names = new ArrayList<>();
    for (int i = 0; i < 3 && i < adults.size(); i++) names.add(adults.get(i).name());
意图散在 6 个语句里，改一个需求要在 6 个地方找

========== 11. 终端操作：短路——找到就停 ==========
    anyMatch 检查到：Alice
anyMatch(age > 18)   = true   ← 第一个人就满足，后面 7 个没被检查
    allMatch 检查到：Alice
    allMatch 检查到：Bob
allMatch(age > 18)   = false  ← 到 Bob 就失败了，后面 6 个没被检查
    noneMatch 检查到：Alice
    noneMatch 检查到：Bob
    noneMatch 检查到：Cindy
    noneMatch 检查到：David
    noneMatch 检查到：Eve
    noneMatch 检查到：Frank
    noneMatch 检查到：Grace
    noneMatch 检查到：Cindy
noneMatch(age > 100) = true  ← 没有一个能提前判定，8 个全查了
noneMatch(age > 30)  = false  ← 到第 5 个 Eve 就满足，后面 3 个不看：同一操作也能短路
findFirst(age < 18)  = Bob
findAny(age < 18)    = Bob   ← 顺序流下它也是第一个，别依赖这个
找不到的时候         = 没有（Optional 空，不会抛异常）

========== 12. 终端操作：聚合 count / max / min / sum / average ==========
count()   → 6
max(age)  → Eve(35,美国)
min(age)  → Bob(17,英国)   ← 17 岁有两个，返回先遇到的那个
sum()     → 204
average() → 25.5
空流的 average 是空 Optional，不是 0：OptionalDouble.empty

========== 13. 终端操作：reduce 规约 ==========
reduce(Integer::max)     → 35
reduce(0, Integer::sum)  → 204
reduce(起点, 累加, 合并) → "Alice Bob Cindy David Eve Frank Grace Cindy"
  顺序流下 combiner 被调用 0 次——它是给并行流分块合并用的

========== 14. 终端操作：collect 收集 ==========
toList()        → [Alice, Bob, Cindy, David, Eve, Frank, Grace, Cindy]
toSet()         → [中国, 美国, 英国]   ← 打印前排过序，HashSet 本身不保证顺序
toMap 遇到重名   → Duplicate key Cindy (attempted merging values 25 and 25)
给合并函数之后  → {Alice=30, Bob=17, Cindy=25, David=17, Eve=35, Frank=25, Grace=30}
groupingBy(country) → {中国=[Cindy, Frank, Cindy], 美国=[Alice, Eve, Grace], 英国=[Bob, David]}
groupingBy + counting → {中国=3, 美国=3, 英国=2}
partitioningBy(age>18) → true  [Alice, Cindy, Eve, Frank, Grace, Cindy]
                         false [Bob, David]
joining()       → AliceBobCindyDavidEveFrankGraceCindy
joining(带分隔和括号) → [Alice, Bob, Cindy, David, Eve, Frank, Grace, Cindy]
summarizingInt  → IntSummaryStatistics{count=8, sum=204, min=17, average=25.500000, max=35}

========== 15. 终端操作：forEach 遍历 ==========
forEach：
    Alice 30
    Bob 17
    Cindy 25
forEachOrdered（顺序流下看不出区别，并行流下才保证顺序）：
    Alice 30
    Bob 17
    Cindy 25
    第 0 次回调，元素 = Alice(30,美国)，当前 size = 8
    第 1 次回调，元素 = Bob(17,英国)，当前 size = 8
    第 2 次回调，元素 = David(17,英国)，当前 size = 7
    第 3 次回调，元素 = Frank(25,中国)，当前 size = 6
    第 4 次回调，元素 = Grace(30,美国)，当前 size = 6
    第 5 次回调，元素 = Cindy(25,中国)，当前 size = 6
    第 6 次回调，元素 = null，当前 size = 6
边遍历边删 → NullPointerException（不是 ConcurrentModificationException！）
边遍历边加 → ConcurrentModificationException
所以别去猜「会抛哪个异常」——只要在流里改了源集合，行为就不可依赖

========== 16. collect 不是魔法：Collector 的四个组件 ==========
自己写的 Collector → Alice Bob Cindy David Eve Frank Grace Cindy
  顺序流下 combiner 被调用 0 次——它只在并行流里才需要
Collectors.toList() 就是 supplier=ArrayList、accumulator=add、combiner=addAll 的同一个套路

========== 17. 流用过一次就不能再用 ==========
第一次 count() = 6
第二次用同一个流 → IllegalStateException: stream has already been operated upon or closed

filtered.count() = 1
回头用 source → IllegalStateException: stream has already been operated upon or closed
所以要复用就复用「数据源」people()，每次都重新 .stream()

========== 18. 别在流里改外部状态 ==========
用数组当累加器能过编译 → 72（但这是绕过限制，不推荐）
同样的结果用 mapToInt(..).sum() → 72，没有副作用
副作用的三条线：改源集合会炸；用外部变量累加编译不过；并行流里做副作用结果不可预期（下一课）
```

18 节里有 4 节是重点：第 4 节（惰性求值）、第 11 节（短路）、第 15 节和第 17 节
（两个真实的坑）。我们一节一节来。

---

## 一、先让代码疼一次

数据源是 8 个人，代码里写死，全程不变：

```java
static List<Person> people() {
    return List.of(
            new Person("Alice", 30, "美国"),
            new Person("Bob", 17, "英国"),
            new Person("Cindy", 25, "中国"),
            new Person("David", 17, "英国"),
            new Person("Eve", 35, "美国"),
            new Person("Frank", 25, "中国"),
            new Person("Grace", 30, "美国"),
            new Person("Cindy", 25, "中国"));   // 和上面那个 Cindy 一模一样
}
```

注意最后那个 Cindy 和第三个 Cindy 内容完全相同。它是留给 `distinct()` 的伏笔。

第一个需求很朴素：**找出所有年龄大于 18 岁的人，收集成新列表**。
用我们最熟的办法写：

```java
static List<Person> adultsByLoop(List<Person> source) {
    List<Person> result = new ArrayList<>();
    for (Person p : source) {
        if (p.age() > 18) {
            result.add(p);
        }
    }
    return result;
}
```

```terminal
========== 1. 先让代码疼一次：for 循环版 ==========
年龄大于 18 的：[Alice(30,美国), Cindy(25,中国), Eve(35,美国), Frank(25,中国), Grace(30,美国), Cindy(25,中国)]
结果对，但读的人得把 for + if + add 在脑子里翻译成「筛出成年人」
```

跑通了，结果也对。但它有两个毛病，而且是会随着需求长大而恶化的毛病。

**毛病一：意图被淹没在步骤里。** 你想要的是一句话——「筛出成年人」。
你写出来的是三件事：造一个空列表、遍历、满足条件就往里塞。
读的人得先在脑子里把这三步**编译**回那句话，才能确认你的代码是对的。
如果条件再复杂一点（年龄 > 18 **且** 国家不是英国），这个编译过程就更长。

**毛病二：改需求要动多个地方。** 现在加上「去重」，你就得自己管一个 `Set`；
加上「排序」，就得自己调 `sort`；加上「取前 10 个」，就得自己写循环边界
`i < 10 && i < list.size()`——顺便一提，这个边界就是经典的差一错误高发区。

代码本身没错，错的是**它没有形状**。需求是一句话，代码是一堆步骤，
中间那道翻译的活儿，落在了每个读代码的人身上。

那如果有一种写法，能把「筛出成年人」这句话直接写出来呢？

---

## 二、正解：Stream 把「做什么」和「怎么做」分开

同一个需求，Stream 版：

```java
List<Person> byStream = people().stream()
        .filter(p -> p.age() > 18)
        .collect(Collectors.toList());
```

```terminal
========== 2. Stream 版：一行说清意图 ==========
同样的结果：[Alice(30,美国), Cindy(25,中国), Eve(35,美国), Frank(25,中国), Grace(30,美国), Cindy(25,中国)]
和 for 版结果一样吗 → true
```

`filter(p -> p.age() > 18)` 读作「过滤，条件是年龄大于 18」。
`collect(Collectors.toList())` 读作「收集成一个列表」。
中间没有「空列表」「遍历」「往里塞」——那些是**怎么做**，Stream 不让你操心。

这里要说清楚一件事，免得产生错误期待：**Stream 并没有让程序变快。**
它内部照样要遍历、要建列表，可能还比 `for` 慢一点（见第四节）。
它买到的是**可读性**——代码的形状和需求的形状对上了。

**声明式 Declarative** 就是这件事的名字：你说「要什么」，
而不是「一步步怎么做」。`for` 循环是**命令式 Imperative**。

---

## 三、Stream 的三个步骤

Stream 的一切都挂在这三步上。理解了它，后面几十个方法就都有地方放了。

```java
Stream<Person> created = people().stream();                          // ① 创建
Stream<String> intermediate = created.filter(p -> p.age() > 18)      // ② 中间操作
        .map(Person::name);
List<String> terminal = intermediate.collect(Collectors.toList());   // ③ 终端操作
```

```terminal
========== 3. Stream 的三个步骤 ==========
① 创建      people.stream()      → 一个流，还没干活
② 中间操作  .filter(..).map(..)  → 还是流，可以继续往后接
③ 终端操作  .collect(toList())   → [Alice, Cindy, Eve, Frank, Grace, Cindy]
终端操作返回的不再是流，所以后面不能再接 .filter()
```

| 步骤 | 干什么 | 返回什么 | 能接几个 |
|---|---|---|---|
| ① 创建 creation | 从集合/数组/文件/... 造出一条流 | `Stream<T>` | 一条链只有一个 |
| ② 中间操作 intermediate | 描述「怎么变」，**不执行** | 新的 `Stream<T>` | 想接几个接几个 |
| ③ 终端操作 terminal | 触发执行，产出结果 | 不再是流 | 只能有一个，放最后 |

为什么中间操作返回的必须还是流？因为这样才能**链式**接下去。
为什么终端操作返回的不是流？因为结果已经出来了，没什么好接的了。

于是就有了这一课最重要的一个特性。

### 3.1 惰性求值：这是理解 Stream 的钥匙

看这条链——它只有中间操作，**没有终端操作**：

```java
Stream<String> lazy = people().stream()
        .filter(p -> {
            System.out.println("    filter 跑了：" + p.name());
            return p.age() > 18;
        })
        .map(p -> {
            System.out.println("    map    跑了：" + p.name());
            return p.name();
        });
```

我们在 `filter` 和 `map` 里都塞了打印。如果流是「边写边跑」的，
这两句打印应该在我们建链的时候就刷出来。

```terminal
========== 4. 惰性求值：没有终端操作，一行都不会跑 ==========
链已经建好了，上面两个打印一句都没出现——因为还没有终端操作
现在补上终端操作 collect，注意打印是这时候才刷出来的：
    filter 跑了：Alice
    map    跑了：Alice
    filter 跑了：Bob
    filter 跑了：Cindy
    map    跑了：Cindy
    filter 跑了：David
    filter 跑了：Eve
    map    跑了：Eve
    filter 跑了：Frank
    map    跑了：Frank
    filter 跑了：Grace
    map    跑了：Grace
    filter 跑了：Cindy
    map    跑了：Cindy
collect 之后拿到 6 个名字：[Alice, Cindy, Eve, Frank, Grace, Cindy]
```

**一句都没打印。** 直到 `collect` 出现，打印才一股脑涌出来。

这就是**惰性求值 Lazy Evaluation**：

> 中间操作只是**记下来**「有这么一步」，并不执行。
> 直到终端操作出现，整条链才真正开始跑。

再看打印的**顺序**，信息量更大。它不是「先把 8 个人全 filter 完，再全 map」，
而是：

```
filter 跑了：Alice
map    跑了：Alice     ← Alice 一路走到底
filter 跑了：Bob       ← 然后才轮到 Bob
filter 跑了：Cindy     ← Cindy 过了 filter
map    跑了：Cindy
...
```

**每个元素是「一个元素走完整条链，再轮到下一个」**，
不是「一步处理所有元素，再进下一步」。这个区别不是学术细节——
下一节讲 `limit` 的时候，它直接决定效率。

理解了惰性，才能解释后面很多「奇怪」的现象。比如这个：

```java
// 只想打印，不想要结果——这样写什么都不会发生
people().stream().peek(System.out::println);   // 空转，一行都不打印
```

因为 `peek` 是中间操作，没有终端操作来"点火"。

那么问题就来了：既然不执行，那「流只能消费一次」这条规矩又是怎么来的？
我们在下面「不推荐的用法」那一节回答。

---

## 四、创建流：不止 `collection.stream()`

第一步是造出一条流。常用的有这么几种：

```terminal
========== 5. 创建流的多种方式 ==========
集合 .stream()      → [Alice, Bob, Cindy]
数组 Arrays.stream  → [3, 1, 4, 1, 5]
  数组版是 IntStream，可以直接 sum() = 14
Stream.of("甲","乙","丙") → [甲, 乙, 丙]
Stream.builder()    → [甲, 乙, 丙]
Files.lines(文件)   → [苹果, 香蕉, 樱桃]
IntStream.range(1,5)       → [1, 2, 3, 4]
IntStream.rangeClosed(1,5) → [1, 2, 3, 4, 5]
  1 到 100 的和 = 5050
new Random(42).ints(5, 1, 100) → [63, 49, 64, 72, 37]
Stream.generate(..).limit(3)   → [重复, 重复, 重复]
Stream.iterate(1, n -> n*2).limit(6) → [1, 2, 4, 8, 16, 32]
三参数的 iterate（带停止条件，不用 limit）→ [1, 2, 3, 4, 5]
Stream.empty() 的 count = 0
Stream.concat  → [1, 2, 3, 4]
```

逐个说几个容易踩的地方。

**`Arrays.stream(数组)` 出来的是数值流。** `int[]` 进去，出来的是 `IntStream`，
所以它能直接 `sum()`。要是你把结果 `boxed()` 成 `Stream<Integer>`，`sum()` 就没了。
原因见下面「数值流」那节。

**`Files.lines` 必须关掉。** 它背后是一个打开的文件句柄，流不关文件就一直开着。
所以它必须配 `try-with-resources`：

```java
try (Stream<String> lines = Files.lines(temp, StandardCharsets.UTF_8)) {
    System.out.println(lines.collect(Collectors.toList()));
}
```

**`range` 不含右端，`rangeClosed` 含。** `range(1, 5)` 是 `1 2 3 4`，
`rangeClosed(1, 5)` 才是 `1 2 3 4 5`。这是差一错误的重灾区，
写 `for (int i = 0; i < n; i++)` 习惯的人尤其容易栽。

**随机数流要写种子才可复现。** `new Random(42)` 里的 42 是**种子 seed**：
种子一样，生成的序列就一样。不写种子（`new Random()`）每次跑结果都不同，
那种代码没法写测试，也没法在文档里贴输出。上面那个 `[63, 49, 64, 72, 37]`
是 JDK 25 用种子 42 跑出来的——换 JDK 版本可能不同，所以测试里只断言范围。

**无限流必须配 `limit`。** `Stream.generate` 和 `Stream.iterate` 会**永远**造下去：

```java
Stream.generate(() -> "重复").limit(3)        // 不写 limit 就永远不结束
Stream.iterate(1, n -> n * 2).limit(6)        // 1, 2, 4, 8, 16, 32
```

忘了 `limit` 的后果不是报错，是程序挂在那儿不停算（或者内存爆掉）。
好消息是 `iterate` 有个三参数版本，自带停止条件，不用 `limit`：

```java
Stream.iterate(1, n -> n <= 5, n -> n + 1)    // 1, 2, 3, 4, 5
```

---

## 五、中间操作：只描述「怎么变」，都不立刻执行

中间操作有一堆，但可以按「改变元素个数」和「改变元素类型」分成几类。

### 5.1 `filter` / `distinct` / `limit` / `skip`

```terminal
========== 6. 中间操作：filter / distinct / limit / skip ==========
原始数据（8 个人）：[Alice(30,美国), Bob(17,英国), Cindy(25,中国), David(17,英国), Eve(35,美国), Frank(25,中国), Grace(30,美国), Cindy(25,中国)]
filter(age > 18)          → 6 个：[Alice(30,美国), Cindy(25,中国), Eve(35,美国), Frank(25,中国), Grace(30,美国), Cindy(25,中国)]
distinct()                → 7 个：[Alice(30,美国), Bob(17,英国), Cindy(25,中国), David(17,英国), Eve(35,美国), Frank(25,中国), Grace(30,美国)]（Cindy 重了，剩一个）
limit(3)                  → [Alice(30,美国), Bob(17,英国), Cindy(25,中国)]
skip(2).limit(3)          → [Cindy(25,中国), David(17,英国), Eve(35,美国)]
skip 超过长度不报错，只是空：0 个
```

| 方法 | 作用 | 元素个数 |
|---|---|---|
| `filter(predicate)` | 留下满足条件的 | 变少 |
| `distinct()` | 去掉重复的 | 变少 |
| `limit(n)` | 只要前 n 个 | 变少（≤ n） |
| `skip(n)` | 跳过前 n 个 | 变少 |

**`distinct()` 靠 `equals` / `hashCode` 去重。** 两个 Cindy 内容一模一样，
所以留下一个。这件事**不是自动的**——`Person` 必须重写过这两个方法。
要是没重写，`distinct()` 会认为这是两个不同的人（两个不同对象），一个都去不掉。
这是[继承与多态](./05-继承与多态.md)里那个契约的直接应用。

**`skip` 超过长度不报错。** `skip(100)` 只是得到一条空流，
不像 `List.get(100)` 那样抛 `IndexOutOfBoundsException`。这算是好事，但也意味着
「跳过头了」这种 bug 不会自己暴露出来。

### 5.2 `map` 与 `flatMap`：一对一 vs 扁平化

这是最常被搞混的一对。先看数据：一个订单里有一串商品。

```java
List<Order> orders = List.of(
        new Order("A", List.of("键盘", "鼠标")),
        new Order("B", List.of("显示器")),
        new Order("C", List.of("鼠标", "耳机", "键盘")));
```

```terminal
========== 7. 中间操作：map 与 flatMap 的区别 ==========
map(Order::items)     → [[键盘, 鼠标], [显示器], [鼠标, 耳机, 键盘]]
  还是 3 个元素，只是每个元素里套了一个 List
flatMap(items.stream) → [键盘, 鼠标, 显示器, 鼠标, 耳机, 键盘]
  6 个元素，全都摊平到一层了（3 个订单：2 + 1 + 3）
摊平之后 distinct() 才有意义 → [键盘, 鼠标, 显示器, 耳机]
拆字符串也是一样       → [甲, 乙, 丙, 丁]
```

**`map` 是一对一。** 进来 3 个订单，出去还是 3 个元素。
每个 `Order` 被换成了它的 `items`——一个 `List<String>`。
结果就是「流里套着 List」，类型是 `Stream<List<String>>`。

**`flatMap` 是一对多，并且摊平。** 它把每个元素换成一个流，
再把这些流**首尾接起来**合成一条。3 个订单 → 2 + 1 + 3 = 6 个商品，
全都在同一层。类型是 `Stream<String>`。

用一句话记：

> `map`：每个元素换成**一个**东西。
> `flatMap`：每个元素换成**一串**东西，然后把所有串接成一串。

**为什么 `flatMap` 这么重要？** 因为只有摊平之后，后面的操作才作用在**真正的元素**上。
对比一下 `distinct()` 的效果：在 `Stream<List<String>>` 上调用，
比的是「两个 List 内容是否相同」，没什么用；摊平之后，
比的是「两个商品名是否相同」，这才是我们想要的 `[键盘, 鼠标, 显示器, 耳机]`。

`flatMap` 的入参也不一定是集合，任何能变成流的东西都行：

```java
Stream.of("甲,乙", "丙,丁").flatMap(s -> Arrays.stream(s.split(",")))   // [甲, 乙, 丙, 丁]
```

### 5.3 `mapToInt` / `mapToLong` / `mapToDouble`：转成数值流

```terminal
========== 8. 中间操作：mapToInt 转成数值流 ==========
mapToInt(Person::age).sum()     = 204
mapToInt(Person::age).average() = 25.5
summaryStatistics()             = IntSummaryStatistics{count=8, sum=204, min=17, average=25.500000, max=35}
数值流 .boxed() 转回对象流      = [30, 17, 25]
mapToInt(..).toArray()          = [30, 17, 25, 17, 35, 25, 30, 25]
IntStream 自己也能直接造         = [1, 4, 9, 16, 25]
```

这里有个很多人第一次写 Stream 会撞的墙：

```java
people().stream().map(Person::age).sum();   // 编译不过：Stream<Integer> 没有 sum()
```

为什么？因为 `Stream<Integer>` 里的元素是 **`Integer` 对象**，
而 `sum` / `average` / `summaryStatistics` 这些方法长在
`IntStream`、`LongStream`、`DoubleStream` 上——它们是**数值流 Numeric Stream**，
里面装的是真正的 `int` / `long` / `double`，没有装箱开销。

所以要用 `mapToInt` 换一条流：

```java
people().stream().mapToInt(Person::age).sum()          // 204
people().stream().mapToInt(Person::age).average()      // OptionalDouble[25.5]
```

顺便说清楚为什么 Java 要专门搞出数值流。原因是[泛型](./06-泛型.md)的
类型参数**只能是引用类型**，`Stream<int>` 这种写法根本不存在：

```terminal
$ javac -encoding UTF-8 -d out -sourcepath src/main/java \
    src/main/java/dev/javalearning/stream/broken/PrimitiveTypeArg.java
src/main/java/dev/javalearning/stream/broken/PrimitiveTypeArg.java:23: error: unexpected type
        Stream<int> numbers = Stream.of(1, 2, 3);   // ← 编译不过：类型参数不能是基本类型
               ^
  required: reference
  found:    int
1 error
```

数值流就是给基本类型开的直通车：既有 `int` 的性能，又有链式操作的方便。

反方向就是 `boxed()`，把数值流转回对象流；`toArray()` 则直接拿一个 `int[]`：

```java
people().stream().mapToInt(Person::age).boxed()    // Stream<Integer>
people().stream().mapToInt(Person::age).toArray()  // int[]
```

`summaryStatistics()` 一次把五个统计量都算出来，需要好几个指标时省事：

```java
IntSummaryStatistics{count=8, sum=204, min=17, average=25.500000, max=35}
```

### 5.4 `sorted`：自然序、比较器、`reversed`

```terminal
========== 9. 中间操作：sorted 与比较器 ==========
sorted() 自然序        → [Alice, Bob, Cindy, David, Eve, Frank, Grace]
sorted(comparingInt(age)) → [Bob(17,英国), David(17,英国), Cindy(25,中国), Frank(25,中国), Alice(30,美国), Grace(30,美国), Eve(35,美国)]
  年龄相同的 Alice/Grace 保持原来的先后——sorted 是稳定排序
reversed()             → [Eve(35,美国), Alice(30,美国), Grace(30,美国), Cindy(25,中国), Frank(25,中国), Bob(17,英国), David(17,英国)]
thenComparing(name)    → [Bob(17,英国), David(17,英国), Cindy(25,中国), Frank(25,中国), Alice(30,美国), Grace(30,美国), Eve(35,美国)]
```

**`sorted()` 无参版要求元素实现 `Comparable`。** 它能排 `String`、`Integer`，
但**排不了 `Person`**——一个人没有「天然的大小」。
`Person` 里我们故意没实现 `Comparable`，因为硬给它排一个顺序只会让调用方困惑。

这里有个**必须说清楚的地方**，我自己一开始也以为是这样：`sorted()` 排
不可比较的对象，**不是编译错误，是运行错误**。代码是能编过的，
要到跑起来才炸：

```terminal
但无参 sorted() 排 Person 会炸——而且要到运行期才知道：
  ClassCastException: class Person cannot be cast to class java.lang.Comparable
  （完整 message 还会带 module / loader 信息，每次都不同）
只有一个元素时却不炸：[Alice(30,美国)]   ← 数据一多就炸，测试只放一条就发现不了
```

编译器**放你过去**了——因为 `sorted()` 的签名对元素类型没有任何约束，
它内部是拿到元素才强转成 `Comparable` 的。所以：

- 报错要到**运行期**才出现，而且是 `ClassCastException`，不是编译错误
- 更阴的是**只有一个元素时它不炸**——因为没有需要比较的对手。
  这种「数据一多就炸」的 bug，测试里只放一条数据就发现不了

这也是为什么排自定义对象**应该**显式给比较器，而不是依赖自然序。

**排自定义对象要显式给比较器。** `Comparator.comparingInt(Person::age)`
就是「按年龄这个 int 字段比」。注意是 `comparingInt` 而不是 `comparing`——
后者要装箱，前者不会。

**`sorted` 是稳定排序。** 看输出：`Alice(30)` 在原来列表里排在 `Grace(30)` 前面，
排完之后还是前面。年龄相同的元素**保持原来的相对顺序**。这一点在处理
「先按 A 排、再按 B 排」的需求时很关键。

**`reversed()` 是包一层新比较器**，不改原来那个：

```java
Comparator.comparingInt(Person::age).reversed()
```

**多级比较用 `thenComparing`。** 年龄一样就按名字：

```java
Comparator.comparingInt(Person::age).thenComparing(Person::name)
```

看输出，`Bob` 和 `David` 都是 17 岁，按名字排成了 `Bob, David`。
而单用 `comparingInt(age)` 时，它俩的顺序由**原列表顺序**决定。

### 5.5 串起来：五步读成一句话

现在把前面学的拼起来：

```java
List<String> chain = people().stream()
        .filter(p -> p.age() >= 25)                                  // 25 岁及以上
        .distinct()                                                  // 去掉重复的人
        .sorted(Comparator.comparingInt(Person::age).reversed())     // 年龄从大到小
        .map(Person::name)                                           // 只要名字
        .limit(3)                                                    // 只要前 3 个
        .collect(Collectors.toList());
```

```terminal
========== 10. 串起来：filter + distinct + sorted + map + limit ==========
结果：[Eve, Alice, Grace]
五步读下来就是一句话：25 岁以上的不重复的人，按年龄从大到小取前三个

for 循环写同一件事要这么长：
    List<Person> adults = new ArrayList<>();
    for (Person p : people) if (p.age() >= 25) adults.add(p);
    Set<Person> seen = new LinkedHashSet<>(adults);      // 还得自己想怎么去重
    adults = new ArrayList<>(seen);
    adults.sort(Comparator.comparingInt(Person::age).reversed());  // 再想怎么排
    List<String> names = new ArrayList<>();
    for (int i = 0; i < 3 && i < adults.size(); i++) names.add(adults.get(i).name());
意图散在 6 个语句里，改一个需求要在 6 个地方找
```

**五步读下来就是一句话**，而且每一步都能单独看懂。
下面那段 for 版我实际编译运行过，结果和 Stream 版完全一致——
所以这不是「Stream 能做到 for 做不到」，而是**同样的事，哪个更好读**。

这里大家思考一个问题：**中间操作的顺序能随便调吗？**

不能。看一个例子：

```java
people().stream().limit(2).sorted(按年龄倒序)    // 先截前两个（Alice 30, Bob 17），再排 → [Alice, Bob]
people().stream().sorted(按年龄倒序).limit(2)    // 先全局排，再取前两个 → [Eve(35), Alice(30)]
```

两条链的差别不是风格问题，是**结果完全不同**。原因是惰性求值下，
元素是「一个一个流过整条链」的：`limit(2)` 在前，它下游就只见到前两个元素，
`sorted` 也就只能在这两个里面排。

所以链不是「一堆操作的集合」，而是**一条有方向的流水线**，顺序有语义。

---

## 六、终端操作：一按开关，水才流

终端操作分两种：**短路 short-circuit** 的和**不短路**的。
短路的找到答案就收工，不短路的必须把整条流走完。

### 6.1 短路操作：找到就停

```terminal
========== 11. 终端操作：短路——找到就停 ==========
    anyMatch 检查到：Alice
anyMatch(age > 18)   = true   ← 第一个人就满足，后面 7 个没被检查
    allMatch 检查到：Alice
    allMatch 检查到：Bob
allMatch(age > 18)   = false  ← 到 Bob 就失败了，后面 6 个没被检查
    noneMatch 检查到：Alice
    noneMatch 检查到：Bob
    noneMatch 检查到：Cindy
    noneMatch 检查到：David
    noneMatch 检查到：Eve
    noneMatch 检查到：Frank
    noneMatch 检查到：Grace
    noneMatch 检查到：Cindy
noneMatch(age > 100) = true  ← 没有一个能提前判定，8 个全查了
noneMatch(age > 30)  = false  ← 到第 5 个 Eve 就满足，后面 3 个不看：同一操作也能短路
findFirst(age < 18)  = Bob
findAny(age < 18)    = Bob   ← 顺序流下它也是第一个，别依赖这个
找不到的时候         = 没有（Optional 空，不会抛异常）
```

我在每条链前面插了 `peek` 来打印「检查到了谁」，这样就能**数出来**它到底看了几个：

| 操作 | 结果 | 实际检查了几个 | 为什么 |
|---|---|---|---|
| `anyMatch(age > 18)` | `true` | **1** | Alice 就满足，后面没必要看 |
| `allMatch(age > 18)` | `false` | **2** | Alice 过了、Bob 挂了，一个反例足以定论 |
| `noneMatch(age > 100)` | `true` | **8** | 必须看完所有人才敢说「一个都没有」 |
| `noneMatch(age > 30)` | `false` | **5** | 到 Eve 就满足了，推翻「一个都没有」，可以收工 |

最后两行是**同一个操作**，检查个数却不同——这就是下面要说的：**短不短路，取决于数据。**

**短路 Short-circuit** 就是这个意思：能提前定论就立刻停，不把整条流走完。
`anyMatch` / `allMatch` / `noneMatch` / `findFirst` / `findAny` 都是短路操作。

但上面最后两行值得多看一眼：**同一个 `noneMatch`，检查个数却不同。**
所以要分清楚「操作具备短路能力」和「这一次恰好用得上」：

`noneMatch` 一旦**找到**一个满足条件的元素，立刻就能返回 `false`——
这时候它短路了。但问「有没有人年龄大于 100」时，前 8 个人一个都不是，
**没法提前定论**，只能看完所有人才敢说「一个都没有」。这不是实现不好，
是逻辑上躲不掉。

```java
// 找得到 → 找到就停
people().stream().noneMatch(p -> p.age() > 30);    // 到第 5 个 Eve 就满足，后面 3 个不看

// 找不到 → 必须全查完才知道
people().stream().noneMatch(p -> p.age() > 100);   // 8 个全查，才敢返回 true
```

第一行我实测过：返回 `false`，`peek` 显示只检查了 **5 个**
（Alice、Bob、Cindy、David、Eve），Eve 一满足就收工了。
**同一个操作，短不短路取决于数据。**

**`limit` 也是短路的**（它是中间操作，但具备短路能力）。
这一点配合惰性求值能省很多事：

```java
people().stream().peek(计数).limit(2)...   // 只流过了 2 个元素，不是 8 个
```

`limit(2)` 告诉上游「我只要两个」，上游就真的只造两个。
如果数据源是「从数据库逐条读」或者无限流，这个性质就是能不能用的区别了。

**`findFirst` 和 `findAny` 的区别**，在顺序流里看不出来（都是 `Bob`）。
`findAny` 的承诺是「随便给我一个」——在**并行流**下它可能返回任何一个，
换来的是不用维护顺序、更快。所以在顺序流里你**不要依赖** `findAny` 返回第一个，
那是实现细节，哪天换了实现就变了。并行流是[下一课](./11-并行流.md)的事。

**找不到的时候返回空 `Optional`，不抛异常。** 这是刻意设计的：

```java
people().stream().filter(p -> p.age() > 100).findFirst()   // Optional.empty
```

要是像 `List.get` 那样抛异常，你就得为「正常情况下的没找到」写 try-catch，
很难看。`Optional` 在类型上就告诉你「这里可能没有」，逼你处理。
细节见 [Optional](./08-Optional.md)。

### 6.2 聚合：`count` / `max` / `min` / `sum` / `average`

```terminal
========== 12. 终端操作：聚合 count / max / min / sum / average ==========
count()   → 6
max(age)  → Eve(35,美国)
min(age)  → Bob(17,英国)   ← 17 岁有两个，返回先遇到的那个
sum()     → 204
average() → 25.5
空流的 average 是空 Optional，不是 0：OptionalDouble.empty
```

注意 `max` / `min` 的返回值是 `Optional`，因为它们**可能没有结果**——
空流里没有最大值。`count` / `sum` 就不一样，空流返回 `0`，这是有意义的答案。

`min(age)` 那行要留意：17 岁的有 Bob 和 David 两个，
返回的是**先遇到的那个**。`min` 和 `sorted` 一样是稳定的，
但「两个并列最小取谁」这种细节，能不依赖就别依赖。

最后一行的对比值得记住：

```java
Stream.<Person>empty().mapToInt(Person::age).average()   // OptionalDouble.empty
Stream.<Person>empty().mapToInt(Person::age).sum()       // 0
```

**「没有平均值」和「平均值是 0」是两件事。** 前者是「没有数据」，
后者是「数据加起来是 0」。用 `0` 去代表「没有」是很多 bug 的源头，
所以 `average()` 返回 `OptionalDouble`。

### 6.3 `reduce`：把整条流压成一个值

`count` / `sum` / `max` 本质上都是 `reduce` 的特例。`reduce` 有三个版本：

```terminal
========== 13. 终端操作：reduce 规约 ==========
reduce(Integer::max)     → 35
reduce(0, Integer::sum)  → 204
reduce(起点, 累加, 合并) → "Alice Bob Cindy David Eve Frank Grace Cindy"
  顺序流下 combiner 被调用 0 次——它是给并行流分块合并用的
```

**单参数版：结果也是 `Optional`。**

```java
people().stream().map(Person::age).reduce(Integer::max)   // Optional[35]
```

因为没有起点，空流就真的没有结果，只能给 `Optional`。

**双参数版：给一个起点，结果不再是 `Optional`。**

```java
people().stream().map(Person::age).reduce(0, Integer::sum)   // 204
```

起点 `0` 叫 **identity 单位元**——它必须满足「拿它和任何值结合都不改变那个值」
（`0 + x == x`）。有了它，空流也能返回 `0`。

**三参数版：多一个 combiner。**

```java
reduce("",                                  // 起点
       (acc, name) -> acc + name + " ",     // accumulator 累加器
       (a, b) -> a + b)                     // combiner 合并器
```

这个版本是为了**并行流**存在的。并行时数据被切成几块，
每块各自用一个 accumulator 累加；最后再用 combiner 把几块结果**合起来**。

所以顺序流下，combiner **一次都不会被调用**（输出里是 0 次）——
因为只有一块，没什么好合的。这不是 bug，是设计。

那什么时候必须用三参数版？**当结果类型和元素类型不同的时候。**
上面那个例子里元素是 `String`、起点是 `String`，其实两参数版也够用；
但如果你想「元素是 `String`，结果是个 `int`」，两参数版就编不过：

```java
// 编不过：两参数版要求起点类型和结果类型一致
Stream.of("a", "bb", "ccc").reduce(0, (acc, s) -> acc + s.length());

// 三参数版才行：起点是 int，元素是 String，结果也是 int
Stream.of("a", "bb", "ccc").reduce(0, (acc, s) -> acc + s.length(), (a, b) -> a + b);
```

简单说：**顺序流下你几乎用不到三参数版**，但看到它别慌，那是并行流的接口。

### 6.4 `collect`：收集成集合

```terminal
========== 14. 终端操作：collect 收集 ==========
toList()        → [Alice, Bob, Cindy, David, Eve, Frank, Grace, Cindy]
toSet()         → [中国, 美国, 英国]   ← 打印前排过序，HashSet 本身不保证顺序
toMap 遇到重名   → Duplicate key Cindy (attempted merging values 25 and 25)
给合并函数之后  → {Alice=30, Bob=17, Cindy=25, David=17, Eve=35, Frank=25, Grace=30}
groupingBy(country) → {中国=[Cindy, Frank, Cindy], 美国=[Alice, Eve, Grace], 英国=[Bob, David]}
groupingBy + counting → {中国=3, 美国=3, 英国=2}
partitioningBy(age>18) → true  [Alice, Cindy, Eve, Frank, Grace, Cindy]
                         false [Bob, David]
joining()       → AliceBobCindyDavidEveFrankGraceCindy
joining(带分隔和括号) → [Alice, Bob, Cindy, David, Eve, Frank, Grace, Cindy]
summarizingInt  → IntSummaryStatistics{count=8, sum=204, min=17, average=25.500000, max=35}
```

**`toList()` / `toSet()`：** 收成集合。`toSet()` 会去重（同样靠 `equals`/`hashCode`）。

**`toMap()` 遇到重复 key 会直接抛异常：**

```java
people().stream().collect(Collectors.toMap(Person::name, Person::age));
// IllegalStateException: Duplicate key Cindy (attempted merging values 25 and 25)
```

这是个**好设计**——两个 Cindy 该留哪个？Stream 不知道，所以它不猜，
直接把问题甩给你。你给个合并函数就行了：

```java
Collectors.toMap(Person::name, Person::age, (a, b) -> a)   // 撞了就留先来的
```

注意这里 `(a, b) -> a` 的意思是「已有值 a 和新值 b 撞了，保留 a」。
到底该保留谁，取决于业务，别习惯性地都写 `(a, b) -> a`。

**`groupingBy` 分组。** 按国家把人分组：

```java
Collectors.groupingBy(Person::country, Collectors.mapping(Person::name, Collectors.toList()))
// {中国=[Cindy, Frank, Cindy], 美国=[Alice, Eve, Grace], 英国=[Bob, David]}
```

第二个参数是「每组怎么收」。不写的话默认收成 `List`，
但那样拿到的是 `List<Person>`；这里用 `mapping` 先把人变成名字，拿到 `List<String>`。

**这里有个必须提醒的坑：`groupingBy` 返回的是 `HashMap`，迭代顺序没有承诺。**
上面输出里 `中国` 在 `美国` 前面，看起来挺有规律，但**不要依赖它**。
如果输出需要稳定（比如要贴进文档、或者要写测试），
打印前换成 `TreeMap` 排一下：

```java
System.out.println(new TreeMap<>(byCountry));
```

同理 `toSet()` 那行——代码里先套了一层 `TreeSet` 才打印。
不这么做的话，`[中国, 美国, 英国]` 换个 JDK 版本就可能变成别的顺序。
我在文档里贴的所有分组输出都是这么处理的，跑两次确认过一致。

**`partitioningBy` 只分两组。** key 一定是 `true` / `false`，
而且**两组一定都在**——就算一组是空的，key 也在，值是空 List。
这一点和 `groupingBy` 不同（`groupingBy` 没有的组就不会出现）。
所以 `partitioningBy` 特别适合「符合条件 / 不符合条件」这种二分。

**`joining` 拼字符串。** 三参数版 `joining(", ", "[", "]")` 是分隔符、前缀、后缀。
注意 `joining` 只接受 `CharSequence` 流，要先把对象 `map` 成字符串。

**`summarizingInt` 一次拿五个统计量。** 前面数值流那节见过它的兄弟，
这里是 `collect` 版本。

### 6.5 `forEach`：遍历

```terminal
========== 15. 终端操作：forEach 遍历 ==========
forEach：
    Alice 30
    Bob 17
    Cindy 25
forEachOrdered（顺序流下看不出区别，并行流下才保证顺序）：
    Alice 30
    Bob 17
    Cindy 25
    第 0 次回调，元素 = Alice(30,美国)，当前 size = 8
    第 1 次回调，元素 = Bob(17,英国)，当前 size = 8
    第 2 次回调，元素 = David(17,英国)，当前 size = 7
    第 3 次回调，元素 = Frank(25,中国)，当前 size = 6
    第 4 次回调，元素 = Grace(30,美国)，当前 size = 6
    第 5 次回调，元素 = Cindy(25,中国)，当前 size = 6
    第 6 次回调，元素 = null，当前 size = 6
边遍历边删 → NullPointerException（不是 ConcurrentModificationException！）
边遍历边加 → ConcurrentModificationException
所以别去猜「会抛哪个异常」——只要在流里改了源集合，行为就不可依赖
```

`forEach` 是最简单的终端操作，但它是**终端操作**——
用了它，这条流就消费掉了，不能再接别的。

`forEachOrdered` 在顺序流里和 `forEach` 完全一样，
差别只在并行流：`forEach` 不保证顺序，`forEachOrdered` 保证。

**最后两行是这一课最值钱的部分之一。** 我原本以为
「在流里删源集合的元素」会抛 `ConcurrentModificationException`——
毕竟这是 `for-each` 里删元素的经典错误。**结果不是。**

真实抛出来的是 `NullPointerException`，位置还很奇怪：

```
Exception in thread "main" java.lang.NullPointerException:
    Cannot invoke "dev.javalearning.stream.Person.age()" because "<parameter2>" is null
```

为什么？我去翻了 JDK 的 `ArrayList` 源码。它的
`ArrayListSpliterator.forEachRemaining` 是这么写的（下面是节选，
省掉了 `action == null` 检查、`expectedModCount` 分支和 `index` 边界判断，
`...` 处就是省略的部分）：

```java
public void forEachRemaining(Consumer<? super E> action) {
    int i, hi, mc;
    Object[] a;
    if ((a = elementData) != null) {
        if ((hi = fence) < 0) {
            mc = modCount;
            hi = size;              // ← 元素个数在这一刻就被「缓存」下来了
        }
        ...
        for (; i < hi; ++i) {
            E e = (E) a[i];         // ← 直接按下标取数组，没有 modCount 检查
            action.accept(e);
        }
        if (modCount == mc)         // ← 检查放在整个循环「之后」
            return;
    }
    throw new ConcurrentModificationException();
}
```

关键在于：**它先把 `size` 存进 `hi`，然后按数组下标遍历，
`modCount` 的检查放在循环之后。**

我一开始以为「删掉 Bob 一个，然后走到末尾取到 null」。
实际打出来是这样（上面第 15 节输出里就有，`hi` 一开始是 8）：

```terminal
    第 0 次回调，元素 = Alice(30,美国)，当前 size = 8
    第 1 次回调，元素 = Bob(17,英国)，当前 size = 8
    第 2 次回调，元素 = David(17,英国)，当前 size = 7
    第 3 次回调，元素 = Frank(25,中国)，当前 size = 6
    第 4 次回调，元素 = Grace(30,美国)，当前 size = 6
    第 5 次回调，元素 = Cindy(25,中国)，当前 size = 6
    第 6 次回调，元素 = null，当前 size = 6
边遍历边删 → NullPointerException（不是 ConcurrentModificationException！）
```

比我想的绕：**删了两个人**（Bob 和 David，都是 17 岁），
而且 `size` 一路掉到 6，循环却还按 `hi = 8` 往下走。逐帧看：

```
hi = 8，按下标遍历
  i=1  Bob 满足条件 → 删掉。数组整体前移，size 变 7
  i=2  现在下标 2 站的是 David（他原本在 3）→ 他也满足，也被删掉！size 变 6
  i=3  下标 3 是 Frank，跳过 Eve 了 ← 注意：Eve 被「跳过」了
  i=4  Grace
  i=5  Cindy
  i=6  null（后面的人早被前移走了）→ NPE
```

这段有两个值得记住的教训，比「会抛异常」更重要：

1. **删元素会让后面的元素前移，于是循环会「跳过」元素。** 上面 Eve 就没被检查到。
   就算不炸，结果也是错的。
2. **它删了不该删的。** David 被删掉纯粹是因为他前移到了刚检查过的下标上。

**而 `ConcurrentModificationException` 反而不会抛**，因为循环在 `null` 上就炸了，
根本走不到最后那个 `modCount` 检查。

那什么情况下会抛 `ConcurrentModificationException` 呢？
**往列表里加元素**。因为加元素不会让某个位置变 `null`，
循环能正常走完，然后撞上最后那句检查——输出里「边遍历边加」那一行正是如此。

这里要说的道理不是「背下哪个操作抛哪个异常」，恰恰相反：

> **只要在流里改了源集合，行为就不可依赖。**

同一条 `forEach`，删元素炸 `NPE`、加元素炸 `CME`，
换个 `LinkedList`、换个中间操作（比如加个 `limit`）又是另一回事。
**别去猜，别去试**——要改集合就别用流，或者改完再建流。

这段经历我也写进了 `StreamDemo` 的注释里。讲课的人也会错，
但这个错误本身比我原本以为的那个结论更有价值。

---

## 七、`collect` 不是魔法：`Collector` 的四个组件

`collect(Collectors.toList())` 看起来像魔法——凭什么传个 `Collectors.toList()`
它就知道该怎么收？我们自己写一个就明白了。

```terminal
========== 16. collect 不是魔法：Collector 的四个组件 ==========
自己写的 Collector → Alice Bob Cindy David Eve Frank Grace Cindy
  顺序流下 combiner 被调用 0 次——它只在并行流里才需要
Collectors.toList() 就是 supplier=ArrayList、accumulator=add、combiner=addAll 的同一个套路
```

一个 `Collector` 就是四个函数：

```java
Collector<Person, StringBuilder, String> nameCollector = Collector.of(
        StringBuilder::new,                            // ① supplier：造一个空容器
        (sb, p) -> sb.append(p.name()).append(' '),    // ② accumulator：把一个元素塞进容器
        (a, b) -> a.append(b),                         // ③ combiner：两个容器合并
        sb -> sb.toString().trim());                   // ④ finisher：容器变成最终结果
```

| 组件 | 类型 | 干什么 | `toList()` 里的对应 |
|---|---|---|---|
| supplier | `Supplier<A>` | 造一个空的结果容器 | `ArrayList::new` |
| accumulator | `BiConsumer<A, T>` | 把一个元素加进容器 | `List::add` |
| combiner | `BinaryOperator<A>` | 合并两个容器（并行用） | `addAll` |
| finisher | `Function<A, R>` | 容器 → 最终结果 | 恒等（不转换） |

所以 `collect(Collectors.toList())` 展开来就是：

```
造一个 ArrayList → 把每个元素 add 进去 → （并行时才合并）→ 直接返回这个 ArrayList
```

**没有魔法。** 而且这也解释了前面 `reduce` 三参数版的 combiner 为什么顺序流下用不到——
collector 的 combiner 是一模一样的道理，只在并行分块合并时才需要。

那如果容器本身就是最终结果（比如 `ArrayList` 就是要的 `List`），
finisher 那一环是多余的。JDK 里有个优化：如果 `Collector` 声明
`characteristics()` 包含 `IDENTITY_FINISH`，`collect` 就会跳过 finisher。
`Collectors.toList()` 正是这样，所以你拿到的是 `ArrayList` 本身，没有多复制一层。

---

## 八、编译不过的反面教材

下面这些住在 `broken/` 包里（`build.gradle.kts` 已整包排除，
在 IDE 里看到它们标红是**正常的**）。它们的共同点是：
**你写的时候觉得没问题，编译器不这么想。**

### 8.1 想在 lambda 里改外部局部变量

```java
int sum = 0;
List.of(1, 2, 3).stream().forEach(n -> sum += n);   // ← 编译不过
```

```terminal
$ javac -encoding UTF-8 -d out -sourcepath src/main/java \
    src/main/java/dev/javalearning/stream/broken/LambdaCapture.java
src/main/java/dev/javalearning/stream/broken/LambdaCapture.java:23: error: local variables referenced from a lambda expression must be final or effectively final
        List.of(1, 2, 3).stream().forEach(n -> sum += n);   // ← 编译不过
                                               ^
1 error
```

**`local variables referenced from a lambda expression must be final or effectively final`**

原因在[Lambda 表达式](./07-Lambda表达式.md)里讲过：Lambda 捕获的是变量的**值**，
不是变量本身。要是允许你改，多线程下这个「值」到底是谁的就不清楚了。
所以编译器干脆禁止。

**那想累加怎么办？** 别用外部变量，让流自己算：

```java
people().stream().mapToInt(Person::age).sum()   // 干净，而且能并行
```

（真要用外部变量也有办法——用一个单元素数组 `int[] box = {0}`，
`box[0] += n` 改的是数组的**元素**，不是变量本身，编译器就放行了。
`StreamDemo` 第 18 节演示了，但那是绕开限制，不推荐。）

### 8.2 以为对象流也有 `sum()`

```java
int total = Stream.of(1, 2, 3).sum();   // ← 编译不过
```

```terminal
$ javac -encoding UTF-8 -d out -sourcepath src/main/java \
    src/main/java/dev/javalearning/stream/broken/StreamSum.java
src/main/java/dev/javalearning/stream/broken/StreamSum.java:23: error: cannot find symbol
        int total = Stream.of(1, 2, 3).sum();
                                      ^
  symbol:   method sum()
  location: interface Stream<Integer>
1 error
```

**`cannot find symbol: method sum()` / `location: interface Stream<Integer>`**

`sum()` 长在数值流上，`Stream<Integer>` 没有。先 `mapToInt(...)`。
这是刚学 Stream 时最常见的错误之一，报错信息倒是很清楚。

### 8.3 以为流就是集合

```java
List<Integer> numbers = IntStream.range(1, 5);   // ← 编译不过
```

```terminal
$ javac -encoding UTF-8 -d out -sourcepath src/main/java \
    src/main/java/dev/javalearning/stream/broken/IntStreamIsNotList.java
src/main/java/dev/javalearning/stream/broken/IntStreamIsNotList.java:24: error: incompatible types: IntStream cannot be converted to List<Integer>
        List<Integer> numbers = IntStream.range(1, 5);
                                               ^
1 error
```

**`incompatible types: IntStream cannot be converted to List<Integer>`**

流**不是**集合。它不存数据，是一条「数据从哪来、怎么变、到哪去」的管道描述。
要变成集合必须走一次终端操作。

### 8.4 想让流直接装基本类型

```java
Stream<int> numbers = Stream.of(1, 2, 3);   // ← 编译不过
```

```terminal
$ javac -encoding UTF-8 -d out -sourcepath src/main/java \
    src/main/java/dev/javalearning/stream/broken/PrimitiveTypeArg.java
src/main/java/dev/javalearning/stream/broken/PrimitiveTypeArg.java:23: error: unexpected type
        Stream<int> numbers = Stream.of(1, 2, 3);   // ← 编译不过：类型参数不能是基本类型
               ^
  required: reference
  found:    int
1 error
```

**`unexpected type / required: reference / found: int`**

泛型的类型参数只能是引用类型。这就是数值流存在的理由。

> 顺带记一笔我在写这一课时踩的坑：我本来还想加一个反面教材，
> 演示「`IntStream` 没有 `collect(Collector)`」：
>
> ```java
> IntStream.range(1, 5).collect(Collectors.toList());
> ```
>
> 这行**确实**编译不过，报错信息也很有教育意义（它只有三参数的 `collect`）。
> 但它会让 `javac` **崩溃**——退出码是 4，还会在 `/tmp` 里丢一个
> `javac.xxxx.args` 文件，最后多打一行 `printing javac parameters to: ...`。
> 我连跑三次都一样。
>
> 既然它连「一条干净的报错」都给不出来，我就把它换成了上面那四个。
> 记在这里是因为：**报错信息本身也是输出，也不该编**。

---

## 九、不推荐的用法

### 9.1 流用过一次就不能再用

```terminal
========== 17. 流用过一次就不能再用 ==========
第一次 count() = 6
第二次用同一个流 → IllegalStateException: stream has already been operated upon or closed

filtered.count() = 1
回头用 source → IllegalStateException: stream has already been operated upon or closed
所以要复用就复用「数据源」people()，每次都重新 .stream()
```

第二次用同一个流，抛：

```
java.lang.IllegalStateException: stream has already been operated upon or closed
```

这是**运行期**错误，编译器不拦，所以很容易漏。更隐蔽的是第二段：
`source` 是源流，`filtered` 是在它后面接出来的。输出里
`filtered.count()` 正常返回 `1`，但回头再用 `source` 就炸了——
**源流在 `filtered` 被创建的那一刻就已经废了**，根本不用等 `filtered` 被使用。
这个很容易让人意外。

**怎么办：复用数据源，不要复用流。** 每次要用就重新 `people().stream()`。
流是「一次性的管道」，不是「可以反复查的表」。

顺带回答第三节留下的问题：既然中间操作只是「记下来」，为什么不执行就不算消费？
答案比我想的更细——我去翻了下 JDK 的 `AbstractPipeline`，那个标志位叫
`linkedOrConsumed`，名字就是答案：**「接上了下游」和「被消费了」
是同一个标志位的两种触发方式**，谁先来谁把它置上。具体是：

- 建中间操作时，`AbstractPipeline` 的构造器把**上游**那句
  `previousStage.linkedOrConsumed = true` 置上；
- 执行终端操作时，`evaluate(...)` 里先检查、再把自己置上。

所以规则可以精确成一句话：

> **只有管道最末端那一节能用；一旦你在它后面接了任何东西，它就废了。**

我用四个小实验验证过（源流、中间流、末端流逐级接出来）：

| 操作 | 结果 |
|---|---|
| 建了流没用过，直接 `count()` | ✅ 能用 |
| 流后面接了 `filter`，再回头用**源流** | ❌ 已消费 |
| 链了两级，回头用**中间那一级** | ❌ 已消费 |
| 用**最末端**那一级 | ✅ 能用 |

注意第二行——**我还没执行任何终端操作，源流就已经废了**。
这比「终端操作才消费」更严格，也解释了为什么
「惰性」和「只能用一次」并不矛盾：惰性说的是**不执行**，
一次性说的是**接上或消费后作废**，两者管的不是同一件事。

### 9.2 别在流里修改外部状态

上面第 15 节已经看过真实后果了：删元素炸 `NPE`，加元素炸 `CME`，
而且**抛哪个异常取决于你做了什么、用的什么集合**。

除了改源集合，还有两种副作用要避免：

- **用外部变量累加**——编译不过（8.1 节）
- **在 `forEach` 里写数据库 / 发消息**——能跑，但顺序不保证，
  并行流下更是灾难。而且这些操作本质上就是「副作用」，
  放在 `forEach` 里等于把流当 `for` 用，白瞎了 Stream 的表达力

要收集结果就用 `collect`，要聚合就用 `reduce` / `sum`。
流是**无副作用**的工具，硬塞副作用进去，两边的好处都丢了。

### 9.3 数据量小的时候，Stream 不一定比 for 快

这一点必须说清楚，免得产生「Stream 更快」的错觉。

Stream 要建管道对象、要传 Lambda（虽然 JIT 会优化）、
数值流还要处理装箱。**在数据量小的时候，这些开销是主导的。**

我自己在本机跑过一个对照（同样的 `filter` + 求和，预热后取多次里最快的一次），
连跑三轮，结果**很不安定**：

| 数据量 | 三轮里 Stream / for 的耗时比 |
|---|---|
| 10 | 1.91x、2.49x、1.80x |
| 100 | 1.86x、2.62x、1.60x |
| 10000 | 0.35x、0.26x、**0.19x** |
| 1000000 | 5.52x、4.53x、4.00x |

小数据量（几十）那两行很一致：**Stream 慢两倍左右**。一百万那行也一致：
差距拉到 4~5 倍。但**中间那行反过来**——Stream 比 `for` 快了三四倍。

这不是我写错了，是这类微基准的真实面目：JIT 什么时候把哪段编译成机器码、
缓存什么温度、GC 有没有插一脚，都会左右结果。所以：

**我故意不给你一个精确到小数点的数字**——那种「事实」你复现不出来，
贴进文档只会误导。你该记住的是两个更稳的结论：

1. **小数据量别指望 Stream 提速**，用它是因为好读
2. **任何性能结论都要自己测**，别信别人（包括我）的微基准

反过来说，**「Stream 慢」也不该成为拒绝它的理由**——
除非这段代码在热点路径上、而且数据量很小，否则可读性赢。
真到了性能敏感的地方，先测，别猜。

### 9.4 别写超过五步的长链

```java
// 不好读
list.stream().filter(..).map(..).flatMap(..).distinct().sorted(..).limit(..).collect(..)

// 好读：拆成两步，给中间结果起个名字
List<Order> valid = orders.stream().filter(..).toList();
Map<String, List<Order>> grouped = valid.stream().collect(groupingBy(..));
```

链太长，好处就没了。**读一屏代码，还得记住前面每一步做了什么，
那和读一个长 `for` 循环没什么区别。** 有个经验法则：超过五步就拆，
给中间结果起个有意义的名字。名字本身就是注释。

### 9.5 `parallelStream()` 不要随便用

你可能已经注意到有个 `parallelStream()`。**先别碰它。**

它不是「自动加速」——它会开多个线程、切分数据、最后合并。
用错了（有副作用、有顺序依赖、数据量太小）结果会**错**，
而且错得很难查。这一课全程用的是顺序流，请保持。

并行流是[下一课](./11-并行流.md)专门要讲的东西，到时候再说。

---

## 十、这一课的术语表

- **Stream（流）**—— 一条「从哪来、怎么变、到哪去」的数据管道，不存数据
- **命令式 Imperative**—— 一步步说「怎么做」，`for` 循环是典型
- **声明式 Declarative**—— 只说「要什么」，Stream 是典型
- **创建 / 中间操作 / 终端操作**（creation / intermediate / terminal）—— Stream 的三个步骤
- **惰性求值 Lazy Evaluation**—— 中间操作只记录不执行，直到终端操作出现
- **短路 Short-circuit**—— 能提前定论就立刻停止，如 `anyMatch`、`findFirst`、`limit`
- **`filter`**—— 按条件保留元素
- **`map`**—— 一对一转换，元素个数不变
- **`flatMap`**—— 一对多转换并摊平，把「流里套流」压成一层
- **数值流 Numeric Stream**—— `IntStream` / `LongStream` / `DoubleStream`，装基本类型，有 `sum` / `average`
- **装箱 boxing**—— 基本类型 ↔ 包装类型（`int` ↔ `Integer`），有开销；`boxed()` 就是转回去
- **`distinct`**—— 按 `equals` / `hashCode` 去重
- **稳定排序 stable sort**—— 相等元素保持原有相对顺序，`sorted` 就是稳定的
- **`reduce` 规约**—— 把整条流压成一个值；三参数版的 combiner 只给并行流用
- **`collect` / `Collector`**—— 把流收进容器；`Collector` 由 supplier / accumulator / combiner / finisher 四个组件构成
- **`groupingBy` / `partitioningBy`**—— 分组 / 二分（后者 key 一定是 `true`/`false`）
- **`Optional`**—— 「可能有值，也可能没有」的容器，`findFirst` / `max` / `average` 都返回它
- **副作用 side effect**—— 改外部状态；流里做副作用会炸或结果不可预期
- **流只能消费一次**—— 管道接上下游或被执行后就作废，复用会抛 `IllegalStateException`
- **并行流 parallel stream**—— 多线程版的流，下一课的内容

---

## 十一、下一课

这一课我们一直在**顺序**处理数据：一条流水线，元素一个一个流过。

但你可能已经注意到几个「为并行埋的伏笔」：

- `reduce` 的三参数版为什么要有 combiner？
- `Collector` 的 combiner 顺序流下一次都不调用，留着干嘛？
- `findAny` 为什么要说「顺序流下也是第一个，别依赖」？
- `forEachOrdered` 和 `forEach` 在顺序流里没区别，那它存在的意义是什么？

这些全是**并行流**的接口。下一课我们就把它打开：
同样是 `stream()` 改一个字变成 `parallelStream()`，为什么结果会乱、
为什么会错、什么时候才真的变快——以及为什么它可能是 Java 里
**最容易被误用**的一个 API。

下期再见。
