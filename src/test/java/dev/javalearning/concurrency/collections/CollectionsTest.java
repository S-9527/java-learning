package dev.javalearning.concurrency.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 第 18 课的断言。
 *
 * <p>每一条对应 {@code docs/18-并发集合与ThreadLocal.md} 里的一节。
 * 跑法：{@code ./gradlew test --tests '*collections*'}
 */
class CollectionsTest {

    // ── 1. HashMap vs ConcurrentHashMap ────────────────────────────

    @Test
    @DisplayName("HashMap 并发 put 会丢元素")
    void hashMapLosesElementsUnderConcurrency() throws Exception {
        int threads = 4;
        int perThread = 10_000;
        boolean lost = false;

        // 竞态是概率性的，跑多轮直到抓到一次丢失
        for (int round = 0; round < 10 && !lost; round++) {
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
            if (map.size() < threads * perThread) {
                lost = true;
            }
        }
        assertThat(lost).isTrue();
    }

    @Test
    @DisplayName("ConcurrentHashMap 同样操作一个都不丢")
    void concurrentHashMapKeepsEverything() throws Exception {
        int threads = 4;
        int perThread = 10_000;
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
        assertThat(map).hasSize(threads * perThread);
    }

    @Test
    @DisplayName("ConcurrentHashMap 不允许 null 键值")
    void concurrentHashMapRejectsNull() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
        assertThatThrownBy(() -> map.put(null, "v")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> map.put("k", null)).isInstanceOf(NullPointerException.class);
        // 而 HashMap 允许
        Map<String, String> hashMap = new HashMap<>();
        hashMap.put(null, null);
        assertThat(hashMap).containsKey(null);
    }

    // ── 2. synchronizedMap 的复合操作 ──────────────────────────────

    @Test
    @DisplayName("synchronizedMap 的单次方法安全，但 check-then-act 不是原子的")
    void synchronizedMapCheckThenActIsNotAtomic() throws Exception {
        int rounds = 200;
        int overwritten = 0;

        for (int round = 0; round < rounds; round++) {
            Map<String, Integer> sync = Collections.synchronizedMap(new HashMap<>());
            sync.put("a", 0);
            CountDownLatch start = new CountDownLatch(1);

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
            start.countDown();
            t1.join();
            t2.join();
            if (sync.get("a") != 0) {
                overwritten++;
            }
        }
        // 两个线程同时通过检查，说明「判断 + 写入」之间被插进来了
        assertThat(overwritten).isPositive();
    }

    @Test
    @DisplayName("ConcurrentHashMap 的 computeIfAbsent 整个操作是原子的")
    void computeIfAbsentIsAtomic() throws Exception {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        AtomicInteger mapperCalls = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);

        Runnable task = () -> {
            await(start);
            map.computeIfAbsent("k", k -> {
                mapperCalls.incrementAndGet();
                return 1;
            });
            done.countDown();
        };
        new Thread(task).start();
        new Thread(task).start();
        start.countDown();
        done.await();

        // 映射函数只成功执行一次，说明检查+计算+放入是原子的
        assertThat(mapperCalls.get()).isEqualTo(1);
        assertThat(map.get("k")).isEqualTo(1);
    }

    @Test
    @DisplayName("merge / putIfAbsent / replace 也都是原子的复合操作")
    void otherAtomicCompoundOperations() {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        map.put("count", 1);

        map.merge("count", 10, Integer::sum);
        assertThat(map.get("count")).isEqualTo(11);

        // 已存在，不覆盖
        assertThat(map.putIfAbsent("count", 999)).isEqualTo(11);
        assertThat(map.get("count")).isEqualTo(11);

        // 值匹配才替换
        assertThat(map.replace("count", 11, 100)).isTrue();
        assertThat(map.get("count")).isEqualTo(100);
        assertThat(map.replace("count", 11, 999)).isFalse();
    }

    // ── 3. 迭代器行为 ──────────────────────────────────────────────

    @Test
    @DisplayName("ArrayList 的迭代器是 fail-fast，迭代中修改抛异常")
    void arrayListIteratorFailsFast() {
        List<Integer> list = new ArrayList<>(List.of(1, 2, 3));
        assertThatThrownBy(() -> {
            for (Integer x : list) {
                if (x == 1) {
                    list.add(99);
                }
            }
        }).isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    @DisplayName("ConcurrentHashMap 的迭代器是弱一致的，迭代中修改不抛异常")
    void concurrentHashMapIteratorIsWeaklyConsistent() {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        // 不抛异常
        for (String key : map.keySet()) {
            if (key.equals("a")) {
                map.put("c", 3);
            }
        }
        assertThat(map).containsKey("c");
    }

    @Test
    @DisplayName("CopyOnWriteArrayList 的迭代器看到的是创建它那一刻的快照")
    void copyOnWriteIteratorSeesASnapshot() {
        CopyOnWriteArrayList<Integer> list = new CopyOnWriteArrayList<>(List.of(1, 2, 3));
        Iterator<Integer> it = list.iterator();

        list.add(4);   // 写时复制

        List<Integer> seen = new ArrayList<>();
        while (it.hasNext()) {
            seen.add(it.next());
        }
        assertThat(seen).containsExactly(1, 2, 3);   // 看不到 4
        assertThat(list).containsExactly(1, 2, 3, 4); // 但集合里有
    }

    @Test
    @DisplayName("CopyOnWriteArrayList 迭代中修改也不抛异常")
    void copyOnWriteAllowsModificationDuringIteration() {
        CopyOnWriteArrayList<Integer> list = new CopyOnWriteArrayList<>(List.of(1, 2, 3));
        for (Integer x : list) {
            if (x == 1) {
                list.add(99);
            }
        }
        assertThat(list).contains(99);
    }

    // ── 4. ThreadLocal ─────────────────────────────────────────────

    @Test
    @DisplayName("每个线程有自己的一份，互相看不见")
    void threadLocalIsolatesPerThread() throws Exception {
        ThreadLocal<String> tl = new ThreadLocal<>();
        tl.set("主线程");

        AtomicReference<String> childSees = new AtomicReference<>("未设置");
        Thread t = new Thread(() -> {
            childSees.set(tl.get());       // 子线程看不到主线程的值
            tl.set("子线程");
        });
        t.start();
        t.join();

        assertThat(childSees.get()).isNull();
        assertThat(tl.get()).isEqualTo("主线程");   // 主线程也没被子线程影响
        tl.remove();
    }

    @Test
    @DisplayName("withInitial 提供默认值，省掉判空")
    void withInitialProvidesDefault() {
        ThreadLocal<Integer> counter = ThreadLocal.withInitial(() -> 0);
        assertThat(counter.get()).isZero();
        counter.set(counter.get() + 1);
        assertThat(counter.get()).isEqualTo(1);
        counter.remove();
    }

    @Test
    @DisplayName("线程池复用线程，上一个任务没清理的数据会被下一个读到")
    void threadPoolReusesThreadsSoDataResidues() throws Exception {
        ThreadLocal<String> ctx = new ThreadLocal<>();
        ExecutorService pool = Executors.newFixedThreadPool(1);

        pool.execute(() -> ctx.set("用户A"));
        AtomicReference<String> secondSees = new AtomicReference<>();
        pool.execute(() -> secondSees.set(ctx.get()));

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        // 第二个任务读到了第一个任务留下的值——这就是泄漏的可见形态
        assertThat(secondSees.get()).isEqualTo("用户A");
        ctx.remove();
    }

    @Test
    @DisplayName("用完 remove，下一个任务就读不到了")
    void removeClearsTheValue() throws Exception {
        ThreadLocal<String> ctx = new ThreadLocal<>();
        ExecutorService pool = Executors.newFixedThreadPool(1);

        pool.execute(() -> {
            ctx.set("用户A");
            ctx.remove();
        });
        AtomicReference<String> secondSees = new AtomicReference<>("未执行");
        pool.execute(() -> secondSees.set(ctx.get()));

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        assertThat(secondSees.get()).isNull();
    }

    @Test
    @DisplayName("ThreadLocalMap 的 key 是弱引用：没有外部强引用时会被 GC 回收")
    void threadLocalKeyIsWeaklyReferenced() throws Exception {
        ThreadLocal<byte[]> leaky = new ThreadLocal<>();
        leaky.set(new byte[1024]);
        WeakReference<ThreadLocal<?>> ref = new WeakReference<>(leaky);

        leaky = null;
        System.gc();
        Thread.sleep(200);

        // key 被回收了——而 value 还挂在线程的 map 上，这就是泄漏
        assertThat(ref.get()).isNull();
    }

    @Test
    @DisplayName("ThreadLocal 的典型用法：请求上下文，用完在 finally 里清理")
    void typicalRequestContextUsage() throws Exception {
        ThreadLocal<String> traceId = new ThreadLocal<>();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<String> collected = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch done = new CountDownLatch(3);

        for (String user : new String[] {"alice", "bob", "carol"}) {
            pool.execute(() -> {
                try {
                    traceId.set("trace-" + user);
                    collected.add(traceId.get());
                } finally {
                    traceId.remove();       // ← 必须
                }
                done.countDown();
            });
        }
        done.await();
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        assertThat(collected).containsExactlyInAnyOrder("trace-alice", "trace-bob", "trace-carol");
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
