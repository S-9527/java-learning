package dev.javalearning.concurrency.collections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 第 18 课的示例代码：并发集合与 ThreadLocal（上半场，集合部分）。
 *
 * <p>直接跑：{@code java src/main/java/dev/javalearning/concurrency/collections/CollectionsDemo.java}
 * 每一节对应 {@code docs/18-并发集合与ThreadLocal.md} 里的一节。
 */
public class CollectionsDemo {

    public static void main(String[] args) throws Exception {
        section("1. HashMap 多线程 put：元素会丢");
        hashMapLosesData();

        section("2. ConcurrentHashMap 同样操作：一个都不丢");
        concurrentHashMapKeepsEverything();

        section("3. synchronizedMap 的 check-then-act 依然不是原子的");
        synchronizedMapCheckThenAct();

        section("4. ConcurrentHashMap 的复合操作是原子的");
        atomicCompoundOperation();

        section("5. ArrayList 迭代中修改 → ConcurrentModificationException");
        arrayListFailsFast();

        section("6. ConcurrentHashMap 的迭代器是弱一致的，不抛异常");
        concurrentHashMapWeaklyConsistent();

        section("7. CopyOnWriteArrayList：迭代器看到的是旧快照");
        copyOnWriteSnapshot();
    }

    /** HashMap 并发 put，元素数量对不上。 */
    static void hashMapLosesData() throws Exception {
        int threads = 4;
        int perThread = 10000;
        int expected = threads * perThread;

        // 具体丢多少个每次都不一样（这正是竞态的特征），所以只统计"丢没丢"
        int rounds = 10;
        int roundsWithLoss = 0;
        for (int round = 0; round < rounds; round++) {
            Map<Integer, Integer> map = new HashMap<>();
            Thread[] ts = new Thread[threads];
            for (int i = 0; i < threads; i++) {
                final int base = i * perThread;
                ts[i] = new Thread(() -> {
                    for (int j = 0; j < perThread; j++) {
                        map.put(base + j, j);
                    }
                });
                ts[i].start();
            }
            for (Thread t : ts) {
                t.join();
            }
            if (map.size() < expected) {
                roundsWithLoss++;
            }
        }
        System.out.println("  期望 " + expected + " 个元素，跑 " + rounds + " 轮");
        // 丢的轮数本身也是随机的（8~10 之间抖），所以只说结论，不报数字
        System.out.println("  出现过元素丢失吗 → " + (roundsWithLoss > 0));
        System.out.println("  ← HashMap 不是线程安全的，并发 put 会互相覆盖");
        System.out.println("  ← 丢多少个、哪几轮丢，每次运行都不同，这正是竞态的特征");
    }

    /** ConcurrentHashMap 同样操作，数量永远对。 */
    static void concurrentHashMapKeepsEverything() throws Exception {
        int threads = 4;
        int perThread = 10000;
        int expected = threads * perThread;

        int rounds = 10;
        int roundsWithLoss = 0;
        for (int round = 0; round < rounds; round++) {
            Map<Integer, Integer> map = new ConcurrentHashMap<>();
            Thread[] ts = new Thread[threads];
            for (int i = 0; i < threads; i++) {
                final int base = i * perThread;
                ts[i] = new Thread(() -> {
                    for (int j = 0; j < perThread; j++) {
                        map.put(base + j, j);
                    }
                });
                ts[i].start();
            }
            for (Thread t : ts) {
                t.join();
            }
            if (map.size() < expected) {
                roundsWithLoss++;
            }
        }
        System.out.println("  期望 " + expected + " 个元素，跑 " + rounds + " 轮");
        System.out.println("  出现过元素丢失吗 → " + (roundsWithLoss > 0));
        System.out.println("  一个都没丢吗 → " + (roundsWithLoss == 0));
    }

