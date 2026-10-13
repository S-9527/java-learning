package dev.javalearning.concurrency.pool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 第 17 课的断言。
 *
 * <p>每一条对应 {@code docs/17-线程池.md} 里的一节。
 * 跑法：{@code ./gradlew test --tests '*pool*'}
 */
class PoolTest {

    // ── 1. 执行顺序 ────────────────────────────────────────────────

    @Test
    @DisplayName("提交顺序：先占满核心线程 → 再进队列 → 队列满了才加到最大线程 → 再满就拒绝")
    void executionOrderIsCoreThenQueueThenMaxThenReject() throws Exception {
        AtomicInteger seq = new AtomicInteger();
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                1, 2, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(2),
                r -> new Thread(r, "t" + seq.incrementAndGet()),
                new ThreadPoolExecutor.AbortPolicy());

        List<String> states = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            final int n = i;
            try {
                pool.execute(() -> sleep(300));
                states.add(n + ":成功:线程数=" + pool.getPoolSize() + ",队列=" + pool.getQueue().size());
            } catch (RejectedExecutionException e) {
                states.add(n + ":拒绝:线程数=" + pool.getPoolSize() + ",队列=" + pool.getQueue().size());
            }
        }

        assertThat(states).containsExactly(
                "1:成功:线程数=1,队列=0",   // 核心线程
                "2:成功:线程数=1,队列=1",   // 进队列
                "3:成功:线程数=1,队列=2",   // 队列满
                "4:成功:线程数=2,队列=2",   // 才加非核心线程
                "5:拒绝:线程数=2,队列=2",   // 全满 → 拒绝
                "6:拒绝:线程数=2,队列=2");

        pool.shutdownNow();
    }

    @Test
    @DisplayName("核心线程数满了但队列没满时，不会新建线程")
    void queueIsFilledBeforeGrowingBeyondCore() throws Exception {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                1, 5, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(3));
        pool.execute(() -> sleep(500));
        pool.execute(() -> sleep(10));
        pool.execute(() -> sleep(10));
        // 队列还有空位，所以线程数保持在 core
        assertThat(pool.getPoolSize()).isEqualTo(1);
        assertThat(pool.getQueue()).hasSize(2);
        pool.shutdownNow();
    }

    // ── 2. 拒绝策略 ────────────────────────────────────────────────

    @Test
    @DisplayName("AbortPolicy 满了直接抛 RejectedExecutionException")
    void abortPolicyThrows() throws Exception {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1),
                new ThreadPoolExecutor.AbortPolicy());
        CountDownLatch busy = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        pool.execute(() -> {
            busy.countDown();
            await(release);
        });
        busy.await();
        pool.execute(() -> sleep(10));   // 进队列

        assertThatThrownBy(() -> pool.execute(() -> sleep(10)))
                .isInstanceOf(RejectedExecutionException.class);

        release.countDown();
        pool.shutdown();
        pool.awaitTermination(3, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("CallerRunsPolicy 让提交任务的线程自己跑")
    void callerRunsPolicyUsesTheCallingThread() throws Exception {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1),
                new ThreadPoolExecutor.CallerRunsPolicy());
        CountDownLatch busy = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        pool.execute(() -> {
            busy.countDown();
            await(release);
        });
        busy.await();
        pool.execute(() -> sleep(10));    // 进队列，此时已满

        String caller = Thread.currentThread().getName();
        List<String> runner = Collections.synchronizedList(new ArrayList<>());
        pool.execute(() -> runner.add(Thread.currentThread().getName()));

        // 第三个任务被交给调用方（main）自己执行
        assertThat(runner).containsExactly(caller);

        release.countDown();
        pool.shutdown();
        pool.awaitTermination(3, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("DiscardPolicy 静默丢掉，不抛异常")
    void discardPolicyIsSilent() throws Exception {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1),
                new ThreadPoolExecutor.DiscardPolicy());
        CountDownLatch busy = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger ran = new AtomicInteger();
        pool.execute(() -> {
            busy.countDown();
            await(release);
        });
        busy.await();
        pool.execute(() -> ran.incrementAndGet());   // 进队列

        pool.execute(() -> ran.incrementAndGet());   // 被静默丢弃，不抛异常
        release.countDown();
        pool.shutdown();
        pool.awaitTermination(3, TimeUnit.SECONDS);

        assertThat(ran.get()).isEqualTo(1);          // 只有队列里那个跑了
    }

    // ── 3. Executors 的坑 ─────────────────────────────────────────

    @Test
    @DisplayName("newFixedThreadPool 用无界队列，任务堆积会 OOM")
    void fixedThreadPoolHasUnboundedQueue() {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        ThreadPoolExecutor tpe = (ThreadPoolExecutor) pool;
        assertThat(tpe.getQueue().remainingCapacity()).isEqualTo(Integer.MAX_VALUE);
        pool.shutdown();
    }

    @Test
    @DisplayName("newCachedThreadPool 的最大线程数无界，来多少任务建多少线程")
    void cachedThreadPoolHasUnboundedThreads() {
        ExecutorService pool = Executors.newCachedThreadPool();
        ThreadPoolExecutor tpe = (ThreadPoolExecutor) pool;
        assertThat(tpe.getMaximumPoolSize()).isEqualTo(Integer.MAX_VALUE);
        assertThat(tpe.getQueue()).isInstanceOf(SynchronousQueue.class);
        pool.shutdown();
    }

    @Test
    @DisplayName("newSingleThreadExecutor 返回的是包装类，不能强转 ThreadPoolExecutor")
    void singleThreadExecutorIsWrapped() {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        assertThat(pool).isNotInstanceOf(ThreadPoolExecutor.class);
        assertThat(pool.getClass().getSimpleName()).contains("Delegated");
        pool.shutdown();
    }

    @Test
    @DisplayName("newWorkStealingPool 返回的是 ForkJoinPool，是另一套实现")
    void workStealingPoolIsAForkJoinPool() {
        ExecutorService pool = Executors.newWorkStealingPool();
        assertThat(pool).isNotInstanceOf(ThreadPoolExecutor.class);
        assertThat(pool.getClass().getSimpleName()).isEqualTo("ForkJoinPool");
        pool.shutdown();
    }

    // ── 4. execute vs submit ──────────────────────────────────────

    @Test
    @DisplayName("execute 抛的异常主线程收不到，只能靠未捕获异常处理器")
    void executeSwallowsExceptionsFromTheCaller() throws Exception {
        AtomicInteger uncaught = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(1, r -> {
            Thread t = new Thread(r);
            t.setUncaughtExceptionHandler((thread, e) -> uncaught.incrementAndGet());
            return t;
        });
        pool.execute(() -> {
            throw new IllegalStateException("execute 里抛的");
        });
        Thread.sleep(200);
        assertThat(uncaught.get()).isEqualTo(1);   // 只有处理器知道
        pool.shutdown();
    }

    @Test
    @DisplayName("submit 抛的异常包在 Future 里，不调 get 就看不见，调了才抛 ExecutionException")
    void submitHidesExceptionsInTheFuture() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(1);
        Future<?> future = pool.submit(() -> {
            throw new IllegalStateException("submit 里抛的");
        });
        Thread.sleep(100);

        assertThatThrownBy(future::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("submit 里抛的");
        pool.shutdown();
    }

    @Test
    @DisplayName("submit 能拿到返回值，execute 不能")
    void submitReturnsAValue() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(1);
        Future<Integer> result = pool.submit(() -> 6 * 7);
        assertThat(result.get()).isEqualTo(42);
        pool.shutdown();
    }

    // ── 5. 停机 ────────────────────────────────────────────────────

    @Test
    @DisplayName("shutdown 不再接新任务，但队列里的会跑完")
    void shutdownFinishesQueuedTasks() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(1);
        AtomicInteger finished = new AtomicInteger();
        for (int i = 0; i < 3; i++) {
            pool.execute(() -> {
                sleep(50);
                finished.incrementAndGet();
            });
        }
        pool.shutdown();

        assertThat(pool.isShutdown()).isTrue();
        assertThat(pool.awaitTermination(3, TimeUnit.SECONDS)).isTrue();
        assertThat(finished.get()).isEqualTo(3);

        // 停机后再提交会被拒绝
        assertThatThrownBy(() -> pool.execute(() -> { }))
                .isInstanceOf(RejectedExecutionException.class);
    }

    @Test
    @DisplayName("shutdownNow 返回没来得及跑的任务，但只是「尝试」中断")
    void shutdownNowReturnsPendingTasks() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(1);
        for (int i = 0; i < 5; i++) {
            pool.execute(() -> sleep(2000));
        }
        Thread.sleep(100);

        List<Runnable> neverRan = pool.shutdownNow();
        assertThat(neverRan).isNotEmpty();
    }

    // ── 6. 线程工厂 ────────────────────────────────────────────────

    @Test
    @DisplayName("线程工厂能给线程起业务名字，排查时一眼认出")
    void threadFactoryNamesThreads() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(3,
                r -> new Thread(r, "order-pool-" + counter.incrementAndGet()));

        List<String> names = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch done = new CountDownLatch(3);
        for (int i = 0; i < 3; i++) {
            pool.execute(() -> {
                names.add(Thread.currentThread().getName());
                done.countDown();
            });
        }
        done.await();
        pool.shutdown();

        assertThat(names).allMatch(n -> n.startsWith("order-pool-"));
        assertThat(names).hasSize(3);
    }

    @Test
    @DisplayName("线程工厂里把线程设成守护线程，JVM 不用等它")
    void threadFactoryCanSetDaemon() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(1, r -> {
            Thread t = new Thread(r, "daemon-worker");
            t.setDaemon(true);
            return t;
        });
        List<Boolean> daemon = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch done = new CountDownLatch(1);
        pool.execute(() -> {
            daemon.add(Thread.currentThread().isDaemon());
            done.countDown();
        });
        done.await();
        pool.shutdown();

        assertThat(daemon).containsExactly(true);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
