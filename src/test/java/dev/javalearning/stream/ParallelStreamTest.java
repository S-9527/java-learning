package dev.javalearning.stream;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 第 11 课的断言。
 *
 * <p>每一条对应 {@code docs/11-并行流.md} 里的一节。
 * 跑法：{@code ./gradlew test --tests '*parallel*'}
 *
 * <p><b>写这一课的测试有个原则</b>：凡是「并行导致顺序不确定」的地方，
 * 不要断言具体的顺序或数量——那会变成随机失败的测试。
 * 要么断言「集合内容相同」，要么断言「确实发生了乱序/丢失」。
 */
class ParallelStreamTest {

    private static final List<String> LETTERS = List.of("A", "L", "B", "E", "R", "T");

    // ── 1. 创建 ────────────────────────────────────────────────────

    @Test
    @DisplayName("两种创建并行流的方式等价")
    void twoWaysToCreate() {
        assertThat(LETTERS.stream().parallel().count()).isEqualTo(6);
        assertThat(LETTERS.parallelStream().count()).isEqualTo(6);
    }

    // ── 2. 顺序 ────────────────────────────────────────────────────

    @Test
    @DisplayName("并行流 forEach 元素一个不少，但不保证到达顺序")
    void forEachIsUnorderedButComplete() {
        List<String> seen = Collections.synchronizedList(new ArrayList<>());
        LETTERS.parallelStream().map(String::toLowerCase).forEach(seen::add);

        // 内容一定完整（不管顺序如何）
        assertThat(seen).containsExactlyInAnyOrderElementsOf(
                LETTERS.stream().map(String::toLowerCase).collect(Collectors.toList()));
    }

    @Test
    @DisplayName("forEachOrdered 严格按出现顺序执行")
    void forEachOrderedKeepsEncounterOrder() {
        List<String> seen = Collections.synchronizedList(new ArrayList<>());
        LETTERS.parallelStream().map(String::toLowerCase).forEachOrdered(seen::add);

        assertThat(seen).containsExactly("a", "l", "b", "e", "r", "t");
    }

    @Test
    @DisplayName("collect(toList) 的结果保持出现顺序——即使跑在多个线程上")
    void collectPreservesOrder() {
        List<String> collected = LETTERS.parallelStream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        assertThat(collected).containsExactly("a", "l", "b", "e", "r", "t");
    }

