package dev.javalearning.concurrency.pool;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 第 17 课的示例代码：线程池。
 *
 * <p>直接跑：{@code java src/main/java/dev/javalearning/concurrency/pool/PoolDemo.java}
 * 每一节对应 {@code docs/17-线程池.md} 里的一节，输出都是真跑出来的。
 */
public class PoolDemo {

    public static void main(String[] args) throws Exception {
        section("1. 为什么不直接用 new Thread");
        costOfRawThreads();

        section("2. 七个参数，和真实的执行顺序");
        executionOrderDemo();

        section("3. 队列满了之后的四种拒绝策略");
        rejectionPolicies();

        section("4. 为什么阿里手册不让用 Executors");
        executorsPitfalls();

        section("5. execute vs submit：一个吞异常，一个能拿到");
        executeVsSubmit();

        section("6. 优雅停机：shutdown vs shutdownNow");
        gracefulShutdown();

        section("7. 线程工厂：给线程起名字");
        namedThreads();
    }

    /** 每个任务新建一个线程，创建和销毁都要花钱。 */
    static void costOfRawThreads() throws Exception {
        int tasks = 2000;
        long start = System.nanoTime();
        Thread[] threads = new Thread[tasks];
        for (int i = 0; i < tasks; i++) {
            threads[i] = new Thread(() -> { });
            threads[i].start();
        }
        for (Thread t : threads) {
            t.join();
        }
        long rawMillis = (System.nanoTime() - start) / 1_000_000;

        start = System.nanoTime();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch done = new CountDownLatch(tasks);
        for (int i = 0; i < tasks; i++) {
            pool.execute(done::countDown);
        }
        done.await();
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);
        long poolMillis = (System.nanoTime() - start) / 1_000_000;

