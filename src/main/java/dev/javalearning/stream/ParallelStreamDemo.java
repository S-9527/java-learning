package dev.javalearning.stream;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * 第 11 课的示例代码：并行流 Parallel Streams。
 *
 * <p>直接跑：{@code java src/main/java/dev/javalearning/stream/ParallelStreamDemo.java}
 *
 * <p><b>注意</b>：这一课演示的正是「并行导致结果不确定」，
 * 所以有几节的输出天生不稳定。文档里会明确标出哪些是每次不同的，
 * 哪些是稳定结论。
 */
public class ParallelStreamDemo {

    public static void main(String[] args) throws Exception {
        section("1. 怎么创建并行流");
        howToCreate();

        section("2. 第一次撞墙：forEach 的输出是乱序的");
        forEachIsUnordered();

        section("3. forEachOrdered：保序，但牺牲并行效率");
        forEachOrderedKeepsOrder();

        section("4. 为什么 collect(toList) 的结果却是有序的");
        collectKeepsOrder();

        section("5. 自定义 Collector：看合并过程到底发生了什么");
        customCollector();

        section("6. 哪些操作并行后结果不变");
        consistentOperations();

        section("7. reduce 用非结合性操作会出错（经典坑）");
        reduceNeedsAssociativity();

        section("8. 并行流里操作共享集合会出问题");
        sharedMutableState();

        section("9. 用的是哪个线程池");
        whichPool();

        section("10. 并行流一定更快吗");
        notAlwaysFaster();
    }

    /** 两种创建方式。 */
    static void howToCreate() {
        List<String> letters = List.of("A", "L", "B", "E", "R", "T");

        long fromSequential = letters.stream().parallel().count();
        long fromParallel = letters.parallelStream().count();

        System.out.println("  letters.stream().parallel() 的元素数 → " + fromSequential);
        System.out.println("  letters.parallelStream()     的元素数 → " + fromParallel);
        System.out.println("  ← 一个是在顺序流上转，一个是集合直接给");
    }

    /**
     * 并行流的 forEach 不保证顺序。
     *
     * <p>这里为了输出稳定，把结果收集起来**排序后**打印，
     * 但同时打印「是否恰好等于原始顺序」——这个布尔值会告诉你它乱没乱。
     */
    static void forEachIsUnordered() {
        List<String> letters = List.of("A", "L", "B", "E", "R", "T");

        // forEach 的写入顺序不确定，所以收集时用同步列表
        List<String> seen = java.util.Collections.synchronizedList(new ArrayList<>());
        letters.parallelStream()
                .map(String::toLowerCase)
                .forEach(seen::add);

        System.out.println("  原始顺序          → " + letters.stream().map(String::toLowerCase).toList());
        System.out.println("  forEach 收到的集合（排序后打印）→ " + sortedCopy(seen));
        System.out.println("  ← 元素一个不少，但**到达顺序**和原始顺序无关");
        System.out.println("  ← 谁先跑完谁先 add，所以这里必须排序后再打印，否则输出会随机变化");
        System.out.println("  ← 想看乱序的原始样子：多跑几次这个 demo，输出顺序会变");
    }

    /** forEachOrdered 保序。 */
    static void forEachOrderedKeepsOrder() {
        List<String> letters = List.of("A", "L", "B", "E", "R", "T");

        List<String> seen = java.util.Collections.synchronizedList(new ArrayList<>());
        letters.parallelStream()
                .map(String::toLowerCase)
                .forEachOrdered(seen::add);

        System.out.println("  forEachOrdered 收到的顺序 → " + seen);
        System.out.println("  和原始顺序一致吗 → " + seen.equals(
                letters.stream().map(String::toLowerCase).collect(Collectors.toList())));
        System.out.println("  ← 它按「出现顺序」执行，代价是部分并行度被牺牲");
    }

