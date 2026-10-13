package dev.javalearning.stream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * 第 10 课的示例代码：Stream API。
 *
 * <p>直接跑：{@code java src/main/java/dev/javalearning/stream/StreamDemo.java}
 * 每一节对应 {@code docs/10-Stream.md} 里的一节，输出都是真跑出来的。
 *
 * <p>全程只用同一份数据源：{@link #people()} 返回的 8 个人。
 * 其中 Cindy 出现了两次、内容完全相同——那是留给 {@code distinct()} 的。
 */
public class StreamDemo {

    public static void main(String[] args) throws IOException {
        section("1. 先让代码疼一次：for 循环版");
        List<Person> byLoop = adultsByLoop(people());
        System.out.println("年龄大于 18 的：" + byLoop);
        System.out.println("结果对，但读的人得把 for + if + add 在脑子里翻译成「筛出成年人」");

        section("2. Stream 版：一行说清意图");
        List<Person> byStream = people().stream()
                .filter(p -> p.age() > 18)
                .collect(Collectors.toList());
        System.out.println("同样的结果：" + byStream);
        System.out.println("和 for 版结果一样吗 → " + byLoop.equals(byStream));

        section("3. Stream 的三个步骤");
        Stream<Person> created = people().stream();                          // ① 创建
        Stream<String> intermediate = created.filter(p -> p.age() > 18)      // ② 中间操作
                .map(Person::name);
        List<String> terminal = intermediate.collect(Collectors.toList());   // ③ 终端操作
        System.out.println("① 创建      people.stream()      → 一个流，还没干活");
        System.out.println("② 中间操作  .filter(..).map(..)  → 还是流，可以继续往后接");
        System.out.println("③ 终端操作  .collect(toList())   → " + terminal);
        System.out.println("终端操作返回的不再是流，所以后面不能再接 .filter()");

        section("4. 惰性求值：没有终端操作，一行都不会跑");
        Stream<String> lazy = people().stream()
                .filter(p -> {
                    System.out.println("    filter 跑了：" + p.name());   // 只为证明，正经代码别这么写
                    return p.age() > 18;
                })
                .map(p -> {
                    System.out.println("    map    跑了：" + p.name());
                    return p.name();
                });
        System.out.println("链已经建好了，上面两个打印一句都没出现——因为还没有终端操作");
        System.out.println("现在补上终端操作 collect，注意打印是这时候才刷出来的：");
        List<String> eager = lazy.collect(Collectors.toList());
        System.out.println("collect 之后拿到 " + eager.size() + " 个名字：" + eager);

        section("5. 创建流的多种方式");
        showCreationWays();

        section("6. 中间操作：filter / distinct / limit / skip");
        showFilterFamily();

        section("7. 中间操作：map 与 flatMap 的区别");
        showMapAndFlatMap();

        section("8. 中间操作：mapToInt 转成数值流");
        showNumericStreams();

        section("9. 中间操作：sorted 与比较器");
        showSorting();

        section("10. 串起来：filter + distinct + sorted + map + limit");
        showChainedPipeline();

        section("11. 终端操作：短路——找到就停");
        showShortCircuit();

        section("12. 终端操作：聚合 count / max / min / sum / average");
        showAggregates();

        section("13. 终端操作：reduce 规约");
        showReduce();

        section("14. 终端操作：collect 收集");
        showCollect();

        section("15. 终端操作：forEach 遍历");
        showForEach();

        section("16. collect 不是魔法：Collector 的四个组件");
        showCustomCollector();

        section("17. 流用过一次就不能再用");
        showStreamReuse();

        section("18. 别在流里改外部状态");
        showSideEffects();
    }

    // ── 全程的数据源 ────────────────────────────────────────────────

    /** 8 个人，其中 Cindy 出现了两次且内容完全相同。 */
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

    // ── 1. 笨办法 ──────────────────────────────────────────────────

    /** 需求：找出所有年龄大于 18 岁的人，收集成新列表。 */
    static List<Person> adultsByLoop(List<Person> source) {
        List<Person> result = new ArrayList<>();
        for (Person p : source) {
            if (p.age() > 18) {
                result.add(p);
            }
        }
        return result;
    }

    // ── 5. 创建流 ──────────────────────────────────────────────────

    static void showCreationWays() throws IOException {
        // ① 集合：最常用的一条
        List<String> names = people().stream().map(Person::name).limit(3).collect(Collectors.toList());
        System.out.println("集合 .stream()      → " + names);

        // ② 数组
        int[] numbers = {3, 1, 4, 1, 5};
        System.out.println("数组 Arrays.stream  → "
                + Arrays.stream(numbers).boxed().collect(Collectors.toList()));
        System.out.println("  数组版是 IntStream，可以直接 sum() = " + Arrays.stream(numbers).sum());

        // ③ Stream.of：把几个零散的值临时凑成一条流
        System.out.println("Stream.of(\"甲\",\"乙\",\"丙\") → "
                + Stream.of("甲", "乙", "丙").collect(Collectors.toList()));

        // ④ Stream.builder：类型参数要写出来，否则会被推断成 Stream<Object>
        Stream<String> built = Stream.<String>builder().add("甲").add("乙").add("丙").build();
        System.out.println("Stream.builder()    → " + built.collect(Collectors.toList()));

        // ⑤ Files.lines：一行一个元素。用 try-with-resources 保证文件句柄被关掉
        Path temp = Files.createTempFile("stream-demo", ".txt");
        Files.writeString(temp, "苹果\n香蕉\n樱桃\n", StandardCharsets.UTF_8);
        try (Stream<String> lines = Files.lines(temp, StandardCharsets.UTF_8)) {
            System.out.println("Files.lines(文件)   → " + lines.collect(Collectors.toList()));
        } finally {
            Files.delete(temp);       // 临时文件用完就删，免得污染 /tmp
        }

        // ⑥ 数值范围：range 不含右端，rangeClosed 含
        System.out.println("IntStream.range(1,5)       → "
                + IntStream.range(1, 5).boxed().collect(Collectors.toList()));
        System.out.println("IntStream.rangeClosed(1,5) → "
                + IntStream.rangeClosed(1, 5).boxed().collect(Collectors.toList()));
        System.out.println("  1 到 100 的和 = " + IntStream.rangeClosed(1, 100).sum());

        // ⑦ 随机数：种子写死，输出才可复现。不写种子的话每次跑都不一样
        Random fixed = new Random(42);
        System.out.println("new Random(42).ints(5, 1, 100) → "
                + fixed.ints(5, 1, 100).boxed().collect(Collectors.toList()));

        // ⑧ 无限流：必须配 limit，否则永远不结束
        System.out.println("Stream.generate(..).limit(3)   → "
                + Stream.generate(() -> "重复").limit(3).collect(Collectors.toList()));
        System.out.println("Stream.iterate(1, n -> n*2).limit(6) → "
                + Stream.iterate(1, n -> n * 2).limit(6).collect(Collectors.toList()));
        System.out.println("三参数的 iterate（带停止条件，不用 limit）→ "
                + Stream.iterate(1, n -> n <= 5, n -> n + 1).collect(Collectors.toList()));

        // ⑨ 空流和拼接
        System.out.println("Stream.empty() 的 count = " + Stream.empty().count());
        System.out.println("Stream.concat  → "
                + Stream.concat(Stream.of(1, 2), Stream.of(3, 4)).collect(Collectors.toList()));
    }

    // ── 6. filter / distinct / limit / skip ────────────────────────

    static void showFilterFamily() {
        System.out.println("原始数据（8 个人）：" + people());

        List<Person> adults = people().stream()
                .filter(p -> p.age() > 18)
                .collect(Collectors.toList());
        System.out.println("filter(age > 18)          → " + adults.size() + " 个：" + adults);

        List<Person> unique = people().stream()
                .distinct()
                .collect(Collectors.toList());
        System.out.println("distinct()                → " + unique.size() + " 个："
                + unique + "（Cindy 重了，剩一个）");

        List<Person> firstThree = people().stream()
                .limit(3)
                .collect(Collectors.toList());
        System.out.println("limit(3)                  → " + firstThree);

        List<Person> skipTwo = people().stream()
                .skip(2)
                .limit(3)
                .collect(Collectors.toList());
        System.out.println("skip(2).limit(3)          → " + skipTwo);

        System.out.println("skip 超过长度不报错，只是空：" + people().stream().skip(100).count() + " 个");
    }

    // ── 7. map 与 flatMap ──────────────────────────────────────────

    static void showMapAndFlatMap() {
        List<Order> orders = List.of(
                new Order("A", List.of("键盘", "鼠标")),
                new Order("B", List.of("显示器")),
                new Order("C", List.of("鼠标", "耳机", "键盘")));

        // map：一对一。每个 Order 换成它的 items，得到的还是「每单一个元素」——
        // 只不过每个元素本身又是一个 List
        List<List<String>> nested = orders.stream()
                .map(Order::items)
                .collect(Collectors.toList());
        System.out.println("map(Order::items)     → " + nested);
        System.out.println("  还是 3 个元素，只是每个元素里套了一个 List");

        // flatMap：一对多，把里层的元素摊平到同一层
        List<String> flat = orders.stream()
                .flatMap(order -> order.items().stream())
                .collect(Collectors.toList());
        System.out.println("flatMap(items.stream) → " + flat);
        System.out.println("  " + flat.size() + " 个元素，全都摊平到一层了"
                + "（3 个订单：2 + 1 + 3）");

        System.out.println("摊平之后 distinct() 才有意义 → "
                + orders.stream().flatMap(o -> o.items().stream()).distinct().collect(Collectors.toList()));

        // flatMap 的入参不一定是集合，任何能变成流的东西都行
        System.out.println("拆字符串也是一样       → "
                + Stream.of("甲,乙", "丙,丁").flatMap(s -> Arrays.stream(s.split(",")))
                        .collect(Collectors.toList()));    }

    // ── 8. 数值流 ──────────────────────────────────────────────────

    static void showNumericStreams() {
        // Stream<Integer> 没有 sum()/average()，先转成 IntStream 才有
        int totalAge = people().stream().mapToInt(Person::age).sum();
        double averageAge = people().stream().mapToInt(Person::age).average().orElse(0);
        System.out.println("mapToInt(Person::age).sum()     = " + totalAge);
        System.out.println("mapToInt(Person::age).average() = " + averageAge);

        IntSummaryStatistics stats = people().stream().mapToInt(Person::age).summaryStatistics();
        System.out.println("summaryStatistics()             = " + stats);

        // 转回对象流用 boxed()
        List<Integer> boxed = people().stream().mapToInt(Person::age).limit(3).boxed()
                .collect(Collectors.toList());
        System.out.println("数值流 .boxed() 转回对象流      = " + boxed);

        // 为什么非要转数值流：boxed 的 Integer 流根本没有 sum()
        int[] ages = people().stream().mapToInt(Person::age).toArray();
        System.out.println("mapToInt(..).toArray()          = " + Arrays.toString(ages));

        System.out.println("IntStream 自己也能直接造         = "
                + IntStream.rangeClosed(1, 5).map(n -> n * n).boxed().collect(Collectors.toList()));
    }

    // ── 9. sorted ──────────────────────────────────────────────────

    static void showSorting() {
        // 自然序：Person 没有实现 Comparable，所以这里排的是 String
        System.out.println("sorted() 自然序        → "
                + people().stream().map(Person::name).distinct().sorted().collect(Collectors.toList()));

        // 给一个 Comparator 才能排 Person
        List<Person> byAge = people().stream()
                .distinct()
                .sorted(Comparator.comparingInt(Person::age))
                .collect(Collectors.toList());
        System.out.println("sorted(comparingInt(age)) → " + byAge);
        System.out.println("  年龄相同的 Alice/Grace 保持原来的先后——sorted 是稳定排序");

        // reversed() 是「再包一层比较器」，不改原来的那个
        List<Person> byAgeDesc = people().stream()
                .distinct()
                .sorted(Comparator.comparingInt(Person::age).reversed())
                .collect(Collectors.toList());
        System.out.println("reversed()             → " + byAgeDesc);

        // 多级比较：年龄一样就按名字
        List<Person> byAgeThenName = people().stream()
                .distinct()
                .sorted(Comparator.comparingInt(Person::age).thenComparing(Person::name))
                .collect(Collectors.toList());
        System.out.println("thenComparing(name)    → " + byAgeThenName);

        // 无参 sorted() 排 Person：编译过得去，要到运行期才炸
        try {
            List<Person> boom = people().stream().sorted().collect(Collectors.toList());
            System.out.println("无参 sorted() 居然过了：" + boom);
        } catch (ClassCastException e) {
            System.out.println();
            System.out.println("但无参 sorted() 排 Person 会炸——而且要到运行期才知道：");
            // 完整 message 末尾还带 module / loader 信息，每次不同，这里只留稳定部分
            System.out.println("  ClassCastException: class Person cannot be cast to class java.lang.Comparable");
            System.out.println("  （完整 message 还会带 module / loader 信息，每次都不同）");
        }
        // 更阴的是：只有一个元素时它不炸，因为没有需要比较的对手
        List<Person> single = people().stream().limit(1).sorted().collect(Collectors.toList());
        System.out.println("只有一个元素时却不炸：" + single + "   ← 数据一多就炸，测试只放一条就发现不了");
    }

    // ── 10. 综合链 ─────────────────────────────────────────────────

    static void showChainedPipeline() {
        List<String> chain = people().stream()
                .filter(p -> p.age() >= 25)                                  // 25 岁及以上
                .distinct()                                                  // 去掉重复的人
                .sorted(Comparator.comparingInt(Person::age).reversed())     // 年龄从大到小
                .map(Person::name)                                           // 只要名字
                .limit(3)                                                    // 只要前 3 个
                .collect(Collectors.toList());
        System.out.println("结果：" + chain);
        System.out.println("五步读下来就是一句话：25 岁以上的不重复的人，按年龄从大到小取前三个");

        // 同样的需求，for 循环版：得自己管「去重」和「排序」
        System.out.println();
        System.out.println("for 循环写同一件事要这么长：");
        System.out.println("    List<Person> adults = new ArrayList<>();");
        System.out.println("    for (Person p : people) if (p.age() >= 25) adults.add(p);");
        System.out.println("    Set<Person> seen = new LinkedHashSet<>(adults);      // 还得自己想怎么去重");
        System.out.println("    adults = new ArrayList<>(seen);");
        System.out.println("    adults.sort(Comparator.comparingInt(Person::age).reversed());  // 再想怎么排");
        System.out.println("    List<String> names = new ArrayList<>();");
        System.out.println("    for (int i = 0; i < 3 && i < adults.size(); i++) names.add(adults.get(i).name());");
        System.out.println("意图散在 6 个语句里，改一个需求要在 6 个地方找");
    }

    // ── 11. 短路 ───────────────────────────────────────────────────

    static void showShortCircuit() {
        boolean any = people().stream()
                .peek(p -> System.out.println("    anyMatch 检查到：" + p.name()))
                .anyMatch(p -> p.age() > 18);
        System.out.println("anyMatch(age > 18)   = " + any + "   ← 第一个人就满足，后面 7 个没被检查");

        boolean all = people().stream()
                .peek(p -> System.out.println("    allMatch 检查到：" + p.name()))
                .allMatch(p -> p.age() > 18);
        System.out.println("allMatch(age > 18)   = " + all + "  ← 到 Bob 就失败了，后面 6 个没被检查");

        boolean none = people().stream()
                .peek(p -> System.out.println("    noneMatch 检查到：" + p.name()))
                .noneMatch(p -> p.age() > 100);
        System.out.println("noneMatch(age > 100) = " + none + "  ← 没有一个能提前判定，8 个全查了");

        // 同一个 noneMatch，换个条件就能短路——短不短路取决于数据，不取决于操作
        int[] checked = {0};
        boolean noneShort = people().stream()
                .peek(p -> checked[0]++)
                .noneMatch(p -> p.age() > 30);
        System.out.println("noneMatch(age > 30)  = " + noneShort + "  ← 到第 " + checked[0]
                + " 个 Eve 就满足，后面 3 个不看：同一操作也能短路");

        // findFirst / findAny 返回的是 Optional，Optional 那一课再细说
        String firstMinor = people().stream()
                .filter(p -> p.age() < 18)
                .findFirst()
                .map(Person::name)
                .orElse("没有");
        System.out.println("findFirst(age < 18)  = " + firstMinor);

        String anyMinor = people().stream()
                .filter(p -> p.age() < 18)
                .findAny()
                .map(Person::name)
                .orElse("没有");
        System.out.println("findAny(age < 18)    = " + anyMinor + "   ← 顺序流下它也是第一个，别依赖这个");

        String missing = people().stream()
                .filter(p -> p.age() > 100)
                .findFirst()
                .map(Person::name)
                .orElse("没有");
        System.out.println("找不到的时候         = " + missing + "（Optional 空，不会抛异常）");
    }

    // ── 12. 聚合 ───────────────────────────────────────────────────

    static void showAggregates() {
        long count = people().stream().filter(p -> p.age() > 18).count();
        System.out.println("count()   → " + count);

        Person oldest = people().stream().max(Comparator.comparingInt(Person::age)).orElseThrow();
        System.out.println("max(age)  → " + oldest);

        Person youngest = people().stream().min(Comparator.comparingInt(Person::age)).orElseThrow();
        System.out.println("min(age)  → " + youngest + "   ← 17 岁有两个，返回先遇到的那个");

        System.out.println("sum()     → " + people().stream().mapToInt(Person::age).sum());
        System.out.println("average() → " + people().stream().mapToInt(Person::age).average().orElse(0));

        System.out.println("空流的 average 是空 Optional，不是 0："
                + Stream.<Person>empty().mapToInt(Person::age).average());
    }

    // ── 13. reduce ─────────────────────────────────────────────────

    static void showReduce() {
        // 单参数版：结果也是 Optional，因为空流没有「第一个元素」
        int maxAge = people().stream().map(Person::age).reduce(Integer::max).orElseThrow();
        System.out.println("reduce(Integer::max)     → " + maxAge);

        // 双参数版：给一个起点，结果不再是 Optional
        int totalAge = people().stream().map(Person::age).reduce(0, Integer::sum);
        System.out.println("reduce(0, Integer::sum)  → " + totalAge);

        // 三参数版：多一个 combiner。顺序流下 combiner 根本不会被调用
        int[] combinerCalls = {0};
        String joined = people().stream().map(Person::name)
                .reduce("",
                        (acc, name) -> acc + name + " ",
                        (a, b) -> {
                            combinerCalls[0]++;
                            return a + b;
                        });
        System.out.println("reduce(起点, 累加, 合并) → \"" + joined.trim() + "\"");
        System.out.println("  顺序流下 combiner 被调用 " + combinerCalls[0] + " 次——它是给并行流分块合并用的");
    }

    // ── 14. collect ────────────────────────────────────────────────

    static void showCollect() {
        List<String> list = people().stream().map(Person::name).collect(Collectors.toList());
        System.out.println("toList()        → " + list);

        // Set 的打印顺序不保证，排序后再打印，输出才稳定
        TreeSet<String> countries = new TreeSet<>(
                people().stream().map(Person::country).collect(Collectors.toSet()));
        System.out.println("toSet()         → " + countries + "   ← 打印前排过序，HashSet 本身不保证顺序");

        // toMap：key 重复会直接抛异常
        try {
            people().stream().collect(Collectors.toMap(Person::name, Person::age));
        } catch (IllegalStateException e) {
            System.out.println("toMap 遇到重名   → " + e.getMessage());
        }
        Map<String, Integer> merged = people().stream()
                .collect(Collectors.toMap(Person::name, Person::age, (a, b) -> a));   // 给个合并函数
        System.out.println("给合并函数之后  → " + new TreeMap<>(merged));

        // groupingBy：分组。HashMap 的顺序没有承诺，所以打印前换成 TreeMap
        Map<String, List<String>> byCountry = people().stream()
                .collect(Collectors.groupingBy(Person::country,
                        Collectors.mapping(Person::name, Collectors.toList())));
        System.out.println("groupingBy(country) → " + new TreeMap<>(byCountry));
        System.out.println("groupingBy + counting → "
                + new TreeMap<>(people().stream()
                        .collect(Collectors.groupingBy(Person::country, Collectors.counting()))));

        // partitioningBy：只分两组，key 一定是 true / false
        Map<Boolean, List<String>> parts = people().stream()
                .collect(Collectors.partitioningBy(p -> p.age() > 18,
                        Collectors.mapping(Person::name, Collectors.toList())));
        System.out.println("partitioningBy(age>18) → true  " + parts.get(true));
        System.out.println("                         false " + parts.get(false));

        System.out.println("joining()       → "
                + people().stream().map(Person::name).collect(Collectors.joining()));
        System.out.println("joining(带分隔和括号) → "
                + people().stream().map(Person::name).collect(Collectors.joining(", ", "[", "]")));

        IntSummaryStatistics stats = people().stream()
                .collect(Collectors.summarizingInt(Person::age));
        System.out.println("summarizingInt  → " + stats);
    }

    // ── 15. forEach ────────────────────────────────────────────────

    static void showForEach() {
        System.out.println("forEach：");
        people().stream().limit(3).forEach(p -> System.out.println("    " + p.name() + " " + p.age()));

        System.out.println("forEachOrdered（顺序流下看不出区别，并行流下才保证顺序）：");
        people().stream().limit(3).forEachOrdered(p -> System.out.println("    " + p.name() + " " + p.age()));

        // 反面：在流里改源集合。真实抛出的异常和很多人以为的不一样，见下一节。
        List<Person> mutable = new ArrayList<>(people());
        int[] callback = {0};
        try {
            mutable.stream().forEach(p -> {
                System.out.println("    第 " + callback[0]++ + " 次回调，元素 = " + p
                        + "，当前 size = " + mutable.size());
                if (p.age() < 18) {
                    mutable.remove(p);            // 边遍历边改，源集合被踩了
                }
            });
            System.out.println("边遍历边删居然没炸？");
        } catch (ConcurrentModificationException e) {
            System.out.println("边遍历边删 → ConcurrentModificationException");
        } catch (NullPointerException e) {
            // 真的跑出来是这个：ArrayList 的 forEachRemaining 把 size 提前缓存了，
            // 删元素后腾出来的位置是 null，取出来调 age() 就炸了。
            // 具体原因见 docs/10-Stream.md，这是个「以为会抛 A、结果抛 B」的真实例子。
            System.out.println("边遍历边删 → NullPointerException（不是 ConcurrentModificationException！）");
        }

        // 同样在流里改源集合，改成「加元素」就会抛大家熟悉的那个
        List<Person> growing = new ArrayList<>(people());
        try {
            growing.stream().forEach(p -> {
                if (p.age() > 30) {
                    growing.add(new Person("新人", 1, "中国"));
                }
            });
            System.out.println("边遍历边加居然没炸？");
        } catch (ConcurrentModificationException e) {
            System.out.println("边遍历边加 → ConcurrentModificationException");
        } catch (NullPointerException e) {
            System.out.println("边遍历边加 → NullPointerException");
        }
        System.out.println("所以别去猜「会抛哪个异常」——只要在流里改了源集合，行为就不可依赖");
    }

    // ── 16. 自己写一个 Collector ───────────────────────────────────

    static void showCustomCollector() {
        int[] combinerCalls = {0};
        Collector<Person, StringBuilder, String> nameCollector = Collector.of(
                StringBuilder::new,                                        // supplier：造一个空容器
                (sb, p) -> sb.append(p.name()).append(' '),                // accumulator：把一个元素塞进去
                (a, b) -> {                                                // combiner：两个容器合并
                    combinerCalls[0]++;
                    return a.append(b);
                },
                sb -> sb.toString().trim());                               // finisher：容器变成最终结果
        System.out.println("自己写的 Collector → " + people().stream().collect(nameCollector));
        System.out.println("  顺序流下 combiner 被调用 " + combinerCalls[0] + " 次——它只在并行流里才需要");
        System.out.println("Collectors.toList() 就是 supplier=ArrayList、accumulator=add、"
                + "combiner=addAll 的同一个套路");
    }

    // ── 17. 流复用 ─────────────────────────────────────────────────

    static void showStreamReuse() {
        Stream<Person> oneShot = people().stream().filter(p -> p.age() > 18);
        System.out.println("第一次 count() = " + oneShot.count());
        try {
            System.out.println("第二次 count() = " + oneShot.count());
        } catch (IllegalStateException e) {
            System.out.println("第二次用同一个流 → IllegalStateException: " + e.getMessage());
        }

        // 源流和中间流是同一条管道：接上下游的那一刻，上游就废了（不用等它被使用）
        Stream<Person> source = people().stream();
        Stream<Person> filtered = source.filter(p -> p.age() > 30);
        System.out.println();
        System.out.println("filtered.count() = " + filtered.count());
        try {
            System.out.println("回头再用 source.count() = " + source.count());
        } catch (IllegalStateException e) {
            System.out.println("回头用 source → IllegalStateException: " + e.getMessage());
        }
        System.out.println("所以要复用就复用「数据源」people()，每次都重新 .stream()");
    }

    // ── 18. 副作用 ─────────────────────────────────────────────────

    static void showSideEffects() {
        // 想用外部变量累加？lambda 里的局部变量必须是 effectively final，编译就过不去。
        // 真实 javac 报错见 broken/LambdaCapture.java。
        int[] box = {0};                                  // 数组的元素不是变量，所以能改
        people().stream().limit(3).forEach(p -> box[0] += p.age());
        System.out.println("用数组当累加器能过编译 → " + box[0] + "（但这是绕过限制，不推荐）");

        // 更好的写法：别用副作用累加，让流自己算
        int sum = people().stream().limit(3).mapToInt(Person::age).sum();
        System.out.println("同样的结果用 mapToInt(..).sum() → " + sum + "，没有副作用");

        // 真正危险的副作用是改源集合，见上一节：抛的异常还不可预测
        System.out.println("副作用的三条线：改源集合会炸；用外部变量累加编译不过；"
                + "并行流里做副作用结果不可预期（下一课）");
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("========== " + title + " ==========");
    }
}