        // 性能测量天生不稳定（同一份代码跑十次能差一倍），所以这里
        // 一个具体数字都不打印，只输出结论。想看真实数字就自己跑一遍。
        System.out.println("  两种做法各跑 " + tasks + " 个空任务");
        System.out.println("  池子明显更快吗 → " + (poolMillis * 3 < rawMillis));
        System.out.println("  ← 池子复用了线程，省掉了反复创建/销毁的开销");
        System.out.println("  ← 这里不打印具体毫秒数：同一份代码跑十次能差一倍，贴出来会误导");
    }

    /**
     * 七个参数：
     * corePoolSize、maximumPoolSize、keepAliveTime、unit、workQueue、threadFactory、handler
     *
     * <p>执行顺序：核心线程 → 队列 → 最大线程 → 拒绝。
     */
    static void executionOrderDemo() throws Exception {
        AtomicInteger seq = new AtomicInteger();
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                1,                                  // 核心 1 个
                2,                                  // 最多 2 个
                0L, TimeUnit.MILLISECONDS,          // 非核心线程空闲就回收
                new ArrayBlockingQueue<>(2),        // 队列容量 2
                r -> new Thread(r, "worker-" + seq.incrementAndGet()),
                new ThreadPoolExecutor.AbortPolicy());

        StringBuilder log = new StringBuilder();
        for (int i = 1; i <= 6; i++) {
            final int n = i;
            try {
                pool.execute(() -> sleep(400));
                log.append(String.format("  提交任务%d 成功  (线程数=%d, 队列=%d)%n",
                        n, pool.getPoolSize(), pool.getQueue().size()));
            } catch (RejectedExecutionException e) {
                log.append(String.format("  提交任务%d 被拒绝  (线程数=%d, 队列=%d)%n",
                        n, pool.getPoolSize(), pool.getQueue().size()));
            }
        }
        System.out.print(log);
        System.out.println("  ← 顺序是：先占满核心线程 → 再进队列 → 队列满了才加到最大线程 → 再满就拒绝");

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
    }

    /** 四种拒绝策略。 */
    static void rejectionPolicies() throws Exception {
        System.out.println("  AbortPolicy（默认）       → 抛 RejectedExecutionException");
        System.out.println("  CallerRunsPolicy          → 提交任务的线程自己跑（变相降速）");
        System.out.println("  DiscardPolicy             → 静默丢掉");
        System.out.println("  DiscardOldestPolicy       → 丢掉队列里最老的那个，再试一次");

        // 演示 CallerRunsPolicy：任务由 main 线程自己执行
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1),
                new ThreadPoolExecutor.CallerRunsPolicy());

        CountDownLatch firstRunning = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        pool.execute(() -> {
            firstRunning.countDown();
            try {
                releaseFirst.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        firstRunning.await();
        pool.execute(() -> sleep(10));      // 进队列
        // 现在核心线程忙、队列也满了，第三个任务会被 CallerRunsPolicy 交给 main
        String before = Thread.currentThread().getName();
        pool.execute(() -> System.out.println("  第三个任务由谁执行 → " + Thread.currentThread().getName()
                + "（提交它的线程是 " + before + "）"));
        releaseFirst.countDown();
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("  ← CallerRunsPolicy 让调用方自己跑，等于给上游一个背压信号");
    }

    /** Executors 的快捷方法各有什么坑。 */
    static void executorsPitfalls() throws Exception {
        // 注意：只有部分快捷方法返回真正的 ThreadPoolExecutor，可以直接查看内部；
        // newSingleThreadExecutor / newWorkStealingPool 返回的是包装过的实现，
        // 强转会抛 ClassCastException（我第一版就是这么挂的）。
        dumpPool("newFixedThreadPool(2)", Executors.newFixedThreadPool(2));
        dumpPool("newCachedThreadPool()", Executors.newCachedThreadPool());
        dumpPool("newScheduledThreadPool(2)", Executors.newScheduledThreadPool(2));

        // 这两个不能强转，单独看它们的类型
        ExecutorService single = Executors.newSingleThreadExecutor();
        System.out.println("  newSingleThreadExecutor() 的实际类型 → "
                + single.getClass().getSimpleName());
        System.out.println("    不能强转成 ThreadPoolExecutor（它是包装过的）");
        single.shutdown();

        ExecutorService stealing = Executors.newWorkStealingPool();
        System.out.println("  newWorkStealingPool() 的实际类型 → "
                + stealing.getClass().getSimpleName() + "（另一套实现，不是 ThreadPoolExecutor）");
        stealing.shutdown();

        System.out.println("  ← 结论：这几个快捷方法要么队列无界、要么线程无界，都可能 OOM");
        System.out.println("  ← 生产上应该手动 new ThreadPoolExecutor，把七个参数写清楚");
    }

    /** 打印一个池的关键参数，用来判断它有没有 OOM 风险。 */
    static void dumpPool(String label, ExecutorService pool) {
        System.out.println("  " + label);
        if (pool instanceof ThreadPoolExecutor tpe) {
            long remaining = tpe.getQueue().remainingCapacity();
            System.out.println("    队列 → " + tpe.getQueue().getClass().getSimpleName()
                    + "，剩余容量 = " + remaining
                    + (remaining == Integer.MAX_VALUE ? "  ← 无界！" : ""));
            System.out.println("    最大线程数 = " + tpe.getMaximumPoolSize()
                    + (tpe.getMaximumPoolSize() == Integer.MAX_VALUE ? "  ← 无界！" : ""));
        } else {
            System.out.println("    不是 ThreadPoolExecutor：" + pool.getClass().getSimpleName());
        }
        pool.shutdown();
    }

    /** execute 抛出的异常没人管，submit 包在 Future 里，不 get 就看不见。 */
    static void executeVsSubmit() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);

        // execute：异常直接冒到线程的未捕获处理器，主线程完全不知道。
        // 为了不让堆栈刷屏（而且堆栈里的行号/线程名每次不同），
        // 这里给这个池单独设一个未捕获异常处理器，把关键信息捞出来。
        ExecutorService observed = Executors.newFixedThreadPool(1, r -> {
            Thread t = new Thread(r, "execute-demo");
            t.setUncaughtExceptionHandler((thread, e) ->
                    System.out.println("  未捕获异常处理器收到 → " + thread.getName()
                            + " 抛了 " + e.getClass().getSimpleName() + ": " + e.getMessage()));
            return t;
        });
        observed.execute(() -> {
            throw new IllegalStateException("execute 里抛的");
        });
        Thread.sleep(200);
        System.out.println("  execute 提交的任务抛异常 → 主线程完全收不到（只有未捕获处理器知道）");
        observed.shutdown();

        // submit：异常被包进 Future，不调 get 就永远看不到
        Future<?> future = pool.submit(() -> {
            throw new IllegalStateException("submit 里抛的");
        });
        Thread.sleep(200);
        System.out.println("  submit 提交的任务抛异常  → 不调 get() 的话同样看不到");
        try {
            future.get();
        } catch (java.util.concurrent.ExecutionException e) {
            System.out.println("  调 future.get() 之后    → 抛 ExecutionException，原因是 "
                    + e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage());
        }

        // submit 能拿到返回值
        Future<Integer> result = pool.submit(() -> 6 * 7);
        System.out.println("  submit 的返回值         → " + result.get());

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
    }

    /** shutdown 不再接新任务但把队列里的跑完；shutdownNow 尝试中断并返回没跑的任务。 */
    static void gracefulShutdown() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(1);
        AtomicInteger finished = new AtomicInteger();

        for (int i = 0; i < 3; i++) {
            pool.execute(() -> {
                sleep(150);
                finished.incrementAndGet();
            });
        }
        pool.shutdown();     // 不再接新任务，队列里的继续跑
        System.out.println("  shutdown() 之后 isShutdown = " + pool.isShutdown());
        boolean terminated = pool.awaitTermination(3, TimeUnit.SECONDS);
        System.out.println("  等它跑完 → 完成了 " + finished.get() + " 个，terminated = " + terminated);

        ExecutorService pool2 = Executors.newFixedThreadPool(1);
        for (int i = 0; i < 5; i++) {
            pool2.execute(() -> sleep(2000));
        }
        Thread.sleep(100);
        java.util.List<Runnable> neverRan = pool2.shutdownNow();   // 尝试中断，返回没开始跑的
        System.out.println("  shutdownNow() 返回了 " + neverRan.size() + " 个没来得及跑的任务");
        System.out.println("  ← 注意：它只是「尝试」中断，不响应中断的任务照样跑完");
    }

    /** 给线程起名字，排查问题时能一眼看出是谁。 */
    static void namedThreads() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        ThreadFactory factory = r -> {
            Thread t = new Thread(r, "order-pool-" + counter.incrementAndGet());
            t.setDaemon(false);
            return t;
        };

        ExecutorService pool = Executors.newFixedThreadPool(3, factory);
        CountDownLatch done = new CountDownLatch(3);
        // 三个任务并发跑，谁先打印不确定，所以先收集再排序
        java.util.List<String> names = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        for (int i = 0; i < 3; i++) {
            pool.execute(() -> {
                names.add(Thread.currentThread().getName());
                done.countDown();
            });
        }
        done.await();
        java.util.List<String> sorted = new java.util.ArrayList<>(names);
        java.util.Collections.sort(sorted);
        for (String name : sorted) {
            System.out.println("  任务跑在 → " + name);
        }
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("  ← 线程名带业务含义，jstack 里一眼能认出是哪个池");
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("========== " + title + " ==========");
    }
}