    /** collect 的结果是有序的。 */
    static void collectKeepsOrder() {
        List<String> letters = List.of("A", "L", "B", "E", "R", "T");

        List<String> collected = letters.parallelStream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        System.out.println("  collect(toList()) 的结果 → " + collected);
        System.out.println("  ← 明明是多线程跑的，为什么还有序？下一节拆开看");

        // 换成 toSet 就无序了（因为 Set 本身无序）
        var asSet = letters.parallelStream()
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(java.util.TreeSet::new));
        System.out.println("  收集到 TreeSet（排过序）→ " + asSet);
        System.out.println("  ← 注意：无序是 Set 这个容器导致的，不是并行导致的");
    }

    /**
     * 自定义 Collector，把 supplier / accumulator / combiner 的调用次数打出来。
     *
     * <p>这是理解「并行流怎么合并结果」最直接的办法。
     * 具体次数取决于 CPU 核数和分片策略，所以这里只打印**结论性**的信息。
     */
    static void customCollector() {
        List<String> letters = List.of("A", "L", "B", "E", "R", "T");

        java.util.concurrent.atomic.AtomicInteger suppliers = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger combiners = new java.util.concurrent.atomic.AtomicInteger();
        List<String> merged = letters.parallelStream().collect(
                () -> {
                    suppliers.incrementAndGet();
                    return new ArrayList<String>();
                },
                (list, s) -> list.add(s.toLowerCase()),
                (left, right) -> {
                    combiners.incrementAndGet();
                    left.addAll(right);
                });

        System.out.println("  结果 → " + merged);
        // supplier / combiner 的具体次数取决于 CPU 核数和分片策略，
        // 每次运行都可能不同，所以只判断「是否发生了多次 / 是否发生合并」
        System.out.println("  supplier 被调用了不止一次吗 → " + (suppliers.get() > 1));
        System.out.println("  combiner 被调用过吗 → " + (combiners.get() > 0));
        System.out.println("  ← 每个分片各有一个局部容器，最后按顺序合并起来");
        System.out.println("  ← 分几片取决于 CPU 核数，所以这里不打印具体次数");

        // 顺序流对比：supplier 只调一次，combiner 一次都不调
        java.util.concurrent.atomic.AtomicInteger seqSuppliers = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger seqCombiners = new java.util.concurrent.atomic.AtomicInteger();
        List<String> seqResult = letters.stream().collect(
                () -> {
                    seqSuppliers.incrementAndGet();
                    return new ArrayList<String>();
                },
                (list, s) -> list.add(s.toLowerCase()),
                (left, right) -> {
                    seqCombiners.incrementAndGet();
                    left.addAll(right);
                });

        System.out.println("  换成顺序流：supplier " + seqSuppliers.get()
                + " 次，combiner " + seqCombiners.get() + " 次");
        System.out.println("  ← 顺序流只用一个容器，压根不需要合并");
    }

    /** 这些操作并行后结果和顺序流一致。 */
    static void consistentOperations() {
        List<Integer> numbers = IntStream.rangeClosed(1, 100).boxed().collect(Collectors.toList());

        long seqCount = numbers.stream().count();
        long parCount = numbers.parallelStream().count();
        int seqSum = numbers.stream().mapToInt(Integer::intValue).sum();
        int parSum = numbers.parallelStream().mapToInt(Integer::intValue).sum();
        int seqMax = numbers.stream().mapToInt(Integer::intValue).max().orElse(-1);
        int parMax = numbers.parallelStream().mapToInt(Integer::intValue).max().orElse(-1);

        System.out.println("  count  顺序 " + seqCount + " / 并行 " + parCount
                + "  一致吗 → " + (seqCount == parCount));
        System.out.println("  sum    顺序 " + seqSum + " / 并行 " + parSum
                + "  一致吗 → " + (seqSum == parSum));
        System.out.println("  max    顺序 " + seqMax + " / 并行 " + parMax
                + "  一致吗 → " + (seqMax == parMax));
        System.out.println("  ← 这些操作与元素顺序无关：先分片各算各的，再合并结果");
    }

    /**
     * reduce 用非结合性操作会出错——这是并行流最经典的坑。
     *
     * <p>减法不满足结合律：(a-b)-c ≠ a-(b-c)。
     * 并行流把数据分段各算各的再合并，结果自然和顺序流不同。
     */
    static void reduceNeedsAssociativity() {
        List<Integer> numbers = List.of(1, 2, 3, 4);

        int seqAdd = numbers.stream().reduce(0, Integer::sum);
        int parAdd = numbers.parallelStream().reduce(0, Integer::sum);
        System.out.println("  加法（结合性）  顺序 " + seqAdd + " / 并行 " + parAdd
                + "  一致吗 → " + (seqAdd == parAdd));

        int seqSub = numbers.stream().reduce(0, (a, b) -> a - b);
        int parSub = numbers.parallelStream().reduce(0, (a, b) -> a - b);
        System.out.println("  减法（非结合性）顺序 " + seqSub + " / 并行 " + parSub
                + "  一致吗 → " + (seqSub == parSub));
        System.out.println("  ← 减法不满足 (a-b)-c = a-(b-c)，并行分段算完再合并就错了");
        System.out.println("  ← 并行流的结果甚至可能每次不同，取决于分片方式");

        // 乘法是结合的
        int seqMul = numbers.stream().reduce(1, (a, b) -> a * b);
        int parMul = numbers.parallelStream().reduce(1, (a, b) -> a * b);
        System.out.println("  乘法（结合性）  顺序 " + seqMul + " / 并行 " + parMul
                + "  一致吗 → " + (seqMul == parMul));
    }

    /** 并行流里操作非线程安全的集合会出问题。 */
    static void sharedMutableState() throws Exception {
        // 用 ArrayList 收集：并行 add 不是线程安全的。
        // 它的表现有两种：丢元素，或者直接抛异常（我实测两种都撞到过），
        // 所以这里把两者合并成一个稳定的结论：「出问题了吗」。
        List<Integer> unsafe = new ArrayList<>();
        boolean problemDetected;
        try {
            IntStream.range(0, 100_000).parallel().forEach(unsafe::add);
            problemDetected = unsafe.size() < 100_000;
        } catch (Exception e) {
            problemDetected = true;
        }
        System.out.println("  用 ArrayList 收集 10 万个，出问题了吗 → " + problemDetected);
        System.out.println("  （可能是丢元素，也可能直接抛异常，每次运行都可能不同）");

        // 正确做法：用 collect（每个分片各自收集，再合并）
        List<Integer> safe = IntStream.range(0, 100_000).parallel()
                .boxed()
                .collect(Collectors.toList());
        System.out.println("  换成 collect 收集 → 收到 " + safe.size() + " 个");
        System.out.println("  ← collect 给每个分片一个局部容器，最后合并，所以是安全的");
        System.out.println("  ← 而 forEach 直接往共享集合里塞，多个线程同时 add 就出事");
    }

    /** 并行流用的是共享的 commonPool。 */
    static void whichPool() {
        System.out.println("  ForkJoinPool.commonPool 的并行度 → " + ForkJoinPool.commonPool().getParallelism());
        System.out.println("  Runtime 报告的 CPU 核数 → " + Runtime.getRuntime().availableProcessors());
        System.out.println("  ← 并行度通常等于核数减一（留一个给主线程）");
        System.out.println("  ← 关键：整个 JVM 共享这一个池，一个并行流卡住会影响别的并行流");
    }

    /** 并行不一定更快。 */
    static void notAlwaysFaster() {
        // 小数据量：并行的分片/合并开销可能超过收益
        List<Integer> small = IntStream.rangeClosed(1, 100).boxed().collect(Collectors.toList());
        long seqResult = small.stream().mapToLong(Integer::longValue).sum();
        long parResult = small.parallelStream().mapToLong(Integer::longValue).sum();

        System.out.println("  100 个元素：顺序 " + seqResult + " / 并行 " + parResult
                + "  结果一样吗 → " + (seqResult == parResult));
        System.out.println("  ← 数据量小的时候，并行的线程调度开销可能比省下的时间还多");
        System.out.println("  ← 这里故意不打印耗时：微基准抖动太大，贴出来会误导");
        System.out.println("  ← 结论：数据量大 + 计算密集 + 无状态，才考虑并行");
    }

    private static List<String> sortedCopy(List<String> list) {
        List<String> copy;
        synchronized (list) {
            copy = new ArrayList<>(list);
        }
        java.util.Collections.sort(copy);
        return copy;
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("========== " + title + " ==========");
    }
}
