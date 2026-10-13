package dev.javalearning.concurrency.safety;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 第 13 课的断言。
 *
 * <p>每一条对应 {@code docs/13-线程安全与同步.md} 里的一节。
 * 跑法：{@code ./gradlew test --tests '*safety*'}
 *
 * <p><b>写并发测试的原则</b>：不要断言「这一轮一定出错」（竞态是概率性的，
 * 那会变成随机失败的测试）。要么断言「跑足够多轮后一定观测到出错」，
 * 要么断言「加锁后一定正确」——后者是确定的，可以放心断言。
 */
class SafetyTest {

    private static final int THREADS = 10;
    private static final int PER_THREAD = 10_000;
    private static final int EXPECTED = THREADS * PER_THREAD;

    // ── 1. 不加锁会丢更新 ──────────────────────────────────────────

    @Test
    @DisplayName("不加锁的 count++ 会丢更新：跑足够多轮一定观测到")
    void unsafeCounterLosesUpdates() throws Exception {
        boolean observedLoss = false;
        for (int round = 0; round < 20 && !observedLoss; round++) {
            if (unsafeCount() < EXPECTED) {
                observedLoss = true;
            }
        }
        assertThat(observedLoss).isTrue();
    }

    @Test
    @DisplayName("加 synchronized 之后，跑一次就是精确的 100000")
    void synchronizedCounterIsExact() throws Exception {
        // 加锁后结果是确定的，可以直接断言精确值
        assertThat(synchronizedCount()).isEqualTo(EXPECTED);
    }

    @Test
    @DisplayName("同步代码块用同一把共享锁，结果同样精确")
    void synchronizedBlockWithSharedLockIsExact() throws Exception {
        int[] count = {0};
        Object lock = new Object();
        Thread[] ts = new Thread[THREADS];
        CountDownLatch start = new CountDownLatch(1);
        for (int i = 0; i < THREADS; i++) {
            ts[i] = new Thread(() -> {
                await(start);
                for (int j = 0; j < PER_THREAD; j++) {
                    synchronized (lock) {
                        count[0]++;
                    }
                }
            });
        }
        for (Thread t : ts) {
            t.start();
        }
        start.countDown();
        for (Thread t : ts) {
            t.join();
        }
        assertThat(count[0]).isEqualTo(EXPECTED);
    }

    // ── 2. 锁 new Object() 等于没锁 ────────────────────────────────

    @Test
    @DisplayName("锁 new Object() 等于完全没锁：每次都是一把新锁")
    void lockingAFreshObjectIsNoLockAtAll() throws Exception {
        boolean observedLoss = false;
        for (int round = 0; round < 20 && !observedLoss; round++) {
            int[] count = {0};
            Thread[] ts = new Thread[THREADS];
            CountDownLatch start = new CountDownLatch(1);
            for (int i = 0; i < THREADS; i++) {
                ts[i] = new Thread(() -> {
                    await(start);
                    for (int j = 0; j < PER_THREAD; j++) {
                        synchronized (new Object()) {   // 每次新锁
                            count[0]++;
                        }
                    }
                });
            }
            for (Thread t : ts) {
                t.start();
            }
            start.countDown();
            for (Thread t : ts) {
                t.join();
            }
            if (count[0] < EXPECTED) {
                observedLoss = true;
            }
        }
        assertThat(observedLoss).isTrue();
    }

    // ── 3. 可重入 ──────────────────────────────────────────────────

    @Test
    @DisplayName("synchronized 可重入：同一线程重复进入同一把锁不会死锁")
    void synchronizedIsReentrant() {
        // 这个方法内部再调另一个 synchronized 方法，不会卡住
        assertThat(outer()).isEqualTo("outer-inner");
    }

    private synchronized String outer() {
        return "outer-" + inner();
    }

    private synchronized String inner() {
        return "inner";
    }

    @Test
    @DisplayName("ReentrantLock 也是可重入的：holdCount 会累加")
    void reentrantLockIsReentrant() {
        ReentrantLock lock = new ReentrantLock();
        lock.lock();
        try {
            assertThat(lock.getHoldCount()).isEqualTo(1);
            lock.lock();
            try {
                assertThat(lock.getHoldCount()).isEqualTo(2);
            } finally {
                lock.unlock();
            }
            assertThat(lock.getHoldCount()).isEqualTo(1);
        } finally {
            lock.unlock();
        }
        assertThat(lock.isLocked()).isFalse();
    }

    // ── 4. Lock ────────────────────────────────────────────────────

