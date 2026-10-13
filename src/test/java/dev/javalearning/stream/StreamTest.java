package dev.javalearning.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 第 10 课的断言。
 *
 * <p>每一条对应 {@code docs/10-Stream.md} 里的一节。
 * 跑法：{@code ./gradlew test --tests '*stream*'}
 *
 * <p>数据源统一用 {@link StreamDemo#people()}，和 demo 里是同一份。
 */
class StreamTest {

    // ── 1~2. 笨办法 vs Stream ──────────────────────────────────────

    @Test
    @DisplayName("for 循环版和 Stream 版给出完全相同的结果")
    void loopAndStreamAgree() {
        List<Person> byLoop = StreamDemo.adultsByLoop(StreamDemo.people());
        List<Person> byStream = StreamDemo.people().stream()
                .filter(p -> p.age() > 18)
                .collect(Collectors.toList());
        assertThat(byStream).isEqualTo(byLoop);
        assertThat(byStream).hasSize(6);
    }

    @Test
    @DisplayName("Stream 版不改动原集合，原数据源一个不少")
    void streamDoesNotTouchTheSource() {
        List<Person> source = StreamDemo.people();
        source.stream().filter(p -> p.age() > 18).collect(Collectors.toList());
        assertThat(source).hasSize(8);   // 流是只读的，不会动源
    }

    // ── 3~4. 三个步骤与惰性求值 ─────────────────────────────────────

    @Test
    @DisplayName("只有中间操作、没有终端操作时，流里的代码一行都不执行")
    void intermediateOperationsAreLazy() {
        AtomicInteger calls = new AtomicInteger();
        // 建一条链，但不加终端操作
        Stream<String> lazy = StreamDemo.people().stream()
                .filter(p -> {
                    calls.incrementAndGet();
                    return p.age() > 18;
                })
                .map(Person::name);
        assertThat(calls).hasValue(0);   // 关键：一个都没跑

        // 补上终端操作，这时才真的开始跑
        List<String> names = lazy.collect(Collectors.toList());
        assertThat(calls.get()).isEqualTo(8);   // 8 个人各过滤一次
        assertThat(names).hasSize(6);
    }

    @Test
    @DisplayName("limit 是短路的中间操作：它下游只处理前 n 个")
    void limitStopsTheUpstreamEarly() {
        AtomicInteger calls = new AtomicInteger();
        List<String> firstTwo = StreamDemo.people().stream()
                .peek(p -> calls.incrementAndGet())
                .limit(2)
                .map(Person::name)
                .collect(Collectors.toList());

        assertThat(firstTwo).containsExactly("Alice", "Bob");
        assertThat(calls.get()).isEqualTo(2);   // 只流过了 2 个，不是 8 个
    }

    // ── 5. 创建流 ──────────────────────────────────────────────────

    @Test
    @DisplayName("集合、数组、Stream.of、builder 都能造出流")
    void severalWaysToCreateAStream() {
        assertThat(StreamDemo.people().stream()).hasSize(8);
        assertThat(Arrays.stream(new int[] {3, 1, 4, 1, 5})).hasSize(5);
        assertThat(Stream.of("甲", "乙", "丙")).containsExactly("甲", "乙", "丙");
        assertThat(Stream.<String>builder().add("甲").add("乙").build())
                .containsExactly("甲", "乙");
        assertThat(Stream.empty()).isEmpty();
        assertThat(Stream.concat(Stream.of(1, 2), Stream.of(3, 4)))
                .containsExactly(1, 2, 3, 4);
    }

    @Test
    @DisplayName("Files.lines 一行一个元素，用 try-with-resources 关掉句柄")
    void filesLinesReadsOneElementPerLine() throws IOException {
        Path temp = Files.createTempFile("stream-test", ".txt");
        Files.writeString(temp, "苹果\n香蕉\n樱桃\n", StandardCharsets.UTF_8);
        try (Stream<String> lines = Files.lines(temp, StandardCharsets.UTF_8)) {
            assertThat(lines).containsExactly("苹果", "香蕉", "樱桃");
        } finally {
            Files.delete(temp);
        }
    }

    @Test
    @DisplayName("range 不含右端，rangeClosed 含——差一错误的高发区")
    void rangeExcludesTheUpperBound() {
        assertThat(IntStream.range(1, 5).boxed()).containsExactly(1, 2, 3, 4);
        assertThat(IntStream.rangeClosed(1, 5).boxed()).containsExactly(1, 2, 3, 4, 5);
        assertThat(IntStream.rangeClosed(1, 100).sum()).isEqualTo(5050);
    }

    @Test
    @DisplayName("无限流必须配 limit，否则永远不结束")
    void infiniteStreamsNeedALimit() {
        assertThat(Stream.generate(() -> "重复").limit(3))
                .containsExactly("重复", "重复", "重复");
        assertThat(Stream.iterate(1, n -> n * 2).limit(6))
                .containsExactly(1, 2, 4, 8, 16, 32);
        // 三参数的 iterate 自带停止条件，不用 limit
        assertThat(Stream.iterate(1, n -> n <= 5, n -> n + 1))
                .containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    @DisplayName("随机数流写死种子才可复现，不写种子每次都不一样")
    void seededRandomStreamIsReproducible() {
        List<Integer> first = new Random(42).ints(5, 1, 100).boxed().collect(Collectors.toList());
        List<Integer> again = new Random(42).ints(5, 1, 100).boxed().collect(Collectors.toList());
        assertThat(first).isEqualTo(again).hasSize(5);
        // 只断言范围，不断言具体值——值依赖 JDK 实现
        assertThat(first).allMatch(n -> n >= 1 && n < 100);
    }

    // ── 6. filter / distinct / limit / skip ────────────────────────

    @Test
    @DisplayName("filter 只留下满足条件的元素")
    void filterKeepsMatchingElements() {
        assertThat(StreamDemo.people().stream().filter(p -> p.age() > 18))
                .hasSize(6)
                .allMatch(p -> p.age() > 18);
    }

    @Test
    @DisplayName("distinct 靠 equals/hashCode 去重，内容相同的 Cindy 只剩一个")
    void distinctUsesEqualsAndHashCode() {
        assertThat(StreamDemo.people().stream().distinct()).hasSize(7);
        // 这正说明 Person 重写了 equals/hashCode：
        // 没重写的话两个 Cindy 是两个不同对象，distinct 会留下 8 个
        assertThat(StreamDemo.people().stream().distinct()
                .filter(p -> p.name().equals("Cindy")))
                .hasSize(1);
    }

    @Test
    @DisplayName("limit 取前 n 个，skip 跳过前 n 个，两者可组合")
    void limitAndSkipSliceTheStream() {
        assertThat(StreamDemo.people().stream().limit(3).map(Person::name))
                .containsExactly("Alice", "Bob", "Cindy");
        assertThat(StreamDemo.people().stream().skip(2).limit(3).map(Person::name))
                .containsExactly("Cindy", "David", "Eve");
    }

    @Test
    @DisplayName("skip 超过长度不报错，只是得到空流")
    void skipBeyondTheEndIsFine() {
        assertThat(StreamDemo.people().stream().skip(100)).isEmpty();
    }

    // ── 7. map 与 flatMap ──────────────────────────────────────────

    @Test
    @DisplayName("map 是一对一：元素个数不变，只是每个元素被换掉了")
    void mapIsOneToOne() {
        List<String> names = StreamDemo.people().stream()
                .map(Person::name)
                .collect(Collectors.toList());
        assertThat(names).hasSize(8);
        assertThat(names).containsExactly("Alice", "Bob", "Cindy", "David", "Eve", "Frank", "Grace", "Cindy");
    }

    @Test
    @DisplayName("map 换成 List 字段会得到「流里套 List」，层数不变")
    void mapCanProduceNestedStructures() {
        List<Order> orders = sampleOrders();
        List<List<String>> nested = orders.stream().map(Order::items).collect(Collectors.toList());
        assertThat(nested).hasSize(3);                       // 还是 3 个元素
        assertThat(nested.get(0)).containsExactly("键盘", "鼠标");   // 只是每个里面套了一个 List
    }

    @Test
    @DisplayName("flatMap 把里层摊平：3 个订单变成 6 个商品")
    void flatMapFlattensNestedStructures() {
        List<String> flat = sampleOrders().stream()
                .flatMap(o -> o.items().stream())
                .collect(Collectors.toList());
        assertThat(flat).hasSize(6);   // 2 + 1 + 3
        assertThat(flat).containsExactly("键盘", "鼠标", "显示器", "鼠标", "耳机", "键盘");
    }

    @Test
    @DisplayName("摊平之后 distinct 才能对里层元素去重")
    void flatMapEnablesDistinctOnInnerElements() {
        assertThat(sampleOrders().stream().flatMap(o -> o.items().stream()).distinct())
                .containsExactlyInAnyOrder("键盘", "鼠标", "显示器", "耳机");
    }

    @Test
    @DisplayName("flatMap 的入参只要是「能变成流的东西」就行，比如拆字符串")
    void flatMapAlsoWorksOnNonCollections() {
        assertThat(Stream.of("甲,乙", "丙,丁").flatMap(s -> Arrays.stream(s.split(","))))
                .containsExactly("甲", "乙", "丙", "丁");
    }

    private static List<Order> sampleOrders() {
        return List.of(
                new Order("A", List.of("键盘", "鼠标")),
                new Order("B", List.of("显示器")),
                new Order("C", List.of("鼠标", "耳机", "键盘")));
    }

    // ── 8. 数值流 ──────────────────────────────────────────────────

    @Test
    @DisplayName("mapToInt 之后才有 sum/average/summaryStatistics")
    void numericStreamsGiveSumAndAverage() {
        assertThat(StreamDemo.people().stream().mapToInt(Person::age).sum()).isEqualTo(204);
        assertThat(StreamDemo.people().stream().mapToInt(Person::age).average())
                .hasValue(25.5);

        IntSummaryStatistics stats = StreamDemo.people().stream()
                .mapToInt(Person::age).summaryStatistics();
        assertThat(stats.getCount()).isEqualTo(8);
        assertThat(stats.getMin()).isEqualTo(17);
        assertThat(stats.getMax()).isEqualTo(35);
        assertThat(stats.getSum()).isEqualTo(204);
    }

    @Test
    @DisplayName("boxed 把数值流转回对象流，toArray 直接拿基本类型数组")
    void numericStreamsConvertBack() {
        assertThat(StreamDemo.people().stream().mapToInt(Person::age).limit(3).boxed())
                .containsExactly(30, 17, 25);
        assertThat(StreamDemo.people().stream().mapToInt(Person::age).toArray())
                .containsExactly(30, 17, 25, 17, 35, 25, 30, 25);
    }

    @Test
    @DisplayName("空流求 average 是空 Optional，不是 0——别把「没有」当成「零」")
    void averageOfEmptyStreamIsEmpty() {
        assertThat(Stream.<Person>empty().mapToInt(Person::age).average()).isEmpty();
        assertThat(Stream.<Person>empty().mapToInt(Person::age).sum()).isZero();   // sum 才是 0
    }

    // ── 9. sorted ──────────────────────────────────────────────────

    @Test
    @DisplayName("sorted() 用自然序，元素必须 Comparable")
    void sortedUsesNaturalOrder() {
        assertThat(Stream.of("banana", "apple", "cherry").sorted())
                .containsExactly("apple", "banana", "cherry");
        // Person 没实现 Comparable，所以排 Person 必须给 Comparator（见下一条）
    }

    @Test
    @DisplayName("无参 sorted() 排不可比较的对象：编译过得去，运行期才炸 ClassCastException")
    void sortedOnNonComparableFailsAtRuntimeNotCompileTime() {
        // 注意这里能编过——sorted() 的签名不约束元素类型
        assertThatThrownBy(() -> StreamDemo.people().stream().sorted().collect(Collectors.toList()))
                .isInstanceOf(ClassCastException.class)
                .hasMessageContaining("Comparable");
    }

    @Test
    @DisplayName("只有一个元素时 sorted() 不炸——没有对手就不用比较，测试只放一条会漏掉这个 bug")
    void sortedOnSingleElementNeverCompares() {
        assertThat(StreamDemo.people().stream().limit(1).sorted().collect(Collectors.toList()))
                .hasSize(1);
    }

    @Test
    @DisplayName("sorted(comparator) 才是排自定义对象的正确姿势")
    void sortedWithAComparatorSortsObjects() {
        List<Person> byAge = StreamDemo.people().stream()
                .distinct()
                .sorted(Comparator.comparingInt(Person::age))
                .collect(Collectors.toList());
        assertThat(byAge).extracting(Person::age).isSorted();
    }

    @Test
    @DisplayName("sorted 是稳定排序：年龄相同的保持原顺序")
    void sortedIsStable() {
        List<Person> byAge = StreamDemo.people().stream()
                .distinct()
                .sorted(Comparator.comparingInt(Person::age))
                .collect(Collectors.toList());
        // Alice(30) 原来在 Grace(30) 前面，排完还在前面
        assertThat(byAge).extracting(Person::name)
                .containsSubsequence("Alice", "Grace");
    }

    @Test
    @DisplayName("reversed() 是包一层新比较器，thenComparing 做多级比较")
    void reversedAndThenComparing() {
        List<String> desc = StreamDemo.people().stream().distinct()
                .sorted(Comparator.comparingInt(Person::age).reversed())
                .map(Person::name)
                .collect(Collectors.toList());
        assertThat(desc.get(0)).isEqualTo("Eve");   // 35 岁最大

        // 年龄一样时按名字
        List<Person> byAgeThenName = StreamDemo.people().stream().distinct()
                .sorted(Comparator.comparingInt(Person::age).thenComparing(Person::name))
                .collect(Collectors.toList());
        assertThat(byAgeThenName.get(0).name()).isEqualTo("Bob");    // 17 岁里 Bob 在 David 前
        assertThat(byAgeThenName.get(1).name()).isEqualTo("David");
    }

    // ── 10. 综合链 ─────────────────────────────────────────────────

    @Test
    @DisplayName("filter + distinct + sorted + map + limit 串起来是一条可读的流水线")
    void chainedPipelineReadsLikeASentence() {
        List<String> chain = StreamDemo.people().stream()
                .filter(p -> p.age() >= 25)
                .distinct()
                .sorted(Comparator.comparingInt(Person::age).reversed())
                .map(Person::name)
                .limit(3)
                .collect(Collectors.toList());
        assertThat(chain).containsExactly("Eve", "Alice", "Grace");
    }

    @Test
    @DisplayName("中间操作的顺序会影响结果：先 limit 再 sorted，和反过来完全不同")
    void orderOfIntermediateOperationsMatters() {
        // 先截前两个（Alice 30、Bob 17），再按年龄从大到小 → Alice, Bob
        List<String> limitFirst = StreamDemo.people().stream()
                .limit(2)
                .sorted(Comparator.comparingInt(Person::age).reversed())
                .map(Person::name)
                .collect(Collectors.toList());
        // 先全局按年龄排，再取前两个 → Eve(35), Alice(30)
        List<String> sortFirst = StreamDemo.people().stream()
                .sorted(Comparator.comparingInt(Person::age).reversed())
                .limit(2)
                .map(Person::name)
                .collect(Collectors.toList());

        assertThat(limitFirst).containsExactly("Alice", "Bob");
        assertThat(sortFirst).containsExactly("Eve", "Alice");
        assertThat(limitFirst).isNotEqualTo(sortFirst);
    }

    // ── 11. 短路 ───────────────────────────────────────────────────

    @Test
    @DisplayName("anyMatch 找到一个就停，不会把整条流走完")
    void anyMatchShortCircuits() {
        AtomicInteger checked = new AtomicInteger();
        boolean any = StreamDemo.people().stream()
                .peek(p -> checked.incrementAndGet())
                .anyMatch(p -> p.age() > 18);
        assertThat(any).isTrue();
        assertThat(checked.get()).isEqualTo(1);   // 第一个人就满足，后面 7 个没检查
    }

    @Test
    @DisplayName("allMatch 遇到一个不满足就停")
    void allMatchShortCircuits() {
        AtomicInteger checked = new AtomicInteger();
        boolean all = StreamDemo.people().stream()
                .peek(p -> checked.incrementAndGet())
                .allMatch(p -> p.age() > 18);
        assertThat(all).isFalse();
        assertThat(checked.get()).isEqualTo(2);   // Alice 过、Bob 挂，到此为止
    }

    @Test
    @DisplayName("noneMatch 无法提前判定时，只能把整条流走完")
    void noneMatchHasNothingToShortCircuitOn() {
        AtomicInteger checked = new AtomicInteger();
        boolean none = StreamDemo.people().stream()
                .peek(p -> checked.incrementAndGet())
                .noneMatch(p -> p.age() > 100);
        assertThat(none).isTrue();
        assertThat(checked.get()).isEqualTo(8);   // 全都得看一遍才知道「一个都没有」
    }

    @Test
    @DisplayName("同一个 noneMatch，找得到时就短路——短不短路取决于数据")
    void noneMatchShortCircuitsWhenItCan() {
        AtomicInteger checked = new AtomicInteger();
        boolean none = StreamDemo.people().stream()
                .peek(p -> checked.incrementAndGet())
                .noneMatch(p -> p.age() > 30);
        assertThat(none).isFalse();
        assertThat(checked.get()).isEqualTo(5);   // 到第 5 个 Eve 就满足，后面 3 个不看
    }

    @Test
    @DisplayName("findFirst 拿第一个，findAny 顺序流下也是第一个")
    void findFirstAndFindAny() {
        assertThat(StreamDemo.people().stream().filter(p -> p.age() < 18).findFirst())
                .map(Person::name).hasValue("Bob");
        // 顺序流下 findAny 返回的也是第一个；并行流下才可能不同（下一课）
        assertThat(StreamDemo.people().stream().filter(p -> p.age() < 18).findAny())
                .map(Person::name).hasValue("Bob");
    }

    @Test
    @DisplayName("找不到时返回空 Optional，不抛异常")
    void findFirstReturnsEmptyOptional() {
        assertThat(StreamDemo.people().stream().filter(p -> p.age() > 100).findFirst()).isEmpty();
    }

    // ── 12. 聚合 ───────────────────────────────────────────────────

    @Test
    @DisplayName("count / max / min 是终端操作，返回的不是流")
    void countMaxMin() {
        assertThat(StreamDemo.people().stream().filter(p -> p.age() > 18).count()).isEqualTo(6);
        assertThat(StreamDemo.people().stream().max(Comparator.comparingInt(Person::age)))
                .map(Person::name).hasValue("Eve");
        assertThat(StreamDemo.people().stream().min(Comparator.comparingInt(Person::age)))
                .map(Person::name).hasValue("Bob");
    }

    // ── 13. reduce ─────────────────────────────────────────────────

    @Test
    @DisplayName("reduce 单参数版返回 Optional，因为空流没有起点")
    void reduceWithoutIdentityReturnsOptional() {
        assertThat(StreamDemo.people().stream().map(Person::age).reduce(Integer::max))
                .hasValue(35);
        assertThat(Stream.<Integer>empty().reduce(Integer::sum)).isEmpty();
    }

    @Test
    @DisplayName("reduce 双参数版给了起点，空流也有结果")
    void reduceWithIdentityAlwaysHasAResult() {
        assertThat(StreamDemo.people().stream().map(Person::age).reduce(0, Integer::sum))
                .isEqualTo(204);
        assertThat(Stream.<Integer>empty().reduce(0, Integer::sum)).isZero();
    }

    @Test
    @DisplayName("reduce 三参数版的 combiner 在顺序流下根本不会被调用")
    void threeArgReduceCombinerIsUnusedOnSequentialStreams() {
        AtomicInteger combinerCalls = new AtomicInteger();
        String joined = StreamDemo.people().stream().map(Person::name)
                .reduce("",
                        (acc, name) -> acc + name + " ",
                        (a, b) -> {
                            combinerCalls.incrementAndGet();
                            return a + b;
                        });
        assertThat(joined.trim()).isEqualTo("Alice Bob Cindy David Eve Frank Grace Cindy");
        // combiner 是给并行流「分块再合并」用的，顺序流只有一块，用不上
        assertThat(combinerCalls).hasValue(0);
    }

    // ── 14. collect ────────────────────────────────────────────────

    @Test
    @DisplayName("toList / toSet / joining 各收各的")
    void collectToListToSetAndJoining() {
        assertThat(StreamDemo.people().stream().map(Person::name).collect(Collectors.toList()))
                .hasSize(8);
        Set<String> countries = StreamDemo.people().stream()
                .map(Person::country).collect(Collectors.toSet());
        assertThat(countries).containsExactlyInAnyOrder("中国", "美国", "英国");
        assertThat(StreamDemo.people().stream().map(Person::name).collect(Collectors.joining(", ")))
                .isEqualTo("Alice, Bob, Cindy, David, Eve, Frank, Grace, Cindy");
    }

    @Test
    @DisplayName("Collectors.toList 可变，Stream.toList 不可变——这是个真会踩的坑")
    void collectorsToListIsMutableButStreamToListIsNot() {
        List<String> mutable = Stream.of("x", "y").collect(Collectors.toList());
        mutable.add("z");                                  // 不抛异常
        assertThat(mutable).containsExactly("x", "y", "z");

        List<String> immutable = Stream.of("x", "y").toList();
        assertThatThrownBy(() -> immutable.add("z"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("toMap 遇到重复 key 直接抛异常，必须给合并函数")
    void toMapThrowsOnDuplicateKeys() {
        // 两个 Cindy，key 撞了
        assertThatThrownBy(() -> StreamDemo.people().stream()
                .collect(Collectors.toMap(Person::name, Person::age)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cindy");

        Map<String, Integer> merged = StreamDemo.people().stream()
                .collect(Collectors.toMap(Person::name, Person::age, (a, b) -> a));
        assertThat(merged).hasSize(7);        // 8 个人、7 个不同的名字
        assertThat(merged.get("Cindy")).isEqualTo(25);
    }

    @Test
    @DisplayName("groupingBy 按国家分组，counting 顺带数个数")
    void groupingByGroupsAndCounts() {
        Map<String, List<String>> byCountry = StreamDemo.people().stream()
                .collect(Collectors.groupingBy(Person::country,
                        Collectors.mapping(Person::name, Collectors.toList())));
        assertThat(byCountry.get("中国")).containsExactly("Cindy", "Frank", "Cindy");
        assertThat(byCountry.get("英国")).containsExactly("Bob", "David");
        assertThat(byCountry.get("美国")).containsExactly("Alice", "Eve", "Grace");

        Map<String, Long> counts = StreamDemo.people().stream()
                .collect(Collectors.groupingBy(Person::country, Collectors.counting()));
        assertThat(counts).containsEntry("中国", 3L)
                .containsEntry("美国", 3L)
                .containsEntry("英国", 2L);
    }

    @Test
    @DisplayName("partitioningBy 只分两组，key 一定是 true/false，而且两组都在")
    void partitioningByAlwaysHasBothKeys() {
        Map<Boolean, List<String>> parts = StreamDemo.people().stream()
                .collect(Collectors.partitioningBy(p -> p.age() > 18,
                        Collectors.mapping(Person::name, Collectors.toList())));
        assertThat(parts.get(true)).containsExactly("Alice", "Cindy", "Eve", "Frank", "Grace", "Cindy");
        assertThat(parts.get(false)).containsExactly("Bob", "David");
        assertThat(parts).containsOnlyKeys(true, false);   // 就算一组是空，key 也在
    }

    @Test
    @DisplayName("summarizingInt 一次拿到 count/sum/min/max/average")
    void summarizingIntGivesEverythingAtOnce() {
        IntSummaryStatistics stats = StreamDemo.people().stream()
                .collect(Collectors.summarizingInt(Person::age));
        assertThat(stats.getCount()).isEqualTo(8);
        assertThat(stats.getSum()).isEqualTo(204);
        assertThat(stats.getMin()).isEqualTo(17);
        assertThat(stats.getMax()).isEqualTo(35);
        assertThat(stats.getAverage()).isEqualTo(25.5);
    }

    @Test
    @DisplayName("groupingBy 的结果顺序不该依赖，要稳定输出就先换 TreeMap")
    void groupingByOrderIsNotGuaranteedSoSortIt() {
        Map<String, List<String>> raw = StreamDemo.people().stream()
                .collect(Collectors.groupingBy(Person::country,
                        Collectors.mapping(Person::name, Collectors.toList())));
        // HashMap 的迭代顺序没有承诺，但「按 key 排序后」是确定的
        assertThat(new TreeMap<>(raw).keySet()).containsExactly("中国", "美国", "英国");
        // 内容本身和顺序无关
        assertThat(raw).hasSize(3);
        assertThat(new TreeSet<>(raw.keySet())).containsExactly("中国", "美国", "英国");
    }

    // ── 15. forEach ────────────────────────────────────────────────

    @Test
    @DisplayName("forEach 按顺序流过的顺序执行")
    void forEachFollowsTheEncounterOrder() {
        List<String> seen = new ArrayList<>();
        StreamDemo.people().stream().limit(3).forEach(p -> seen.add(p.name()));
        assertThat(seen).containsExactly("Alice", "Bob", "Cindy");
    }

    // ── 16. 自定义 Collector ───────────────────────────────────────

    @Test
    @DisplayName("自己写的 Collector 也能用，四个组件各司其职")
    void customCollectorWorks() {
        AtomicInteger combinerCalls = new AtomicInteger();
        Collector<Person, StringBuilder, String> nameCollector = Collector.of(
                StringBuilder::new,
                (sb, p) -> sb.append(p.name()).append(' '),
                (a, b) -> {
                    combinerCalls.incrementAndGet();
                    return a.append(b);
                },
                sb -> sb.toString().trim());

        String result = StreamDemo.people().stream().collect(nameCollector);
        assertThat(result).isEqualTo("Alice Bob Cindy David Eve Frank Grace Cindy");
        assertThat(combinerCalls).hasValue(0);   // 顺序流下 combiner 用不上
    }

    // ── 17. 流复用 ─────────────────────────────────────────────────

    @Test
    @DisplayName("流用过一次就不能再用，抛 IllegalStateException")
    void streamCannotBeReused() {
        Stream<Person> oneShot = StreamDemo.people().stream().filter(p -> p.age() > 18);
        assertThat(oneShot.count()).isEqualTo(6);

        assertThatThrownBy(oneShot::count)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("stream has already been operated upon or closed");
    }

    @Test
    @DisplayName("源流在「被接上下游」那一刻就废了，不用等终端操作")
    void sourceIsConsumedAsSoonAsItIsLinked() {
        Stream<Person> source = StreamDemo.people().stream();
        Stream<Person> filtered = source.filter(p -> p.age() > 30);

        // 注意：这里还没执行任何终端操作，但 source 已经不能用了
        assertThatThrownBy(source::count).isInstanceOf(IllegalStateException.class);
        // 只有管道最末端那一节能用
        assertThat(filtered.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("链了两级之后，中间那一级也废了，只有末端能用")
    void onlyTheLastStageOfTheChainIsUsable() {
        Stream<Person> source = StreamDemo.people().stream();
        Stream<Person> middle = source.filter(p -> p.age() > 18);
        Stream<String> last = middle.map(Person::name);

        assertThatThrownBy(source::count).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(middle::count).isInstanceOf(IllegalStateException.class);
        assertThat(last).hasSize(6);
    }

    @Test
    @DisplayName("短路终端操作也会把流消费掉，之后同样不能再用")
    void shortCircuitTerminalAlsoConsumesTheStream() {
        Stream<Person> stream = StreamDemo.people().stream();
        assertThat(stream.findFirst()).isPresent();
        assertThatThrownBy(stream::count).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("要复用就复用数据源：每次重新 people().stream() 都是新流")
    void reuseTheSourceNotTheStream() {
        assertThat(StreamDemo.people().stream().count()).isEqualTo(8);
        assertThat(StreamDemo.people().stream().count()).isEqualTo(8);   // 再来一次，照样能用
    }

    // ── 18. 副作用 ─────────────────────────────────────────────────

    @Test
    @DisplayName("在流里改源集合会炸——但抛的是哪个异常并不值得依赖")
    void modifyingTheSourceInsideAStreamBreaks() {
        List<Person> mutable = new ArrayList<>(StreamDemo.people());

        // 删元素：ArrayList 的 spliterator 把 size 提前缓存了，
        // 删完之后腾出的位置是 null，取出来调 age() 就成了 NPE。
        // 注意：很多人以为这里会抛 ConcurrentModificationException，实际不是。
        assertThatThrownBy(() -> mutable.stream().forEach(p -> {
            if (p.age() < 18) {
                mutable.remove(p);
            }
        })).isInstanceOf(RuntimeException.class);

        // 加元素：这次才是 ConcurrentModificationException
        List<Person> growing = new ArrayList<>(StreamDemo.people());
        assertThatThrownBy(() -> growing.stream().forEach(p -> {
            if (p.age() > 30) {
                growing.add(new Person("新人", 1, "中国"));
            }
        })).isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    @DisplayName("想累加就用 reduce/sum，不要用外部变量——lambda 里也改不了它")
    void accumulateWithTheStreamNotWithSideEffects() {
        // 正确做法：让流自己算
        assertThat(StreamDemo.people().stream().limit(3).mapToInt(Person::age).sum()).isEqualTo(72);

        // 用数组当累加器能过编译，但这是绕过 effectively final 的限制，不推荐
        int[] box = {0};
        StreamDemo.people().stream().limit(3).forEach(p -> box[0] += p.age());
        assertThat(box[0]).isEqualTo(72);
    }
}