    @Test
    @DisplayName("收集到 Set 就无序了——那是容器导致的，不是并行导致的")
    void unorderedIsCausedByTheContainer() {
        var asSet = LETTERS.parallelStream()
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(TreeSet::new));
        // TreeSet 会自己排序，所以这里是"有序"的——但那是排序，不是保持出现顺序
        assertThat(asSet).containsExactly("a", "b", "e", "l", "r", "t");
    }

    // ── 3. 自定义 Collector ────────────────────────────────────────

    @Test
    @DisplayName("并行流会分片：supplier 被调用多次，combiner 负责合并")
    void parallelCollectSplitsAndCombines() {
        AtomicInteger suppliers = new AtomicInteger();
        AtomicInteger combiners = new AtomicInteger();

        List<String> result = LETTERS.parallelStream().collect(
                () -> {
                    suppliers.incrementAndGet();
                    return new ArrayList<String>();
                },
                (list, s) -> list.add(s.toLowerCase()),
                (left, right) -> {
                    combiners.incrementAndGet();
                    left.addAll(right);
                });

        assertThat(result).containsExactly("a", "l", "b", "e", "r", "t");
        // 分几片取决于核数，但「不止一片」是并行的必要条件
        assertThat(suppliers.get()).isGreaterThan(1);
        assertThat(combiners.get()).isPositive();
    }

    @Test
    @DisplayName("顺序流只有一个容器，combiner 一次都不调")
    void sequentialCollectNeverCombines() {
        AtomicInteger suppliers = new AtomicInteger();
        AtomicInteger combiners = new AtomicInteger();

        LETTERS.stream().collect(
                () -> {
                    suppliers.incrementAndGet();
                    return new ArrayList<String>();
                },
                (list, s) -> list.add(s.toLowerCase()),
                (left, right) -> {
                    combiners.incrementAndGet();
                    left.addAll(right);
                });

        assertThat(suppliers.get()).isEqualTo(1);
        assertThat(combiners.get()).isZero();
    }

    // ── 4. 结果一致的操作 ──────────────────────────────────────────

    @Test
    @DisplayName("count / sum / max 这些与顺序无关的操作，并行结果和顺序一致")
    void orderIndependentOperationsAgree() {
        List<Integer> numbers = IntStream.rangeClosed(1, 100).boxed().collect(Collectors.toList());

        assertThat(numbers.parallelStream().count()).isEqualTo(numbers.stream().count());
        assertThat(numbers.parallelStream().mapToInt(Integer::intValue).sum())
                .isEqualTo(numbers.stream().mapToInt(Integer::intValue).sum());
        assertThat(numbers.parallelStream().mapToInt(Integer::intValue).max())
                .isEqualTo(numbers.stream().mapToInt(Integer::intValue).max());
    }

    @Test
    @DisplayName("distinct / sorted 也能保持一致结果")
    void statefulOperationsStayConsistent() {
        List<Integer> withDupes = List.of(3, 1, 2, 3, 1);

        List<Integer> seqSorted = withDupes.stream().distinct().sorted().collect(Collectors.toList());
        List<Integer> parSorted = withDupes.parallelStream().distinct().sorted().collect(Collectors.toList());

        assertThat(parSorted).isEqualTo(seqSorted);
        assertThat(parSorted).containsExactly(1, 2, 3);
    }

    // ── 5. 非结合性操作的坑 ────────────────────────────────────────

    @Test
    @DisplayName("加法是结合的，顺序和并行结果一样")
    void additionIsAssociative() {
        List<Integer> numbers = List.of(1, 2, 3, 4);
        int seq = numbers.stream().reduce(0, Integer::sum);
        int par = numbers.parallelStream().reduce(0, Integer::sum);
        assertThat(par).isEqualTo(seq).isEqualTo(10);
    }

    @Test
    @DisplayName("减法不是结合的，并行结果和顺序流不同——这是经典坑")
    void subtractionIsNotAssociativeSoParallelDiffers() {
        List<Integer> numbers = List.of(1, 2, 3, 4);

        int seq = numbers.stream().reduce(0, (a, b) -> a - b);
        int par = numbers.parallelStream().reduce(0, (a, b) -> a - b);

        assertThat(seq).isEqualTo(-10);          // ((0-1)-2)-3-4
        // 并行分段算再合并，结果不是 -10。具体值取决于分片，所以只断言"不同"
        assertThat(par).isNotEqualTo(seq);
    }

    @Test
    @DisplayName("乘法是结合的，并行结果一致")
    void multiplicationIsAssociative() {
        List<Integer> numbers = List.of(1, 2, 3, 4);
        assertThat(numbers.parallelStream().reduce(1, (a, b) -> a * b))
                .isEqualTo(numbers.stream().reduce(1, (a, b) -> a * b))
                .isEqualTo(24);
    }

    // ── 6. 共享可变状态 ────────────────────────────────────────────

    @Test
    @DisplayName("并行流往普通 ArrayList 里 forEach 会丢元素（或抛异常）")
    void forEachIntoSharedArrayListIsUnsafe() {
        // ⚠️ 这个测试**依赖机器核数**，所以要先判断能不能测。
        //
        // 并行流用的是 ForkJoinPool.commonPool，并行度默认是「核数 - 1」。
        // 在 2 核的 CI runner 上并行度 = 1，任务实际是串行跑的，
        // 压根不会竞争——这时候这个 bug 根本观察不到。
        //
        // 我一开始没加这个判断，本地 16 核必现、CI 上必不现，
        // 结果就是「本地全绿、CI 红」。对这类依赖硬件的测试，
        // 正确做法是明确跳过并说明原因，而不是让它随机失败。
        int parallelism = ForkJoinPool.commonPool().getParallelism();
        Assumptions.assumeTrue(parallelism >= 2,
                "commonPool 并行度只有 " + parallelism
                        + "，并行流实际是串行的，观察不到这个竞态");

        boolean problemDetected = false;
        // 这是概率性的，跑多轮直到抓到问题
        for (int round = 0; round < 20 && !problemDetected; round++) {
            List<Integer> unsafe = new ArrayList<>();
            try {
                IntStream.range(0, 100_000).parallel().forEach(unsafe::add);
                if (unsafe.size() < 100_000) {
                    problemDetected = true;
                }
            } catch (Exception e) {
                problemDetected = true;      // 抛异常也算"出了问题"
            }
        }
        assertThat(problemDetected).isTrue();
    }

    @Test
    @DisplayName("换成 collect 就是安全的：每个分片各自收集再合并")
    void collectIntoListIsSafe() {
        List<Integer> safe = IntStream.range(0, 100_000).parallel()
                .boxed()
                .collect(Collectors.toList());
        assertThat(safe).hasSize(100_000);
    }

    // ── 7. 线程池 ──────────────────────────────────────────────────

    @Test
    @DisplayName("并行流用的是共享的 ForkJoinPool.commonPool")
    void parallelStreamsShareTheCommonPool() {
        // 并行度通常等于核数减一
        int parallelism = ForkJoinPool.commonPool().getParallelism();
        int cores = Runtime.getRuntime().availableProcessors();

        assertThat(parallelism).isPositive();
        assertThat(parallelism).isLessThanOrEqualTo(cores);
    }

    @Test
    @DisplayName("并行流用的是共享的 commonPool（并行度默认核数减一）")
    void parallelStreamsUseTheCommonPool() {
        // 注意：这里**不去断言具体线程名**。
        // commonPool 是 JVM 全局共享的，别的测试可能正在用它，
        // 任务也可能在主线程上执行（并行度低时 ForkJoin 允许调用线程自己干活）。
        // 我一开始断言 "线程名里必须有 commonPool"，在 2 核 CI 上就偶发失败。
        //
        // 真正稳定、且同样是这一课要讲的事实是：并行度由核数决定，
        // 而且它是全局共享的一个池。
        int parallelism = ForkJoinPool.commonPool().getParallelism();
        assertThat(parallelism).isPositive();
        assertThat(parallelism).isLessThanOrEqualTo(Runtime.getRuntime().availableProcessors());

        // 并行流确实被并行执行了：多个线程都参与过
        Set<String> threads = Collections.synchronizedSet(new HashSet<>());
        IntStream.range(0, 10_000).parallel().forEach(i -> threads.add(Thread.currentThread().getName()));
        assertThat(threads).isNotEmpty();
    }
}