    @Test
    @DisplayName("ReentrantLock 保护的结果同样精确")
    void reentrantLockProtectsCorrectly() throws Exception {
        Lock lock = new ReentrantLock();
        int[] count = {0};
        Thread[] ts = new Thread[THREADS];
        CountDownLatch start = new CountDownLatch(1);
        for (int i = 0; i < THREADS; i++) {
            ts[i] = new Thread(() -> {
                await(start);
                for (int j = 0; j < PER_THREAD; j++) {
                    lock.lock();
                    try {
                        count[0]++;
                    } finally {
                        lock.unlock();
                    }
                }
            });
        }
        for (Thread t : ts) {
            t.start();
        }
        start.countDown();
        for (Thread t : ts) {
            t.join();
        }
        assertThat(count[0]).isEqualTo(EXPECTED);
    }

    @Test
    @DisplayName("unlock 放 finally：抛异常也能把锁还回去")
    void unlockInFinallyReleasesOnException() {
        ReentrantLock lock = new ReentrantLock();
        try {
            lock.lock();
            try {
                throw new IllegalStateException("故意抛的");
            } finally {
                lock.unlock();
            }
        } catch (IllegalStateException ignored) {
            // 吞掉
        }
        assertThat(lock.isLocked()).isFalse();
    }

    // ── 5. 原子类 ──────────────────────────────────────────────────

    @Test
    @DisplayName("换成 AtomicInteger 也能精确：CAS 保证原子性")
    void atomicIntegerIsExact() throws Exception {
        AtomicInteger count = new AtomicInteger();
        Thread[] ts = new Thread[THREADS];
        CountDownLatch start = new CountDownLatch(1);
        for (int i = 0; i < THREADS; i++) {
            ts[i] = new Thread(() -> {
                await(start);
                for (int j = 0; j < PER_THREAD; j++) {
                    count.incrementAndGet();
                }
            });
        }
        for (Thread t : ts) {
            t.start();
        }
        start.countDown();
        for (Thread t : ts) {
            t.join();
        }
        assertThat(count.get()).isEqualTo(EXPECTED);
    }

    // ── 6. 实战：卖票 ──────────────────────────────────────────────

    @Test
    @DisplayName("加锁的售票：100 张正好卖完，剩余 0")
    void lockedTicketOfficeSellsExactly100() throws Exception {
        TicketOffice office = new TicketOffice(true);
        Thread[] windows = new Thread[4];
        for (int i = 0; i < 4; i++) {
            windows[i] = new Thread(office::sell);
        }
        for (Thread w : windows) {
            w.start();
        }
        for (Thread w : windows) {
            w.join();
        }
        assertThat(office.remaining()).isZero();
    }

    @Test
    @DisplayName("不加锁的售票会超卖：跑足够多轮一定观测到剩余为负")
    void unlockedTicketOfficeOversells() throws Exception {
        boolean oversold = false;
        for (int round = 0; round < 200 && !oversold; round++) {
            TicketOffice office = new TicketOffice(false);
            Thread[] windows = new Thread[4];
            for (int i = 0; i < 4; i++) {
                windows[i] = new Thread(office::sell);
            }
            for (Thread w : windows) {
                w.start();
            }
            for (Thread w : windows) {
                w.join();
            }
            if (office.remaining() < 0) {
                oversold = true;
            }
        }
        assertThat(oversold).isTrue();
    }

    // ── 工具 ───────────────────────────────────────────────────────

    /** 跑一轮不加锁的计数。这里用 int[] 模拟"裸的 count++"（读-改-写三步）。 */
    private static int unsafeCount() throws Exception {
        int[] count = {0};
        runCounting(() -> count[0]++);
        return count[0];
    }

    private static void runCounting(Runnable increment) throws Exception {
        Thread[] ts = new Thread[THREADS];
        CountDownLatch start = new CountDownLatch(1);
        for (int i = 0; i < THREADS; i++) {
            ts[i] = new Thread(() -> {
                await(start);
                for (int j = 0; j < PER_THREAD; j++) {
                    increment.run();
                }
            });
        }
        for (Thread t : ts) {
            t.start();
        }
        start.countDown();
        for (Thread t : ts) {
            t.join();
        }
    }

    private static int[] synchronizedCountHolder = {0};

    private static synchronized void safeIncrement() {
        synchronizedCountHolder[0]++;
    }

    private static int synchronizedCount() throws Exception {
        synchronizedCountHolder = new int[]{0};
        Thread[] ts = new Thread[THREADS];
        CountDownLatch start = new CountDownLatch(1);
        for (int i = 0; i < THREADS; i++) {
            ts[i] = new Thread(() -> {
                await(start);
                for (int j = 0; j < PER_THREAD; j++) {
                    safeIncrement();
                }
            });
        }
        for (Thread t : ts) {
            t.start();
        }
        start.countDown();
        for (Thread t : ts) {
            t.join();
        }
        return synchronizedCountHolder[0];
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
