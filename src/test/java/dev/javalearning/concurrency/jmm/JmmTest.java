package dev.javalearning.concurrency.jmm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 第 14 课的断言。
 *
 * <p>每一条对应 {@code docs/14-JMM与volatile.md} 里的一节。
 * 跑法：{@code ./gradlew test --tests '*jmm*'}
 */
class JmmTest {

    // ── 1. 可见性 ──────────────────────────────────────────────────

    @Test
    @DisplayName("volatile 保证可见性：写立刻刷回主内存，读强制从主内存读")
    void volatileGuaranteesVisibility() throws Exception {
        JmmDemo.volatileRunning = true;
        AtomicInteger seen = new AtomicInteger();

        Thread worker = new Thread(() -> {
            while (JmmDemo.volatileRunning) {
                seen.incrementAndGet();
            }
        });
        worker.start();
        Thread.sleep(100);
        JmmDemo.volatileRunning = false;
        worker.join(3000);

        // 加了 volatile，线程一定能看到标志位变化并退出
        assertThat(worker.isAlive()).isFalse();
    }

    @Test
    @DisplayName("volatile 不保证原子性：count++ 依然丢更新")
    void volatileDoesNotGuaranteeAtomicity() throws Exception {
        // 单轮可能凑巧不丢（两个线程串行执行了），所以循环到观测到为止。
        // 只要出现一次 count < 400000，volatile 不保证原子性就被证明了。
        int rounds = 0;
        boolean observedLoss = false;
        while (rounds < 50 && !observedLoss) {
            rounds++;
            JmmDemo.volatileCount = 0;
            Thread t1 = new Thread(() -> {
                for (int i = 0; i < 200_000; i++) {
                    JmmDemo.volatileCount++;
                }
            });
            Thread t2 = new Thread(() -> {
                for (int i = 0; i < 200_000; i++) {
                    JmmDemo.volatileCount++;
                }
            });
            t1.start();
            t2.start();
            t1.join();
            t2.join();
            if (JmmDemo.volatileCount < 400_000) {
                observedLoss = true;
            }
        }
        assertThat(observedLoss).isTrue();
    }

    @Test
    @DisplayName("换成 AtomicInteger 就对了：CAS 保证原子性")
    void atomicIntegerFixesIt() throws Exception {
        AtomicInteger count = new AtomicInteger();
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 200_000; i++) {
                count.incrementAndGet();
            }
        });
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 200_000; i++) {
                count.incrementAndGet();
            }
        });
        t1.start();
        t2.start();
        t1.join();
        t2.join();

        assertThat(count.get()).isEqualTo(400_000);
    }

    // ── 2. happens-before：锁规则 ──────────────────────────────────

    @Test
    @DisplayName("解锁 happens-before 后续加锁：普通变量也能跨线程可见")
    void lockRuleGivesVisibilityWithoutVolatile() throws Exception {
        int[] box = {0};                  // 普通数组，不是 volatile
        Object lock = new Object();
        CountDownLatch written = new CountDownLatch(1);

        Thread writer = new Thread(() -> {
            synchronized (lock) {
                box[0] = 42;
            }
            written.countDown();
        });
        writer.start();
        written.await();
        writer.join();

        synchronized (lock) {
            // 靠锁的 happens-before，这里一定读到 42
            assertThat(box[0]).isEqualTo(42);
        }
    }

    // ── 3. happens-before：线程启动与终止 ──────────────────────────

    @Test
    @DisplayName("start() happens-before 新线程里的一切：start 之前的写子线程看得见")
    void startHappensBeforeEverythingInTheNewThread() throws Exception {
        AtomicReference<String> seen = new AtomicReference<>();
        String writtenBeforeStart = "start 之前写的";

        Thread t = new Thread(() -> seen.set(writtenBeforeStart));
        t.start();
        t.join();

        assertThat(seen.get()).isEqualTo("start 之前写的");
    }

    @Test
    @DisplayName("线程里的一切 happens-before join() 返回：join 之后主线程看得见")
    void everythingInThreadHappensBeforeJoinReturns() throws Exception {
        StringBuilder shared = new StringBuilder("主线程写的");
        Thread t = new Thread(() -> shared.append("，子线程追加的"));
        t.start();
        t.join();

        assertThat(shared.toString()).isEqualTo("主线程写的，子线程追加的");
    }

    // ── 4. 状态标志位 ──────────────────────────────────────────────

    @Test
    @DisplayName("volatile 最典型的用法：状态标志位，一个线程写其他线程读")
    void volatileIsForFlags() throws Exception {
        JmmDemo.volatileRunning = true;
        AtomicInteger loops = new AtomicInteger();

        Thread worker = new Thread(() -> {
            while (JmmDemo.volatileRunning) {
                loops.incrementAndGet();
            }
        });
        worker.start();
        Thread.sleep(50);
        JmmDemo.volatileRunning = false;
        worker.join(2000);

        assertThat(worker.isAlive()).isFalse();
        assertThat(loops.get()).isPositive();
    }

    // ── 5. 双重检查锁定 ────────────────────────────────────────────

    @Test
    @DisplayName("双重检查锁定的单例，多次拿到的必须是同一个对象")
    void doubleCheckedLockingReturnsTheSameInstance() {
        assertThat(Singleton.getInstance()).isSameAs(Singleton.getInstance());
    }

    @Test
    @DisplayName("并发拿单例也只有一个：instance 上的 volatile 禁止了 new 的重排")
    void singletonUnderConcurrency() throws Exception {
        int threads = 16;
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Singleton> first = new AtomicReference<>();
        AtomicInteger mismatches = new AtomicInteger();

        Thread[] workers = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            workers[i] = new Thread(() -> {
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                Singleton s = Singleton.getInstance();
                if (!first.compareAndSet(null, s) && first.get() != s) {
                    mismatches.incrementAndGet();
                }
            });
            workers[i].start();
        }
        start.countDown();
        for (Thread w : workers) {
            w.join();
        }

        assertThat(mismatches.get()).isZero();
    }
}