    /**
     * Collections.synchronizedMap 把每个方法都加了锁，但**复合操作**还是不安全。
     *
     * <p>"先检查再写"（check-then-act）是两步，中间可能被别人插进来。
     */
    static void synchronizedMapCheckThenAct() throws Exception {
        // 跑多轮统计：只要有一轮出现「两个都写了」，就证明 check-then-act 不是原子的。
        // 单轮里谁赢是随机的，所以不能断言具体的值。
        int overwrittenRounds = 0;
        int rounds = 200;
        for (int round = 0; round < rounds; round++) {
            Map<String, Integer> sync = Collections.synchronizedMap(new HashMap<>());
            sync.put("a", 0);
            java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);

            // 两个线程都做「如果还是 0 就写自己的值」
            Thread t1 = new Thread(() -> {
                await(start);
                if (sync.get("a") == 0) {
                    sync.put("a", 1);
                }
            });
            Thread t2 = new Thread(() -> {
                await(start);
                if (sync.get("a") == 0) {
                    sync.put("a", 2);
                }
            });
            t1.start();
            t2.start();
            start.countDown();     // 让两个线程同时冲
            t1.join();
            t2.join();
            // 如果两个都通过检查，那 a 就是最后一个写的值——但两者都以为自己是第一个
            if (sync.get("a") != 0) {
                overwrittenRounds++;
            }
        }
        System.out.println("  两个线程都想「如果 a 还是 0 就写」，跑 " + rounds + " 轮");
        System.out.println("  出现「双方都写了」的轮数 → " + overwrittenRounds + " / " + rounds);
        System.out.println("  ← 单次方法是原子的，但「判断 + 写入」合起来不是——这就是 check-then-act");
        System.out.println("  ← 谁最后赢是随机的，所以这里只统计「有没有发生覆盖」");
    }

    /** ConcurrentHashMap 提供了原子的复合操作。 */
    static void atomicCompoundOperation() throws Exception {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        java.util.concurrent.atomic.AtomicInteger mapperCalls = new java.util.concurrent.atomic.AtomicInteger();

        Thread t1 = new Thread(() -> map.computeIfAbsent("k", k -> {
            mapperCalls.incrementAndGet();
            return 1;
        }));
        Thread t2 = new Thread(() -> map.computeIfAbsent("k", k -> {
            mapperCalls.incrementAndGet();
            return 2;
        }));
        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("  computeIfAbsent 的映射函数一共被调用了 " + mapperCalls.get() + " 次");
        System.out.println("  k 有值吗 → " + (map.get("k") != null));
        System.out.println("  ← 整个「检查 + 计算 + 放入」是原子的，所以映射函数只会成功执行一次");
        System.out.println("  ← 至于 k 是 1 还是 2，取决于哪个线程先到，每次可能不同");

        // 其他原子的复合操作
        ConcurrentHashMap<String, Integer> m = new ConcurrentHashMap<>();
        m.put("count", 1);
        m.merge("count", 10, Integer::sum);
        System.out.println("  merge 之后 count = " + m.get("count"));
        System.out.println("  putIfAbsent 已存在时不覆盖 → " + m.putIfAbsent("count", 999));
        System.out.println("  replace 只在值匹配时才换 → " + m.replace("count", 11, 100));
        System.out.println("  最终 count = " + m.get("count"));
    }

    /** ArrayList 的迭代器是 fail-fast 的，边迭代边改直接抛异常。 */
    static void arrayListFailsFast() {
        List<Integer> list = new ArrayList<>(List.of(1, 2, 3));
        try {
            for (Integer x : list) {
                if (x == 1) {
                    list.add(99);
                }
            }
            System.out.println("  没抛异常");
        } catch (ConcurrentModificationException e) {
            System.out.println("  ConcurrentModificationException");
            System.out.println("  ← fail-fast：宁可立刻报错，也不给你一个不确定的结果");
        }
    }

    /** ConcurrentHashMap 的迭代器是弱一致的，迭代中改不会抛异常。 */
    static void concurrentHashMapWeaklyConsistent() {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        try {
            for (String key : map.keySet()) {
                if (key.equals("a")) {
                    map.put("c", 3);
                }
            }
            System.out.println("  迭代中修改 → 没抛异常");
            System.out.println("  ← 弱一致（weakly consistent）：不保证看到最新修改，但也不会抛异常");
            System.out.println("  最终内容里有 c 吗 → " + map.containsKey("c"));
        } catch (ConcurrentModificationException e) {
            System.out.println("  ConcurrentModificationException");
        }
    }

    /** CopyOnWriteArrayList：写的时候复制一份，迭代器拿到的是旧快照。 */
    static void copyOnWriteSnapshot() {
        CopyOnWriteArrayList<Integer> list = new CopyOnWriteArrayList<>(List.of(1, 2, 3));
        Iterator<Integer> it = list.iterator();

        list.add(4);   // 写时复制，不影响已经在手上的迭代器

        StringBuilder seen = new StringBuilder();
        while (it.hasNext()) {
            seen.append(it.next()).append(' ');
        }
        System.out.println("  迭代器看到的 → " + seen.toString().trim());
        System.out.println("  集合的实际内容 → " + list);
        System.out.println("  ← 迭代器拿到的是「创建它那一刻」的快照，之后的新增看不到");
        System.out.println("  ← 适合「读多写极少」的场景，写一次复制整个数组，很贵");
    }

    private static void await(java.util.concurrent.CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("========== " + title + " ==========");
    }
}
